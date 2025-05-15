package Domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;



@Entity
@Table(name = "groups")
public class Group {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    /** open=false ⇒ closed group (needs admin approval) */
    @Column(nullable = false)
    private boolean closed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /** actual members of the group */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();


    /** pending join requests (only used when closed=true) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_join_requests",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> joinRequests = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_admins",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )

    private Set<User> admins = new HashSet<>();

    public Set<User> getAdmins() {
        return admins;
    }
    public void setAdmins(Set<User> admins) {
        this.admins = admins;
    }
    public void addAdmin(User u) {
        admins.add(u);
    }
    public void removeAdmin(User u) {
        admins.remove(u);
    }

    /** posts in this group (new feature for 4.3) */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();


    public Group() {}

    public Group(String name, String description, boolean closed, User creator) {
        this.name = name;
        this.description = description;
        this.closed = closed;
        this.creator = creator;

        this.members.add(creator);        // creator is automatically a member
        this.admins .add(creator);        // creator is first admin
    }

    // --- getters & setters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public boolean isClosed() { return closed; }

    public void setClosed(boolean closed) { this.closed = closed; }

    public User getCreator() { return creator; }

    public void setCreator(User creator) { this.creator = creator; }

    public Set<User> getMembers() { return members; }

    public void setMembers(Set<User> members) { this.members = members; }

    public Set<User> getJoinRequests() { return joinRequests; }

    public void setJoinRequests(Set<User> joinRequests) { this.joinRequests = joinRequests; }

    public List<Post> getPosts() { return posts; }

    public void setPosts(List<Post> posts) { this.posts = posts; }

    // --- utility helpers ---

    public void addMember(User u) { members.add(u); }

    public void removeMember(User u) { members.remove(u);
       }

    public void addJoinRequest(User u) {
        joinRequests.add(u);
    }

    public void removeJoinRequest(User u) { joinRequests.remove(u);}

    /** New helpers for posts */

    public void addPost(Post p) {
        posts.add(p);
        p.setGroup(this);
    }

    public void removePost(Post p) {
        posts.remove(p);
        p.setGroup(null);
    }
}