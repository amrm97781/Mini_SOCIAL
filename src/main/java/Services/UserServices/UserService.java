package Services.UserServices;

import Domain.Role;
import Domain.User;
import app.DTO.UserRegistrationDTO;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

@Stateless
public class UserService {

    @PersistenceContext(unitName = "persistence-unit")
    private EntityManager entityManager;

    public User registerUser(UserRegistrationDTO userDTO) {
        if (userDTO == null || userDTO.getEmail() == null || userDTO.getPassword() == null) {
            throw new IllegalArgumentException("Invalid user data");
        }

        // Check if the email already exists
        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", userDTO.getEmail());
        List<User> existingUsers = query.getResultList();
        if (!existingUsers.isEmpty()) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Create new User
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(encryptPassword(userDTO.getPassword()));  // Assume you have a method for password hashing
        user.setName(userDTO.getName());
        user.setBio(userDTO.getBio());
        user.setRole(Role.valueOf(userDTO.getRole().toUpperCase()));

        entityManager.persist(user);
        return user;
    }

   public String encryptPassword(String password) {
        // Use a secure hashing algorithm like BCrypt or PBKDF2
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public List<User> getAllUsers() {  return entityManager.createQuery("SELECT u FROM User u", User.class).getResultList();
    }
}
