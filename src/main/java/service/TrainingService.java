package service;

import dao.TraineeDAO;
import dao.TrainerDAO;
import dao.TrainingDAO;
import jakarta.persistence.EntityNotFoundException;
import model.Trainee;
import model.Trainer;
import model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import util.ValidationUtils;

import java.time.LocalDateTime;

@Service
public class TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDAO trainingDAO;
    private TraineeDAO traineeDAO;
    private TrainerDAO trainerDAO;

    @Autowired
    public void setTrainingDAO(TrainingDAO trainingDAO) {
        this.trainingDAO = trainingDAO;
    }

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) {
        this.traineeDAO = traineeDAO;
    }

    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    // Add Training (resolves trainee and trainer by username, derives training type from trainer's specialization, persists the training and links trainee/trainer)
    @Transactional
    public Training addTraining(String traineeUsername,
                                String trainerUsername,
                                String trainingName,
                                LocalDateTime trainingDate,
                                Integer trainingDuration) {
        Trainee trainee = traineeDAO.findByUsername(traineeUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainee not found with username: " + traineeUsername));
        Trainer trainer = trainerDAO.findByUsername(trainerUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainer not found with username: " + trainerUsername));

        Training training = new Training();
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingType(trainer.getSpecialization());
        training.setTrainingName(trainingName);
        training.setTrainingDate(trainingDate);
        training.setTrainingDuration(trainingDuration);
        ValidationUtils.validateTraining(training);

        Training saved = trainingDAO.save(training);

        trainee.getTrainers().add(trainer);
        trainer.getTrainees().add(trainee);

        log.info("Added training '{}' for Trainee '{}' with Trainer '{}'",
                trainingName, traineeUsername, trainerUsername);
        return saved;
    }
}
