package api;

import Domain.User;
import Services.UserServices.ProfileService;
import Services.UserServices.UserService;
import app.DTO.ProfileUpdateDTO;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/profile")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @EJB
    private ProfileService profileService;

    @PUT
    @Path("/{userId}")
    public Response updateProfile(
            @PathParam("userId") Long userId,
            ProfileUpdateDTO request
    ) {
        try {
            User updatedUser = profileService.updateProfile(
                    userId,
                    request.getName(),
                    request.getBio(),
                    request.getEmail(),
                    request.getPassword()
            );
            return Response.ok(updatedUser).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage()).build();
        }
    }
}

