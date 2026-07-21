package dto;

import java.time.LocalDate;
import java.util.List;

public record TraineeProfileResponse(
        String username,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean active,
        List<TrainerBrief> trainers
) {
}
