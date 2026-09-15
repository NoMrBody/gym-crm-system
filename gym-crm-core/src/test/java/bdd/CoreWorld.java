package bdd;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import workload.TrainerWorkloadRequest;

import java.util.UUID;

@Component
@ScenarioScope
public class CoreWorld {

    private String traineeFirstName;
    private String traineeLastName;
    private String traineeUsername;
    private String traineePassword;
    private String traineeToken;

    private String trainerFirstName;
    private String trainerLastName;
    private String trainerUsername;
    private String trainerPassword;
    private String trainerToken;

    private String accessToken;
    private int status;
    private String body;
    private TrainerWorkloadRequest lastEvent;

    public String unique(String base) {
        return base + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public String traineeFirstName() {
        return traineeFirstName;
    }

    public void setTraineeFirstName(String traineeFirstName) {
        this.traineeFirstName = traineeFirstName;
    }

    public String traineeLastName() {
        return traineeLastName;
    }

    public void setTraineeLastName(String traineeLastName) {
        this.traineeLastName = traineeLastName;
    }

    public String traineeUsername() {
        return traineeUsername;
    }

    public void setTraineeUsername(String traineeUsername) {
        this.traineeUsername = traineeUsername;
    }

    public String traineePassword() {
        return traineePassword;
    }

    public void setTraineePassword(String traineePassword) {
        this.traineePassword = traineePassword;
    }

    public String traineeToken() {
        return traineeToken;
    }

    public void setTraineeToken(String traineeToken) {
        this.traineeToken = traineeToken;
    }

    public String trainerFirstName() {
        return trainerFirstName;
    }

    public void setTrainerFirstName(String trainerFirstName) {
        this.trainerFirstName = trainerFirstName;
    }

    public String trainerLastName() {
        return trainerLastName;
    }

    public void setTrainerLastName(String trainerLastName) {
        this.trainerLastName = trainerLastName;
    }

    public String trainerUsername() {
        return trainerUsername;
    }

    public void setTrainerUsername(String trainerUsername) {
        this.trainerUsername = trainerUsername;
    }

    public String trainerPassword() {
        return trainerPassword;
    }

    public void setTrainerPassword(String trainerPassword) {
        this.trainerPassword = trainerPassword;
    }

    public String trainerToken() {
        return trainerToken;
    }

    public void setTrainerToken(String trainerToken) {
        this.trainerToken = trainerToken;
    }

    public String accessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int status() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String body() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public TrainerWorkloadRequest lastEvent() {
        return lastEvent;
    }

    public void setLastEvent(TrainerWorkloadRequest lastEvent) {
        this.lastEvent = lastEvent;
    }
}
