package bdd;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ScenarioScope
public class WorkloadWorld {

    private String trainerUsername;
    private String trainerFirstName;
    private String trainerLastName;
    private int status;
    private String body;
    private String tokenMode = "valid";

    public void newTrainer() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        this.trainerUsername = "Trainer." + id;
        this.trainerFirstName = "Alice";
        this.trainerLastName = "Cooper";
        this.tokenMode = "valid";
    }

    public String trainerUsername() {
        return trainerUsername;
    }

    public String trainerFirstName() {
        return trainerFirstName;
    }

    public String trainerLastName() {
        return trainerLastName;
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

    public String tokenMode() {
        return tokenMode;
    }

    public void setTokenMode(String tokenMode) {
        this.tokenMode = tokenMode;
    }
}
