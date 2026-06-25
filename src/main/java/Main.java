import config.SpringConfig;
import facade.GymFacade;
import model.Trainee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Date;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting Gym CRM Application...");

        ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);

        GymFacade facade = context.getBean(GymFacade.class);

        Trainee trainee1 = new Trainee();
        trainee1.setFirstName("John");
        trainee1.setLastName("Doe");
        trainee1.setDateOfBirth(new Date());
        trainee1.setAddress("123 Main St");

        facade.createTrainee(trainee1);
        log.info("Created Trainee 1 Username: {}", trainee1.getUsername());

        Trainee trainee2 = new Trainee();
        trainee2.setFirstName("John");
        trainee2.setLastName("Doe");
        trainee2.setDateOfBirth(new Date());
        trainee2.setAddress("456 Elm St");

        facade.createTrainee(trainee2);
        log.info("Created Trainee 2 Username: {}", trainee2.getUsername());
    }
}