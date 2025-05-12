package Services.UserServices;

import Domain.*;
import app.DTO.PostDTO;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import notificationModule.NotificationEvent;
import notificationModule.NotificationProducer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class PostService {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private NotificationProducer notificationProducer;
    @EJB
    private ConnectionService connectionService;

    @Resource(lookup = "java:/jms/queue/NotificationQueue")
    private Queue notificationQueue;

    @Inject
    private JMSContext jmsContext;


    @Transactional
    public void createPost(Long authorId, String content, String imageUrl, String link) {
        Post post = new Post();
        post.setAuthor(em.find(User.class, authorId));
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setLink(link);
        post.setCreatedAt(LocalDateTime.now());
        em.persist(post);
    }

    @Inject
    private GroupService groupService;
    public Post createPostInGroup(Long groupId, User author, PostDTO dto) {
        Group group = groupService.findById(groupId);
        if (!groupService.isMember(group, author)) {
            throw new ForbiddenException("Must be a group member to create posts");
        }
        Post post = new Post();
        post.setAuthor(author);
        post.setContent(dto.getContent());
        post.setCreatedAt(LocalDateTime.now());

        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
            post.setImageUrl(dto.getImageUrl());
        }

        group.addPost(post);
        em.persist(post);
        return post;
    }

    @Transactional
    public List<PostDTO> getFeed(Long userId) {
        List<User> friends = connectionService.getFriends(userId);
        friends.add(em.find(User.class, userId));

        List<Post> posts = em.createQuery(
                        "SELECT DISTINCT p FROM Post p " +
                                "LEFT JOIN FETCH p.comments " +
                                "LEFT JOIN FETCH p.likes " +
                                "WHERE p.author IN :users " +
                                "ORDER BY p.createdAt DESC", Post.class)
                .setParameter("users", friends)
                .getResultList();

        return posts.stream().map(post -> {
            PostDTO dto = new PostDTO();
            dto.setId(post.getId());
            dto.setAuthorId(post.getAuthor().getId());
            dto.setContent(post.getContent());
            dto.setImageUrl(post.getImageUrl());
            dto.setLink(post.getLink());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setAuthorUsername(post.getAuthor().getName());

            dto.setCommentContents(
                    post.getComments().stream()
                            .map(Comment::getText)
                            .collect(Collectors.toList())
            );

            dto.setLikesCount(post.getLikes().size());

            return dto;
        }).collect(Collectors.toList());
    }

    public void updatePost(Long postId, String content, String imageUrl, String link) {
        Post post = em.find(Post.class, postId);
        if (post != null) {
            post.setContent(content);
            post.setImageUrl(imageUrl);
            post.setLink(link);
            post.setUpdatedAt(LocalDateTime.now());
            em.merge(post);
        }
    }

    public void deletePost(Long postId) {
        Post post = em.find(Post.class, postId);
        if (post != null) em.remove(post);
    }

    public void addComment(Long postId, Long userId, String text) {
        Comment comment = new Comment();
        User user = em.find(User.class, userId);
        Post post = em.find(Post.class, postId);

        // Check if the user is friends with the post's author
        boolean isFriend = checkIfFriend(userId, post.getAuthor().getId());
        if (!isFriend) {
            throw new RuntimeException("You can only comment on posts from your friends.");
        }

        comment.setAuthor(user);
        comment.setPost(post);
        comment.setText(text);
        comment.setCreatedAt(LocalDateTime.now());
        em.persist(comment);

        if (!userId.equals(post.getAuthor().getId())) {
            NotificationEvent event = new NotificationEvent();
            event.setEventType("COMMENT");
            event.setFromUserId(userId);
            event.setToUserId(post.getAuthor().getId());
            event.setMessage(user.getName() + " commented on your post.");
            event.setPostId(postId);

            notificationProducer.sendNotification(event);
        }
    }

    private boolean checkIfFriend(Long userId, Long postAuthorId) {
        String query = "SELECT c FROM Connection c WHERE (c.sender.id = :userId AND c.receiver.id = :postAuthorId OR c.sender.id = :postAuthorId AND c.receiver.id = :userId) AND c.status = :status";
        List<Connection> connections = em.createQuery(query, Connection.class)
                .setParameter("userId", userId)
                .setParameter("postAuthorId", postAuthorId)
                .setParameter("status", Status.ACCEPTED)
                .getResultList();

        return !connections.isEmpty();
    }


    public void likePost(Long postId, Long userId) {
        Post post = em.find(Post.class, postId);
        User user = em.find(User.class, userId);

        // Check if the user is friends with the post's author
        boolean isFriend = checkIfFriend(userId, post.getAuthor().getId());
        if (!isFriend) {
            throw new RuntimeException("You can only like posts from your friends.");
        }

        if (!post.getLikes().contains(user)) {
            post.getLikes().add(user);
            em.merge(post);

            if (!userId.equals(post.getAuthor().getId())) {
                NotificationEvent event = new NotificationEvent();
                event.setEventType("LIKE");
                event.setFromUserId(userId);
                event.setToUserId(post.getAuthor().getId());
                event.setMessage(user.getName() + " liked your post.");
                event.setPostId(postId);

                notificationProducer.sendNotification(event);
            }
        }
    }



    public List<Post> listPosts(Long groupId, User requester) {
        Group g = groupService.findById(groupId);
        if (!groupService.isMember(g, requester)) {
            throw new ForbiddenException("Must be a group member to view posts");
        }
        // fresh query—won’t include deleted rows
        return em.createQuery(
                        "SELECT p FROM Post p WHERE p.group.id = :gid ORDER BY p.createdAt DESC",
                        Post.class
                )
                .setParameter("gid", groupId)
                .getResultList();
    }


    public void removePost(Long groupId, Long postId, User requester) {
        Group g = groupService.findById(groupId);
        if (!groupService.isAdmin(g, requester)) {
            throw new ForbiddenException("Only admins can remove posts");
        }
        Post p = em.find(Post.class, postId);
        if (p == null || !p.getGroup().getId().equals(groupId)) {
            throw new BadRequestException("Post not found in this group");
        }
        g.removePost(p);
        em.remove(em.contains(p) ? p : em.merge(p));
        em.merge(g);
    }




}
