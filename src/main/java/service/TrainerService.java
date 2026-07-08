package service;

import dao.TrainerDAO;
import model.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class TrainerService {
    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDAO trainerDAO;
    private ProfileService profileService;


    @Autowired
    public void setTrainerDAO(TrainerDAO trainerDAO) {
        this.trainerDAO = trainerDAO;
    }

    @Autowired
    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

    public Trainer create(Trainer trainer) {
        log.info("Creating profile for Trainer: {} {}", trainer.getFirstName(), trainer.getLastName());

        trainer.setUsername(profileService.generateUsername(trainer.getFirstName(), trainer.getLastName()));
        trainer.setPassword(profileService.generatePassword());
        trainer.setActive(true);

        return trainerDAO.create(trainer);
    }

    public Trainer update(Trainer trainer) {
        log.info("Updating Trainer profile: {}", trainer.getUsername());
        return trainerDAO.update(trainer);
    }

    public Trainer getById(Long id) {
        log.info("Fetching Trainer with ID: {}", id);
        return trainerDAO.getById(id);
    }

}
