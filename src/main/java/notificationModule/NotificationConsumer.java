package notificationModule;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;

@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/NotificationQueue"),
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
        }
)
public class NotificationConsumer implements MessageListener {

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                String json = textMessage.getText();
                NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
                System.out.println("New Event Received:");
                System.out.println("Event Type: " + event.getEventType());
                System.out.println("Message: " + event.getMessage());

                // Log the Notification Event in the database
                ActivityLog log = new ActivityLog();
                log.setUserId(event.getToUserId());
                log.setActionType(event.getEventType());
                log.setActionDetails(event.getMessage());
                log.setTimestamp(LocalDateTime.now());


                // Persist the log
                em.persist(log);

                // Optionally, print the log for debugging purposes
                System.out.println("Logged Event: " + log);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


