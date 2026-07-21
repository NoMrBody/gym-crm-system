package dto;

public record TrainerBrief(
        String username,
        String firstName,
        String lastName,
        TrainingTypeResponse specialization
) {
}
