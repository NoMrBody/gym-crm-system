package service;

import model.TrainingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.TrainingTypeRepository;

import java.util.List;

@Service
public class TrainingTypeService {
    private static final Logger log = LoggerFactory.getLogger(TrainingTypeService.class);

    private TrainingTypeRepository trainingTypeRepository;

    @Autowired
    public void setTrainingTypeRepository(TrainingTypeRepository trainingTypeRepository) {
        this.trainingTypeRepository = trainingTypeRepository;
    }

    // Get the constant list of training types.
    @Transactional(readOnly = true)
    public List<TrainingType> getAll() {
        log.debug("Fetching all training types");
        return trainingTypeRepository.findAll();
    }
}
