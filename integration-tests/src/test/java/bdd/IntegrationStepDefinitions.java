package bdd;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationStepDefinitions {

    private static final long CARDIO_SPECIALIZATION_ID = 1L;

    private final IntegrationWorld world = new IntegrationWorld();
    private final IntegrationHttp http = new IntegrationHttp(world);

    @BeforeAll
    public static void startServices() {
        DualAppSupport.start();
    }

    @AfterAll
    public static void stopServices() {
        DualAppSupport.stop();
    }

    @Given("a registered trainee in gym-crm")
    public void aRegisteredTraineeInGymCrm() {
        world.setTraineeFirstName(world.unique("Jane"));
        world.setTraineeLastName(world.unique("Trainee"));
        http.post(DualAppSupport.coreBaseUrl(), "/api/v1/trainees", """
                {"firstName":"%s","lastName":"%s"}
                """.formatted(world.traineeFirstName(), world.traineeLastName()), null);
        assertEquals(201, world.status(), world.body());
        world.setTraineeUsername(JsonPath.read(world.body(), "$.username"));
        world.setTraineePassword(JsonPath.read(world.body(), "$.password"));
        world.setTraineeToken(JsonPath.read(world.body(), "$.accessToken"));
        world.setAccessToken(world.traineeToken());
    }

    @Given("a registered trainer in gym-crm")
    public void aRegisteredTrainerInGymCrm() {
        http.post(DualAppSupport.coreBaseUrl(), "/api/v1/trainers", """
                {"firstName":"%s","lastName":"%s","specializationId":%d}
                """.formatted(world.unique("Alice"), world.unique("Trainer"), CARDIO_SPECIALIZATION_ID), null);
        assertEquals(201, world.status(), world.body());
        world.setTrainerUsername(JsonPath.read(world.body(), "$.username"));
        world.setTrainerToken(JsonPath.read(world.body(), "$.accessToken"));
    }

    @Given("the trainee is authenticated in gym-crm")
    public void theTraineeIsAuthenticatedInGymCrm() {
        world.setAccessToken(world.traineeToken());
    }

    @Given("the trainee has a {int} minute training with the trainer")
    public void theTraineeHasATrainingWithTheTrainer(int duration) {
        aRegisteredTraineeInGymCrm();
        aRegisteredTrainerInGymCrm();
        theTraineeIsAuthenticatedInGymCrm();
        theyAddATrainingWithTheTrainer(duration);
        assertEquals(200, world.status(), world.body());
        awaitWorkloadSummary(200, 5_000);
    }

    @When("they log in to gym-crm")
    public void theyLogInToGymCrm() {
        http.post(DualAppSupport.coreBaseUrl(), "/api/v1/login", """
                {"username":"%s","password":"%s"}
                """.formatted(world.traineeUsername(), world.traineePassword()), null);
        assertEquals(200, world.status(), world.body());
        world.setAccessToken(JsonPath.read(world.body(), "$.accessToken"));
        world.setTraineeToken(world.accessToken());
    }

    @When("they add a {int} minute training with the trainer")
    public void theyAddATrainingWithTheTrainer(int duration) {
        http.post(DualAppSupport.coreBaseUrl(), "/api/v1/trainings", """
                {
                  "traineeUsername":"%s",
                  "trainerUsername":"%s",
                  "trainingName":"Morning cardio",
                  "trainingDate":"2026-03-12T10:00:00",
                  "trainingDuration":%d
                }
                """.formatted(world.traineeUsername(), world.trainerUsername(), duration),
                world.accessToken());
    }

    @When("they add a training with an unknown trainer")
    public void theyAddATrainingWithAnUnknownTrainer() {
        http.post(DualAppSupport.coreBaseUrl(), "/api/v1/trainings", """
                {
                  "traineeUsername":"%s",
                  "trainerUsername":"Ghost.Trainer",
                  "trainingName":"Morning cardio",
                  "trainingDate":"2026-03-12T10:00:00",
                  "trainingDuration":60
                }
                """.formatted(world.traineeUsername()), world.accessToken());
    }

    @When("they delete the trainee")
    public void theyDeleteTheTrainee() {
        http.delete(DualAppSupport.coreBaseUrl(), "/api/v1/trainees/" + world.traineeUsername(),
                world.accessToken());
    }

    @When("they delete an unknown trainee")
    public void theyDeleteAnUnknownTrainee() {
        http.delete(DualAppSupport.coreBaseUrl(), "/api/v1/trainees/Ghost.Trainee", world.accessToken());
    }

    @When("they request the trainer workload with the gym-crm token")
    public void theyRequestTheTrainerWorkloadWithTheGymCrmToken() {
        http.get(DualAppSupport.workloadBaseUrl(),
                "/api/v1/trainer-workloads/" + world.trainerUsername(), world.accessToken());
    }

    @When("they request the trainer workload without a token")
    public void theyRequestTheTrainerWorkloadWithoutAToken() {
        http.get(DualAppSupport.workloadBaseUrl(),
                "/api/v1/trainer-workloads/" + world.trainerUsername(), null);
    }

    @When("they request the trainer workload with a garbage token")
    public void theyRequestTheTrainerWorkloadWithAGarbageToken() {
        http.get(DualAppSupport.workloadBaseUrl(),
                "/api/v1/trainer-workloads/" + world.trainerUsername(), "not-a-jwt");
    }

    @When("they request workload for the unknown trainer")
    public void theyRequestWorkloadForTheUnknownTrainer() {
        http.get(DualAppSupport.workloadBaseUrl(), "/api/v1/trainer-workloads/Ghost.Trainer",
                world.accessToken());
    }

    @Then("the gym-crm response status is {int}")
    public void theGymCrmResponseStatusIs(int status) {
        assertEquals(status, world.status(), world.body());
    }

    @Then("the workload response status is {int}")
    public void theWorkloadResponseStatusIs(int status) {
        assertEquals(status, world.status(), world.body());
    }

    @Then("the trainer workload shows {int} minutes in {int}-{int}")
    public void theTrainerWorkloadShowsMinutesIn(int duration, int year, int month) {
        awaitWorkloadSummary(200, 5_000);
        assertEquals(200, world.status(), world.body());
        assertEquals(world.trainerUsername(), JsonPath.read(world.body(), "$.trainerUsername"));
        assertEquals(duration, monthDuration(year, month));
    }

    @Then("the trainer workload has no monthly totals")
    public void theTrainerWorkloadHasNoMonthlyTotals() {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            theyRequestTheTrainerWorkloadWithTheGymCrmToken();
            if (world.status() == 200 && ((Integer) JsonPath.read(world.body(), "$.years.length()")) == 0) {
                return;
            }
            sleep(50);
        }
        theyRequestTheTrainerWorkloadWithTheGymCrmToken();
        assertEquals(200, world.status(), world.body());
        assertEquals(0, (Integer) JsonPath.read(world.body(), "$.years.length()"));
    }

    @Then("the unknown trainer still has no recorded workload")
    public void theUnknownTrainerStillHasNoRecordedWorkload() {
        sleep(500);
        theyRequestWorkloadForTheUnknownTrainer();
        assertEquals(404, world.status(), world.body());
    }

    @Then("the existing trainer workload is unchanged at {int} minutes in {int}-{int}")
    public void theExistingTrainerWorkloadIsUnchanged(int duration, int year, int month) {
        theyRequestTheTrainerWorkloadWithTheGymCrmToken();
        assertEquals(200, world.status(), world.body());
        assertEquals(duration, monthDuration(year, month));
    }

    private void awaitWorkloadSummary(int expectedStatus, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            theyRequestTheTrainerWorkloadWithTheGymCrmToken();
            if (world.status() == expectedStatus) {
                return;
            }
            sleep(50);
        }
    }

    private int monthDuration(int year, int month) {
        int years = JsonPath.read(world.body(), "$.years.length()");
        for (int i = 0; i < years; i++) {
            int y = JsonPath.read(world.body(), "$.years[" + i + "].year");
            if (y != year) {
                continue;
            }
            int months = JsonPath.read(world.body(), "$.years[" + i + "].months.length()");
            for (int j = 0; j < months; j++) {
                int m = JsonPath.read(world.body(), "$.years[" + i + "].months[" + j + "].month");
                if (m == month) {
                    return JsonPath.read(world.body(),
                            "$.years[" + i + "].months[" + j + "].trainingSummaryDuration");
                }
            }
        }
        throw new AssertionError("No " + year + "-" + month + " entry in " + world.body());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
