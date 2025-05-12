package app.DTO;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * DTO for membership actions (join, approve, leave).
 */
public class MembershipActionDTO {

    private Long groupId;
    private Long userId;    // for admin approval actions
    private boolean approve; // true to approve, false to reject (for closed groups)

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isApprove() {
        return approve;
    }

    public void setApprove(boolean approve) {
        this.approve = approve;
    }
}
