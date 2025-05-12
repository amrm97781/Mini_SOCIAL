package notificationModule;

import java.io.Serializable;

public class NotificationEvent implements Serializable {
    private String eventType;     // e.g., "FRIEND_REQUEST", "COMMENT", "LIKE", "GROUP_JOIN", "GROUP_LEAVE"
    private Long fromUserId;
    private Long toUserId;
    private Long postId;          // optional
    private String message;


    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}