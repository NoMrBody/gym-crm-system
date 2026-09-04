package service;

import jakarta.persistence.EntityNotFoundException;
import model.Trainee;
import model.Trainer;
import model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.TraineeRepository;
import repository.TrainerRepository;
import repository.TrainingRepository;
import util.ValidationUtils;
import workload.ActionType;
import workload.TrainingChangedEvent;

import java.time.LocalDateTime;

@Service
public class TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingRepository trainingRepository;
    private TraineeRepository traineeRepository;
    private TrainerRepository trainerRepository;
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public void setTrainingRepository(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Autowired
    public void setTraineeRepository(TraineeRepository traineeRepository) {
        this.traineeRepository = traineeRepository;
    }

    @Autowired
    public void setTrainerRepository(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    // Add Training (resolves trainee and trainer by username, derives training type from trainer's specialization, persists the training and links trainee/trainer)
    @Transactional
    public Training addTraining(String traineeUsername,
                                String trainerUsername,
                                String trainingName,
                                LocalDateTime trainingDate,
                                Integer trainingDuration) {
        Trainee trainee = traineeRepository.findByUser_Username(traineeUsername)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trainee not found with username: " + traineeUsername));
        Trainer trainer = trainerRepository.findByUser_Username(trainerUsername)
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

        Training saved = trainingRepository.save(training);

        trainee.getTrainers().add(trainer);
        trainer.getTrainees().add(trainee);

        // Delivered to trainer-workload-service only once this transaction commits.
        eventPublisher.publishEvent(TrainingChangedEvent.of(ActionType.ADD, saved));

        log.info("Added training '{}' for Trainee '{}' with Trainer '{}'",
                trainingName, traineeUsername, trainerUsername);
        return saved;
    }
}
