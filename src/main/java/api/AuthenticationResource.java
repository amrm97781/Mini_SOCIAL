/*
package api;

import Domain.User;
import Services.UserServices.AuthenticationService;
import app.DTO.LoginDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    @Inject
    private AuthenticationService authenticationService;

    @POST
    @Path("/login")
    public Response login(LoginDTO loginDTO) {
        try {
            User user = authenticationService.login(loginDTO.getEmail(), loginDTO.getPassword());
            return Response.ok(user).build(); // You can return a JWT token for authentication
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
    }
}
*/

package api;

import Domain.User;
import Services.UserServices.AuthenticationService;
import app.DTO.LoginDTO;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    @Inject
    private AuthenticationService authenticationService;

    @POST
    @Path("/login")
    public Response login(LoginDTO loginDTO, @Context HttpServletRequest request) {
        try {
            // 1) authenticate credentials
            User user = authenticationService.login(
                    loginDTO.getEmail(),
                    loginDTO.getPassword()
            );

            // 2) store user in session
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);

            // 3) return OK with user info (or a token in a real app)
            return Response.ok(user).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout(@Context HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Response.noContent().build();
    }
}


