package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.Trainer;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerDAOTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TrainerDAO trainerDAO;

    private Trainer trainer;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("Alice.Cooper");
        trainer = new Trainer();
        trainer.setUser(user);
    }

    @Test
    void save_newTrainer_persists() {
        Trainer saved = trainerDAO.save(trainer);

        verify(entityManager).persist(trainer);
        verify(entityManager, never()).merge(any());
        assertSame(trainer, saved);
    }

    @Test
    void save_existingTrainer_merges() {
        trainer.setId(1L);
        Trainer merged = new Trainer();
        when(entityManager.merge(trainer)).thenReturn(merged);

        Trainer saved = trainerDAO.save(trainer);

        verify(entityManager).merge(trainer);
        verify(entityManager, never()).persist(any());
        assertSame(merged, saved);
    }

    @Test
    void findById_delegatesToEntityManager() {
        when(entityManager.find(Trainer.class, 1L)).thenReturn(trainer);

        Optional<Trainer> result = trainerDAO.findById(1L);

        assertTrue(result.isPresent());
        assertSame(trainer, result.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUsername_found_returnsTrainer() {
        TypedQuery<Trainer> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Trainer.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(trainer);

        Optional<Trainer> result = trainerDAO.findByUsername("Alice.Cooper");

        assertTrue(result.isPresent());
        verify(query).setParameter("username", "Alice.Cooper");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUsername_notFound_returnsEmpty() {
        TypedQuery<Trainer> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Trainer.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        assertTrue(trainerDAO.findByUsername("Ghost.User").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsByUsername_returnsTrueWhenCountPositive() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(2L);

        assertTrue(trainerDAO.existsByUsername("Alice.Cooper"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_returnsResultList() {
        TypedQuery<Trainer> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Trainer.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(trainer));

        assertEquals(1, trainerDAO.findAll().size());
    }
}
