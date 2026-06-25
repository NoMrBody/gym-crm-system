package service;

import dao.TrainingDAO;
import model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrainingService {
    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDAO trainingDAO;

    @Autowired
    public void setTrainingDAO(TrainingDAO trainingDAO) {
        this.trainingDAO = trainingDAO;
    }

    public Training create(Training training) {
        log.info("Creating new Training record: {}", training.getTrainingName());
        return trainingDAO.create(training);
    }

    public Training getById(Long id) {
        log.info("Fetching Training with ID: {}", id);
        return trainingDAO.getById(id);
    }
}