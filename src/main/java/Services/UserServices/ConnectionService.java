package Services.UserServices;

import Domain.Connection;
import Domain.Status;
import Domain.User;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import notificationModule.NotificationEvent;
import notificationModule.NotificationProducer;

import java.util.ArrayList;
import java.util.List;

@Stateless
public class ConnectionService {

    @Resource(lookup = "java:/jms/queue/NotificationQueue")
    private Queue notificationQueue;

    @Inject
    private NotificationProducer notificationProducer;
    @Inject
    private JMSContext jmsContext;

    @PersistenceContext
    private EntityManager em;


    public void sendFriendRequest(Long senderId, Long receiverId) {
        Connection connection = new Connection();
        User sender = em.find(User.class, senderId);
        User receiver = em.find(User.class, receiverId);
        connection.setSender(sender);
        connection.setReceiver(receiver);
        connection.setStatus(Status.PENDING);
        em.persist(connection);

        if (!senderId.equals(receiverId)) {
            NotificationEvent event = new NotificationEvent();
            event.setEventType("FRIEND_REQUEST");
            event.setFromUserId(senderId);
            event.setToUserId(receiverId);
            event.setMessage(sender.getName() + " sent you a friend request.");
            event.setPostId(connection.getId());

            notificationProducer.sendNotification(event);
        }
    }


    public void respondToRequest(Long requestId, boolean accept) {
        Connection connection = em.find(Connection.class, requestId);
        if (connection != null && connection.getStatus()== Status.PENDING) {
            connection.setStatus(Status.valueOf(accept ? "ACCEPTED" : "REJECTED"));
            em.merge(connection);
        }
    }

    public List<Connection> getPendingRequests(Long userId) {
        return em.createQuery("SELECT c FROM Connection c WHERE c.receiver.id = :uid AND c.status = 'PENDING'", Connection.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    public List<User> getFriends(Long userId) {
        List<User> sent = em.createQuery("""
        SELECT c.receiver FROM Connection c
        WHERE c.sender.id = :uid AND c.status = 'ACCEPTED'
        """, User.class)
                .setParameter("uid", userId)
                .getResultList();

        List<User> received = em.createQuery("""
        SELECT c.sender FROM Connection c
        WHERE c.receiver.id = :uid AND c.status = 'ACCEPTED'
        """, User.class)
                .setParameter("uid", userId)
                .getResultList();

        sent.addAll(received);
        return sent;
    }

    public User findUserById(Long senderId) {
        return em.find(User.class, senderId);
    }

    public List<User> searchUsers(String searchTerm) {
        String query = "SELECT u FROM User u WHERE u.name LIKE :searchTerm OR u.email LIKE :searchTerm";
        return em.createQuery(query, User.class)
                .setParameter("searchTerm", "%" + searchTerm + "%")
                .getResultList();
    }

    public List<User> suggestFriends(Long userId) {

        List<User> friends = getFriends(userId);

        // Create a list to store suggested friends
        List<User> suggestedFriends = new ArrayList<>();

        // For each friend, get their friends and find mutual connections
        for (User friend : friends) {
            List<User> mutualFriends = getFriends(friend.getId());

            for (User mutualFriend : mutualFriends) {
                // Only suggest users that are not already friends and not the user themself
                if (!friends.contains(mutualFriend) && !mutualFriend.getId().equals(userId)) {
                    suggestedFriends.add(mutualFriend);
                }
            }
        }

        // Return suggested friends based on mutual connections
        return suggestedFriends;
    }

}



