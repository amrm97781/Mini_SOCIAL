package notificationModule;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
@Stateless
public class ActivityLogService {

    @PersistenceContext
    private EntityManager em;

    // Get activity logs for a specific user
    public List<ActivityLog> getActivityLogsByUser(Long userId) {
        return em.createQuery("SELECT log FROM ActivityLog log WHERE log.userId = :userId ORDER BY log.timestamp DESC", ActivityLog.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    // Get all activity logs
    public List<ActivityLog> getAllActivityLogs() {
        return em.createQuery("SELECT log FROM ActivityLog log ORDER BY log.timestamp DESC", ActivityLog.class)
                .getResultList();
    }
}
