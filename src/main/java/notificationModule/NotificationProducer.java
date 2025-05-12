package notificationModule;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jms.*;
import jakarta.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class NotificationProducer {

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = "java:/jms/queue/NotificationQueue")
    private Queue notificationQueue;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendNotification(NotificationEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            TextMessage message = jmsContext.createTextMessage(json);
            jmsContext.createProducer().send(notificationQueue, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
