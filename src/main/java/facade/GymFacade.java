package facade;

import model.Trainee;
import model.Trainer;
import model.Training;
import service.TraineeService;
import service.TrainerService;
import service.TrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GymFacade {
    private static final Logger log = LoggerFactory.getLogger(GymFacade.class);

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    @Autowired
    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        log.info("GymFacade initialized with all services.");
    }

    // Trainee Operations

    public Trainee createTrainee(Trainee trainee) {
        return traineeService.create(trainee);
    }

    public Trainee updateTrainee(Trainee trainee) {
        return traineeService.update(trainee);
    }

    public void deleteTrainee(Long id) {
        traineeService.delete(id);
    }

    public Trainee getTrainee(Long id) {
        return traineeService.getById(id);
    }

    // Trainer Operations

    public Trainer createTrainer(Trainer trainer) {
        return trainerService.create(trainer);
    }

    public Trainer updateTrainer(Trainer trainer) {
        return trainerService.update(trainer);
    }

    public Trainer getTrainer(Long id) {
        return trainerService.getById(id);
    }

    // Training Operations

    public Training createTraining(Training training) {
        return trainingService.create(training);
    }

    public Training getTraining(Long id) {
        return trainingService.getById(id);
    }
}
