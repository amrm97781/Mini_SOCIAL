package api;

import Services.UserServices.GroupService;
import Services.UserServices.PostService;
import Services.UserServices.UserService;
import Domain.Group;
import Domain.User;
import app.DTO.CreateGroupDTO;
import app.DTO.MembershipActionDTO;
import app.DTO.GroupDetailsDTO;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.security.*;


@Path("/groups")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GroupResource {

    @Inject private GroupService groupService;
    @Inject private PostService postService;
    @Inject private UserService  userService;
    @Context private HttpServletRequest request;

    private User getCurrentUser() {
        HttpSession session = request.getSession(false);
        if (session == null) throw new NotAuthorizedException("You must log in first");
        User u = (User) session.getAttribute("currentUser");
        if (u == null)      throw new NotAuthorizedException("You must log in first");
        return u;
    }

    private void ensureAdmin(Group g, User u) {
        if (!groupService.isAdmin(g, u)) {
            throw new ForbiddenException("Only the group creator may perform this action");
        }
    }

    @POST
    @Path("/create")
    public Response createGroup(CreateGroupDTO dto, @Context UriInfo uriInfo) {
        User me = getCurrentUser();
        Group g = groupService.createGroup(
                dto.getName(),
                dto.getDescription(),
                !dto.isOpen(),   // your DTO isOpen→entity.closed invert
                me
        );
        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(g.getId().toString())
                .build();
        return Response.created(uri)
                .entity(toDTO(g))
                .build();
    }


    @GET
    public List<GroupDetailsDTO> listGroups() {
        return groupService.listAllGroups().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    @GET @Path("/{id}")
    public GroupDetailsDTO getGroup(@PathParam("id") Long id) {
        Group g = groupService.findById(id);
        return toDTO(g);
    }


    @POST @Path("/{id}/join")
    public Response joinGroup(@PathParam("id") Long id) {
        User me = getCurrentUser();
        boolean pending = groupService.joinGroup(id, me);
        String status = pending ? "pending" : "joined";
        return Response.ok(Collections.singletonMap("status", status)).build();
    }

    /** Approve / reject (admin only) */
    @POST @Path("/{id}/approve")
    public Response approveMember(
            @PathParam("id") Long groupId,
            MembershipActionDTO dto
    ) {
        User me = getCurrentUser();
        Group g  = groupService.findById(groupId);
        ensureAdmin(g, me);

        groupService.approveMembership(
                groupId,
                dto.getUserId(),
                dto.isApprove()
        );
        return Response.ok().build();
    }

    /** Leave group (any member) */
    @POST @Path("/{id}/leave")
    public Response leaveGroup(@PathParam("id") Long id) {
        User me = getCurrentUser();
        groupService.leaveGroup(id, me);

        return Response.ok().build();
    }

    /** List pending requests (admin only) */
    @GET @Path("/{id}/requests")
    public Set<Long> listJoinRequests(@PathParam("id") Long groupId) {
        User me    = getCurrentUser();
        Group g    = groupService.findById(groupId);
        ensureAdmin(g, me);

        return groupService.listJoinRequestIds(groupId);
    }

    /** (Optional) Update group metadata (admin only) */
    @PUT @Path("/{id}")
    public Response updateGroup(
            @PathParam("id") Long groupId,
            CreateGroupDTO dto
    ) {
        User me = getCurrentUser();
        Group g = groupService.findById(groupId);
        ensureAdmin(g, me);

        Group updated = groupService.updateGroup(
                groupId,
                dto.getName(),
                dto.getDescription(),
                !dto.isOpen()
        );
        return Response.ok(toDTO(updated)).build();
    }


    // ——————————————————————————————————————————————————————————————————————————
    // DTO mapping
    // ——————————————————————————————————————————————————————————————————————————

    private GroupDetailsDTO toDTO(Group g) {
        GroupDetailsDTO dto = new GroupDetailsDTO();
        dto.setId(g.getId());
        dto.setName(g.getName());
        dto.setDescription(g.getDescription());
        dto.setClosed(g.isClosed());
        dto.setCreatorId(g.getCreator().getId());
        dto.setMemberIds(
                g.getMembers().stream()
                        .map(User::getId)
                        .collect(Collectors.toSet())
        );
        return dto;
    }


    // ─────────────────────────────────────────────────────────────────────
    // List all admin user‐IDs for this group
    // ─────────────────────────────────────────────────────────────────────
    @GET
    @Path("/{id}/admins")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Long> listAdmins(@PathParam("id") Long groupId) {
        // optionally: ensure the caller is at least a member
        User me = getCurrentUser();
        Group g = groupService.findById(groupId);
        if (!groupService.isMember(g, me)) {
            throw new ForbiddenException("Must be a group member to see admins");
        }
        return groupService.listAdminIds(groupId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Promote a member to admin
    // ─────────────────────────────────────────────────────────────────────
    @POST
    @Path("/{id}/admins")
    @RolesAllowed("GROUP_ADMIN")
    public Response promote(
            @PathParam("id") Long groupId,
            MembershipActionDTO dto
    ) {
        groupService.promoteToAdmin(groupId, dto.getUserId(), getCurrentUser());
        return Response.ok().build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Demote an admin
    // ─────────────────────────────────────────────────────────────────────
    @DELETE
    @Path("/{id}/admins/{userId}")
    @RolesAllowed("GROUP_ADMIN")
    public Response demote(
            @PathParam("id")     Long groupId,
            @PathParam("userId") Long userId
    ) {
        groupService.demoteAdmin(groupId, userId, getCurrentUser());
        return Response.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Remove a member
    // ─────────────────────────────────────────────────────────────────────
    @DELETE
    @Path("/{id}/members/{userId}")
    @RolesAllowed("GROUP_ADMIN")
    public Response removeMember(
            @PathParam("id")     Long groupId,
            @PathParam("userId") Long userId
    ) {
        groupService.removeMember(groupId, userId, getCurrentUser());
        return Response.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Remove a post
    // ─────────────────────────────────────────────────────────────────────
    @DELETE
    @Path("/{id}/posts/{postId}")
    @RolesAllowed("GROUP_ADMIN")
    public Response removePost(
            @PathParam("id")     Long groupId,
            @PathParam("postId") Long postId
    ) {

        postService.removePost(groupId, postId, getCurrentUser());
        return Response.ok().build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // (Your existing deleteGroup should also have @RolesAllowed)
    // ─────────────────────────────────────────────────────────────────────
    @DELETE
    @Path("/{id}")
    @RolesAllowed("GROUP_ADMIN")
    public Response deleteGroup(@PathParam("id") Long groupId){
        groupService.deleteGroup(groupId,getCurrentUser());
        return Response.noContent().build();
    }



}
