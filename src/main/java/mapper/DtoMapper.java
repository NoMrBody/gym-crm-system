package mapper;

import dto.CredentialsResponse;
import dto.TraineeBrief;
import dto.TraineeProfileResponse;
import dto.TraineeRegistrationRequest;
import dto.TrainerBrief;
import dto.TrainerProfileResponse;
import dto.TrainerRegistrationRequest;
import dto.TrainingResponse;
import dto.TrainingTypeResponse;
import dto.UpdateTraineeRequest;
import dto.UpdateTrainerRequest;
import model.Trainee;
import model.Trainer;
import model.Training;
import model.TrainingType;
import model.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts between JPA entities and the rest dtos. Collection-backed conversions rely on the
 * request-scoped persistence context (open-in-view) so lazy associations can be traversed.
 */
@Component
public class DtoMapper {

    // requests -> entities

    public Trainee toTraineeEntity(TraineeRegistrationRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        Trainee trainee = new Trainee();
        trainee.setUser(user);
        trainee.setDateOfBirth(request.dateOfBirth());
        trainee.setAddress(request.address());
        return trainee;
    }

    public Trainer toTrainerEntity(TrainerRegistrationRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        TrainingType specialization = new TrainingType();
        specialization.setId(request.specializationId());
        Trainer trainer = new Trainer();
        trainer.setUser(user);
        trainer.setSpecialization(specialization);
        return trainer;
    }

    public Trainee toTraineeEntity(UpdateTraineeRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setActive(Boolean.TRUE.equals(request.isActive()));
        Trainee trainee = new Trainee();
        trainee.setUser(user);
        trainee.setDateOfBirth(request.dateOfBirth());
        trainee.setAddress(request.address());
        return trainee;
    }

    public Trainer toTrainerEntity(UpdateTrainerRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setActive(Boolean.TRUE.equals(request.isActive()));
        Trainer trainer = new Trainer();
        trainer.setUser(user);
        return trainer;
    }

    // entities -> responses

    public CredentialsResponse toCredentials(User user) {
        return new CredentialsResponse(user.getUsername(), user.getPassword());
    }

    public TrainingTypeResponse toTrainingType(TrainingType type) {
        if (type == null) {
            return null;
        }
        return new TrainingTypeResponse(type.getId(), type.getTrainingTypeName());
    }

    public TrainerBrief toTrainerBrief(Trainer trainer) {
        User user = trainer.getUser();
        return new TrainerBrief(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                toTrainingType(trainer.getSpecialization()));
    }

    public TraineeBrief toTraineeBrief(Trainee trainee) {
        User user = trainee.getUser();
        return new TraineeBrief(user.getUsername(), user.getFirstName(), user.getLastName());
    }

    public TraineeProfileResponse toTraineeProfile(Trainee trainee) {
        User user = trainee.getUser();
        List<TrainerBrief> trainers = trainee.getTrainers().stream()
                .map(this::toTrainerBrief)
                .toList();
        return new TraineeProfileResponse(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                user.isActive(),
                trainers);
    }

    public TrainerProfileResponse toTrainerProfile(Trainer trainer) {
        User user = trainer.getUser();
        List<TraineeBrief> trainees = trainer.getTrainees().stream()
                .map(this::toTraineeBrief)
                .toList();
        return new TrainerProfileResponse(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                toTrainingType(trainer.getSpecialization()),
                user.isActive(),
                trainees);
    }

    public List<TrainerBrief> toTrainerBriefs(List<Trainer> trainers) {
        return trainers.stream().map(this::toTrainerBrief).toList();
    }

    public TrainingResponse toTrainingResponse(Training training) {
        User trainerUser = training.getTrainer().getUser();
        User traineeUser = training.getTrainee().getUser();
        return new TrainingResponse(
                training.getTrainingName(),
                training.getTrainingDate(),
                training.getTrainingType() != null ? training.getTrainingType().getTrainingTypeName() : null,
                training.getTrainingDuration(),
                trainerUser.getFirstName() + " " + trainerUser.getLastName(),
                traineeUser.getFirstName() + " " + traineeUser.getLastName());
    }

    public List<TrainingResponse> toTrainingResponses(List<Training> trainings) {
        return trainings.stream().map(this::toTrainingResponse).toList();
    }
}
