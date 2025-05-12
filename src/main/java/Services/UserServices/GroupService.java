package Services.UserServices;

import Domain.Group;
import Domain.User;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import Domain.Post;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import notificationModule.NotificationEvent;
import notificationModule.NotificationProducer;


@Stateless
public class GroupService {

    @PersistenceContext
    private EntityManager em;
    @Inject
    private NotificationProducer notificationProducer;
    /**
     * Create a new group and persist it.
     */
    public Group createGroup(String name, String description, boolean closed, User creator) {
        Group group = new Group(name, description, closed, creator);
        em.persist(group);
        return group;
    }

    /**
     * Request to join a group. If open, add to members immediately.
     * Otherwise, leave for admin approval.
     */



    /**
     * Admin approves or rejects membership. Approve then add to members.
     */
    public void approveMembership(Long groupId, Long userId, boolean approve) {
        Group group = em.find(Group.class, groupId);
        User  user  = em.find(User.class, userId);
        if (group == null || user == null)
            throw new IllegalArgumentException("Group or user not found");

        if (approve) {
            // only if they actually asked
            if (group.getJoinRequests().remove(user)) {
                group.addMember(user);
            }
        } else {
            // reject: just drop the request
            group.getJoinRequests().remove(user);
        }

        em.merge(group);
    }


    /**
     * Leave a group, removing the user from members list.
     */
    public boolean joinGroup(Long groupId, User user) {
        Group group = em.find(Group.class, groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found");
        }

        if (!group.isClosed()) {
            // open → join immediately
            if (!group.getMembers().contains(user)) {
                group.addMember(user);
                em.merge(group);

                // Send notification to group creator/admin
                if (!user.getId().equals(group.getCreator().getId())) {
                    NotificationEvent event = new NotificationEvent();
                    event.setEventType("GROUP_JOIN");
                    event.setFromUserId(user.getId());
                    event.setToUserId(group.getCreator().getId());
                    event.setMessage(user.getName() + " joined your group \"" + group.getName() + "\".");


                    notificationProducer.sendNotification(event);
                }
            }
            return false;
        } else {
            // closed → queue a request
            if (!group.getJoinRequests().contains(user)
                    && !group.getMembers().contains(user)) {
                group.addJoinRequest(user);
                em.merge(group);
            }
            return true;
        }
    }

    public void leaveGroup(Long groupId, User user) {
        Group group = em.find(Group.class, groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found");
        }

        if (group.getMembers().contains(user)) {
            group.removeMember(user);
            em.merge(group);

            // Send notification to group creator/admin
            if (!user.getId().equals(group.getCreator().getId())) {
                NotificationEvent event = new NotificationEvent();
                event.setEventType("GROUP_LEAVE");
                event.setFromUserId(user.getId());
                event.setToUserId(group.getCreator().getId());
                event.setMessage(user.getName() + " left your group \"" + group.getName() + "\".");


                notificationProducer.sendNotification(event);
            }
        }
    }


    /**
     * Retrieve all groups with their members eagerly fetched to avoid lazy initialization errors.
     */
    public List<Group> listAllGroups() {
        return em.createQuery(
                        "SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.members", Group.class)
                .getResultList();
    }

    /**
     * Get members of a group and initialize the collection.
     */
    public Set<User> getMembers(Long groupId) {
        Group group = em.find(Group.class, groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found");
        }
        // Initialize members collection
        group.getMembers().size();
        return group.getMembers();
    }

    public Group findById(Long id) {
        Group g = em.find(Group.class, id);
        if (g == null) throw new NotFoundException("Group not found");
        g.getMembers().size();       // init members
        g.getPosts().size();         // init posts (once you add posts list)
        return g;
    }

    public boolean isMember(Group group, User user) {
        Long uid = user.getId();
        return group.getMembers().stream()
                .anyMatch(m -> m.getId().equals(uid));
    }
    public Set<Long> listJoinRequestIds(Long groupId) {
        Group g = em.find(Group.class, groupId);
        if (g == null) throw new NotFoundException("Group not found");
        // touching the collection inside the transaction causes it to load
        return g.getJoinRequests()
                .stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    public boolean isAdmin(Group g, User u) { return g.getCreator() != null && g.getCreator().getId().equals(u.getId());}


    public Group updateGroup(Long groupId, String name, String description, boolean closed) {
        Group group = findById(groupId);
        group.setName(name);
        group.setDescription(description);
        group.setClosed(closed);
        // entity is already managed; merge not strictly required, but ensures sync
        em.merge(group);
        return group;
    }

    public void deleteGroup(Long groupId) {
        Group group = findById(groupId);
        // ensure managed
        if (!em.contains(group)) {
            group = em.merge(group);
        }
        em.remove(group);
    }



    /** Promote a member to admin */
    public void promoteToAdmin(Long groupId, Long userId, User requester) {
        Group g = findById(groupId);
        if (!isAdmin(g, requester)) {
            throw new ForbiddenException("Only admins can promote");
        }
        User u = em.find(User.class, userId);
        if (u == null || !isMember(g, u)) {
            throw new BadRequestException("User is not a member");
        }
        g.addAdmin(u);
        em.merge(g);
    }

    /** Demote an admin (but not the creator) */
    public void demoteAdmin(Long groupId, Long userId, User requester) {
        Group g = findById(groupId);
        if (!isAdmin(g, requester)) {
            throw new ForbiddenException("Only admins can demote");
        }
        User u = em.find(User.class, userId);
        if (u == null
                || !g.getAdmins().contains(u)
                || u.equals(g.getCreator())) {
            throw new BadRequestException("Cannot demote this user");
        }
        g.removeAdmin(u);
        em.merge(g);
    }

    /** Remove a member (and demote if needed) */
    public void removeMember(Long groupId, Long userId, User requester) {
        Group g = findById(groupId);
        if (!isAdmin(g, requester)) {
            throw new ForbiddenException("Only admins can remove members");
        }
        User u = em.find(User.class, userId);
        if (u == null || !isMember(g, u)) {
            throw new BadRequestException("Not a member");
        }
        g.removeMember(u);
        g.removeAdmin(u);
        em.merge(g);
    }

    /** Remove a post from the group */
    public void removePost(Long groupId, Long postId, User requester) {
        Group g = findById(groupId);
        if (!isAdmin(g, requester)) {
            throw new ForbiddenException("Only admins can remove posts");
        }
        Post p = em.find(Post.class, postId);
        if (p == null || !p.getGroup().getId().equals(groupId)) {
            throw new BadRequestException("Post not found in this group");
        }
        em.remove(em.contains(p) ? p : em.merge(p));
    }


    public Set<Long> listAdminIds(Long groupId) {
        Group g = findById(groupId);
        return g.getAdmins().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }




}