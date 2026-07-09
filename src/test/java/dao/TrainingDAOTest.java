package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import model.Training;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingDAOTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TrainingDAO trainingDAO;

    @Test
    void save_persistsTraining() {
        Training training = new Training();

        Training saved = trainingDAO.save(training);

        verify(entityManager).persist(training);
        assertSame(training, saved);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findTraineeTrainings_noOptionalCriteria_usesBaseQuery() {
        TypedQuery<Training> query = mock(TypedQuery.class);
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        when(entityManager.createQuery(jpql.capture(), eq(Training.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        trainingDAO.findTraineeTrainings("Jane.Smith", null, null, null, null);

        String sql = jpql.getValue();
        assertTrue(sql.contains("t.trainee.user.username = :traineeUsername"));
        assertFalse(sql.contains(":fromDate"));
        assertFalse(sql.contains(":trainerName"));
        assertFalse(sql.contains(":trainingTypeName"));
        verify(query).setParameter("traineeUsername", "Jane.Smith");
        verify(query, never()).setParameter(eq("fromDate"), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findTraineeTrainings_allCriteria_appendsClausesAndBindsParams() {
        TypedQuery<Training> query = mock(TypedQuery.class);
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        when(entityManager.createQuery(jpql.capture(), eq(Training.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new Training()));

        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        List<Training> result =
                trainingDAO.findTraineeTrainings("Jane.Smith", from, to, "Alice.Cooper", "Yoga");

        assertEquals(1, result.size());
        String sql = jpql.getValue();
        assertTrue(sql.contains(":fromDate"));
        assertTrue(sql.contains(":toDate"));
        assertTrue(sql.contains(":trainerName"));
        assertTrue(sql.contains(":trainingTypeName"));
        verify(query).setParameter("traineeUsername", "Jane.Smith");
        verify(query).setParameter("fromDate", from.atStartOfDay());
        verify(query).setParameter("trainerName", "Alice.Cooper");
        verify(query).setParameter("trainingTypeName", "Yoga");
    }

    @Test
    @SuppressWarnings("unchecked")
    void findTrainerTrainings_withTraineeName_appendsClause() {
        TypedQuery<Training> query = mock(TypedQuery.class);
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        when(entityManager.createQuery(jpql.capture(), eq(Training.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        trainingDAO.findTrainerTrainings("Alice.Cooper", null, null, "Jane.Smith");

        String sql = jpql.getValue();
        assertTrue(sql.contains("t.trainer.user.username = :trainerUsername"));
        assertTrue(sql.contains(":traineeName"));
        assertFalse(sql.contains(":fromDate"));
        verify(query).setParameter("trainerUsername", "Alice.Cooper");
        verify(query).setParameter("traineeName", "Jane.Smith");
    }
}
