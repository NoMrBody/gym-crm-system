package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.Trainee;
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
class TraineeDAOTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TraineeDAO traineeDAO;

    private Trainee trainee;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("Jane.Smith");
        trainee = new Trainee();
        trainee.setUser(user);
    }

    @Test
    void save_newTrainee_persists() {
        Trainee saved = traineeDAO.save(trainee);

        verify(entityManager).persist(trainee);
        verify(entityManager, never()).merge(any());
        assertSame(trainee, saved);
    }

    @Test
    void save_existingTrainee_merges() {
        trainee.setId(1L);
        Trainee merged = new Trainee();
        when(entityManager.merge(trainee)).thenReturn(merged);

        Trainee saved = traineeDAO.save(trainee);

        verify(entityManager).merge(trainee);
        verify(entityManager, never()).persist(any());
        assertSame(merged, saved);
    }

    @Test
    void findById_delegatesToEntityManager() {
        when(entityManager.find(Trainee.class, 1L)).thenReturn(trainee);

        Optional<Trainee> result = traineeDAO.findById(1L);

        assertTrue(result.isPresent());
        assertSame(trainee, result.get());
    }

    @Test
    void findById_missing_returnsEmpty() {
        when(entityManager.find(Trainee.class, 99L)).thenReturn(null);

        assertTrue(traineeDAO.findById(99L).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUsername_found_returnsTrainee() {
        TypedQuery<Trainee> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Trainee.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(trainee);

        Optional<Trainee> result = traineeDAO.findByUsername("Jane.Smith");

        assertTrue(result.isPresent());
        assertSame(trainee, result.get());
        verify(query).setParameter("username", "Jane.Smith");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUsername_notFound_returnsEmpty() {
        TypedQuery<Trainee> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Trainee.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        assertTrue(traineeDAO.findByUsername("Ghost.User").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsByUsername_returnsTrueWhenCountPositive() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);

        assertTrue(traineeDAO.existsByUsername("Jane.Smith"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void existsByUsername_returnsFalseWhenCountZero() {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(0L);

        assertFalse(traineeDAO.existsByUsername("Jane.Smith"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteByUsername_existing_removesTrainee() {
        TypedQuery<Trainee> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Trainee.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(trainee);

        traineeDAO.deleteByUsername("Jane.Smith");

        verify(entityManager).remove(trainee);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteByUsername_missing_doesNotRemove() {
        TypedQuery<Trainee> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Trainee.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        traineeDAO.deleteByUsername("Ghost.User");

        verify(entityManager, never()).remove(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_returnsResultList() {
        TypedQuery<Trainee> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Trainee.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(trainee));

        List<Trainee> all = traineeDAO.findAll();

        assertEquals(1, all.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findUnassignedTrainers_returnsResultList() {
        TypedQuery<Trainer> query = mock(TypedQuery.class);
        Trainer trainer = new Trainer();
        when(entityManager.createQuery(anyString(), eq(Trainer.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(trainer));

        List<Trainer> result = traineeDAO.findUnassignedTrainers("Jane.Smith");

        assertEquals(1, result.size());
        verify(query).setParameter("username", "Jane.Smith");
    }
}
