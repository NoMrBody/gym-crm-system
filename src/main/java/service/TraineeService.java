package service;

import dao.TraineeDAO;
import model.Trainee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class TraineeService {
    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDAO traineeDAO;
    private ProfileService profileService;

    @Autowired
    public void setTraineeDAO(TraineeDAO traineeDAO) {
        this.traineeDAO = traineeDAO;
    }

    @Autowired
    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

    public Trainee create(Trainee trainee) {
        log.info("Creating profile for Trainee: {} {}", trainee.getFirstName(), trainee.getLastName());

        trainee.setUsername(profileService.generateUsername(trainee.getFirstName(), trainee.getLastName()));
        trainee.setPassword(profileService.generatePassword());
        trainee.setActive(true);

        return traineeDAO.create(trainee);
    }

    public Trainee update(Trainee trainee) {
        log.info("Updating Trainee profile: {}", trainee.getUsername());
        return traineeDAO.update(trainee);
    }

    public void delete(Long id) {
        log.info("Deleting Trainee profile with ID: {}", id);
        traineeDAO.delete(id);
    }

    public Trainee getById(Long id) {
        log.info("Fetching Trainee with ID: {}", id);
        return traineeDAO.getById(id);
    }

}
