package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserDAO {
    private static final Logger log = LoggerFactory.getLogger(UserDAO.class);

    // EntityManager for database operations
    @PersistenceContext
    private EntityManager entityManager;

    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    public Optional<User> findByUsername(String username) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.username = :username", User.class);
        query.setParameter("username", username);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            log.debug("No User found with username: {}", username);
            return Optional.empty();
        }
    }

    public Optional<User> findByCredentials(String username, String password) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.username = :username AND u.password = :password", User.class);
        query.setParameter("username", username);
        query.setParameter("password", password);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            log.debug("No User found matching provided credentials for username: {}", username);
            return Optional.empty();
        }
    }
}
