package notificationModule;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/activity-logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActivityLogResource {

    @Inject
    private ActivityLogService activityLogService;  // Inject your service that fetches activity logs

    @GET
    @Path("/user/{userId}")
    public Response getUserActivityLogs(@PathParam("userId") Long userId) {
        List<ActivityLog> logs = activityLogService.getActivityLogsByUser(userId);
        if (logs.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No activity logs found for this user.")
                    .build();
        }
        return Response.ok(logs).build();
    }

    @GET
    @Path("/all")
    public Response getAllActivityLogs() {
        List<ActivityLog> logs = activityLogService.getAllActivityLogs();
        if (logs.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No activity logs found.")
                    .build();
        }
        return Response.ok(logs).build();
    }
}
