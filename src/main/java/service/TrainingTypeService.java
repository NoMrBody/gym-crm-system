package service;

import dao.TrainingTypeDAO;
import model.TrainingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainingTypeService {
    private static final Logger log = LoggerFactory.getLogger(TrainingTypeService.class);

    private TrainingTypeDAO trainingTypeDAO;

    @Autowired
    public void setTrainingTypeDAO(TrainingTypeDAO trainingTypeDAO) {
        this.trainingTypeDAO = trainingTypeDAO;
    }

    // Get the constant list of training types.
    @Transactional(readOnly = true)
    public List<TrainingType> getAll() {
        log.debug("Fetching all training types");
        return trainingTypeDAO.findAll();
    }
}
