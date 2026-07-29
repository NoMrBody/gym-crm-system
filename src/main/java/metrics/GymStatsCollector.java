package metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import repository.TraineeRepository;
import repository.TrainerRepository;
import repository.TrainingRepository;
import repository.UserRepository;

@Component
public class GymStatsCollector {
    private final TraineeRepository traineeRepository;
    private final TrainerRepository trainerRepository;
    private final TrainingRepository trainingRepository;
    private final UserRepository userRepository;
    private final GymMetrics gymMetrics;

    @Autowired
    public GymStatsCollector(TraineeRepository traineeRepository,
                             TrainerRepository trainerRepository,
                             TrainingRepository trainingRepository,
                             UserRepository userRepository,
                             GymMetrics gymMetrics) {
        this.traineeRepository = traineeRepository;
        this.trainerRepository = trainerRepository;
        this.trainingRepository = trainingRepository;
        this.userRepository = userRepository;
        this.gymMetrics = gymMetrics;
    }

    @Scheduled(fixedDelayString = "${gym.metrics.refresh-ms:30000}")
    @Transactional(readOnly = true)
    public void collectStats() {
        long trainers = trainerRepository.count();
        long trainees = traineeRepository.count();
        long trainings = trainingRepository.count();
        Long actives = userRepository.countByIsActiveTrue();

        gymMetrics.updateMetrics(trainees, trainers, actives, trainings);
    }


}
