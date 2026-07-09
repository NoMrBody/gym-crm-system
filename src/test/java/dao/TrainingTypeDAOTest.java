package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import model.TrainingType;
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
class TrainingTypeDAOTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TrainingTypeDAO trainingTypeDAO;

    @Test
    @SuppressWarnings("unchecked")
    void findAll_returnsResultList() {
        TypedQuery<TrainingType> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(TrainingType.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new TrainingType(), new TrainingType()));

        assertEquals(2, trainingTypeDAO.findAll().size());
    }

    @Test
    void findById_delegatesToEntityManager() {
        TrainingType type = new TrainingType();
        when(entityManager.find(TrainingType.class, 3L)).thenReturn(type);

        Optional<TrainingType> result = trainingTypeDAO.findById(3L);

        assertTrue(result.isPresent());
        assertSame(type, result.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByName_found_returnsType() {
        TrainingType type = new TrainingType();
        TypedQuery<TrainingType> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(TrainingType.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(type);

        Optional<TrainingType> result = trainingTypeDAO.findByName("Yoga");

        assertTrue(result.isPresent());
        verify(query).setParameter("name", "Yoga");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByName_notFound_returnsEmpty() {
        TypedQuery<TrainingType> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(TrainingType.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        assertTrue(trainingTypeDAO.findByName("Unknown").isEmpty());
    }
}
