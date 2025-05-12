package api;

import Domain.User;
import Services.UserServices.UserService;
import app.DTO.UserRegistrationDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import util.JwtUtil;

import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    private UserService userService;

    @POST
    @Path("/register")
    public Response register(UserRegistrationDTO userDTO) {
        try {
            User user = userService.registerUser(userDTO);

            String token = JwtUtil.generateToken(user.getEmail());

            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\":\"User registered successfully\", \"token\":\"" + token + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }
    @GET
    @Path("/all")
    public Response getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Response.ok(users).build();
    }


}
