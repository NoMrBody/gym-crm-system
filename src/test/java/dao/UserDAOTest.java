package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDAOTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UserDAO userDAO;

    @Test
    @SuppressWarnings("unchecked")
    void existsByUsername_returnsTrueWhenCountPositive() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        assertTrue(userDAO.existsByUsername("Jane.Smith"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsByUsername_returnsFalseWhenCountZero() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);

        assertFalse(userDAO.existsByUsername("Jane.Smith"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUsername_found_returnsUser() {
        User user = new User();
        TypedQuery<User> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(user);

        Optional<User> result = userDAO.findByUsername("Jane.Smith");

        assertTrue(result.isPresent());
        verify(query).setParameter("username", "Jane.Smith");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUsername_notFound_returnsEmpty() {
        TypedQuery<User> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        assertTrue(userDAO.findByUsername("Ghost.User").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCredentials_valid_returnsUser() {
        User user = new User();
        TypedQuery<User> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(user);

        Optional<User> result = userDAO.findByCredentials("Jane.Smith", "secret");

        assertTrue(result.isPresent());
        verify(query).setParameter("username", "Jane.Smith");
        verify(query).setParameter("password", "secret");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCredentials_invalid_returnsEmpty() {
        TypedQuery<User> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        assertTrue(userDAO.findByCredentials("Jane.Smith", "wrong").isEmpty());
    }
}
