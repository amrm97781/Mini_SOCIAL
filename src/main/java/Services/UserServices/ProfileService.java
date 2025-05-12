package Services.UserServices;

import Domain.User;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.mindrot.jbcrypt.BCrypt;

@Stateless
public class ProfileService {
    @EJB
    UserService userService;
    @PersistenceContext(unitName = "persistence-unit")
    private EntityManager entityManager;

    public User updateProfile(Long userId, String name, String bio, String email, String password) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setName(name);
        user.setBio(bio);
        user.setEmail(email);

        if (password != null && !password.isEmpty()) {
            user.setPassword(userService.encryptPassword(password));  // Optionally update password
        }

        entityManager.merge(user);
        return user;
    }

}
