package bdd;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class CoreStepDefinitions {

    private static final long CARDIO_SPECIALIZATION_ID = 1L;

    private final CoreWorld world;
    private final CoreHttpSupport http;
    private final CoreMessagingSupport messaging;

    public CoreStepDefinitions(CoreWorld world, CoreHttpSupport http, CoreMessagingSupport messaging) {
        this.world = world;
        this.http = http;
        this.messaging = messaging;
    }

    @Before
    public void drainWorkloadQueue() {
        messaging.drainQueue();
    }

    @Given("a registered trainee")
    public void aRegisteredTrainee() throws Exception {
        world.setTraineeFirstName(world.unique("Jane"));
        world.setTraineeLastName(world.unique("Trainee"));
        registerTrainee(world.traineeFirstName(), world.traineeLastName());
        world.setTraineeUsername(JsonPath.read(world.body(), "$.username"));
        world.setTraineePassword(JsonPath.read(world.body(), "$.password"));
        world.setTraineeToken(JsonPath.read(world.body(), "$.accessToken"));
        world.setAccessToken(world.traineeToken());
    }

    @Given("a registered trainer")
    public void aRegisteredTrainer() throws Exception {
        world.setTrainerFirstName(world.unique("Alice"));
        world.setTrainerLastName(world.unique("Trainer"));
        registerTrainer(world.trainerFirstName(), world.trainerLastName(), CARDIO_SPECIALIZATION_ID);
        org.junit.jupiter.api.Assertions.assertEquals(201, world.status(), world.body());
        world.setTrainerUsername(JsonPath.read(world.body(), "$.username"));
        world.setTrainerPassword(JsonPath.read(world.body(), "$.password"));
        world.setTrainerToken(JsonPath.read(world.body(), "$.accessToken"));
    }

    @Given("the trainee is authenticated")
    public void theTraineeIsAuthenticated() {
        world.setAccessToken(world.traineeToken());
    }

    @When("they log in with the generated password")
    public void theyLogInWithTheGeneratedPassword() throws Exception {
        login(world.traineeUsername(), world.traineePassword());
        if (world.status() == 200) {
            world.setAccessToken(JsonPath.read(world.body(), "$.accessToken"));
        }
    }

    @When("they log in with the wrong password")
    public void theyLogInWithTheWrongPassword() throws Exception {
        login(world.traineeUsername(), "definitely-wrong");
    }

    @When("they fail to log in {int} times")
    public void theyFailToLogInTimes(int attempts) throws Exception {
        for (int i = 0; i < attempts; i++) {
            login(world.traineeUsername(), "definitely-wrong");
        }
    }

    @When("they request their trainee profile")
    public void theyRequestTheirTraineeProfile() throws Exception {
        http.getPath("/api/v1/trainees/" + world.traineeUsername());
    }

    @When("they request their trainer profile")
    public void theyRequestTheirTrainerProfile() throws Exception {
        world.setAccessToken(world.trainerToken());
        http.getPath("/api/v1/trainers/" + world.trainerUsername());
    }

    @When("someone registers a trainee without a first name")
    public void someoneRegistersATraineeWithoutAFirstName() throws Exception {
        http.postJson("/api/v1/trainees", """
                {"firstName":"","lastName":"%s"}
                """.formatted(world.unique("Blank")));
    }

    @When("an authenticated caller requests an unknown trainee")
    public void anAuthenticatedCallerRequestsAnUnknownTrainee() throws Exception {
        aRegisteredTrainee();
        http.getPath("/api/v1/trainees/Ghost.Trainee");
    }

    @When("someone registers a trainer with an unknown specialization")
    public void someoneRegistersATrainerWithAnUnknownSpecialization() throws Exception {
        registerTrainer(world.unique("Ghost"), world.unique("Spec"), 999L);
    }

    @When("an unauthenticated caller requests a trainer profile")
    public void anUnauthenticatedCallerRequestsATrainerProfile() throws Exception {
        aRegisteredTrainer();
        http.getPath("/api/v1/trainers/" + world.trainerUsername(), null);
    }

    @When("an unauthenticated caller requests a trainee profile")
    public void anUnauthenticatedCallerRequestsATraineeProfile() throws Exception {
        aRegisteredTrainee();
        http.getPath("/api/v1/trainees/" + world.traineeUsername(), null);
    }

    @When("they request a protected resource with a garbage token")
    public void theyRequestAProtectedResourceWithAGarbageToken() throws Exception {
        http.getPath("/api/v1/training-types", "not-a-jwt");
    }

    @When("they add a {int} minute training with the trainer")
    public void theyAddATrainingWithTheTrainer(int duration) throws Exception {
        http.postJson("/api/v1/trainings", """
                {
                  "traineeUsername":"%s",
                  "trainerUsername":"%s",
                  "trainingName":"Morning cardio",
                  "trainingDate":"2026-03-12T10:00:00",
                  "trainingDuration":%d
                }
                """.formatted(world.traineeUsername(), world.trainerUsername(), duration));
    }

    @When("they add a training with an unknown trainer")
    public void theyAddATrainingWithAnUnknownTrainer() throws Exception {
        http.postJson("/api/v1/trainings", """
                {
                  "traineeUsername":"%s",
                  "trainerUsername":"Ghost.Trainer",
                  "trainingName":"Morning cardio",
                  "trainingDate":"2026-03-12T10:00:00",
                  "trainingDuration":60
                }
                """.formatted(world.traineeUsername()));
    }

    @When("they add a training with an invalid body")
    public void theyAddATrainingWithAnInvalidBody() throws Exception {
        http.postJson("/api/v1/trainings", """
                {
                  "traineeUsername":"%s",
                  "trainerUsername":"%s",
                  "trainingName":"",
                  "trainingDate":"2026-03-12T10:00:00",
                  "trainingDuration":60
                }
                """.formatted(world.traineeUsername(), world.trainerUsername()));
    }

    @Then("they receive a bearer token")
    public void theyReceiveABearerToken() {
        org.junit.jupiter.api.Assertions.assertEquals(200, world.status());
        org.junit.jupiter.api.Assertions.assertFalse(
                ((String) JsonPath.read(world.body(), "$.accessToken")).isBlank());
        org.junit.jupiter.api.Assertions.assertEquals("Bearer", JsonPath.read(world.body(), "$.tokenType"));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        org.junit.jupiter.api.Assertions.assertEquals(status, world.status());
    }

    @Then("the trainee profile is returned")
    public void theTraineeProfileIsReturned() {
        org.junit.jupiter.api.Assertions.assertEquals(200, world.status());
        org.junit.jupiter.api.Assertions.assertEquals(
                world.traineeUsername(), JsonPath.read(world.body(), "$.username"));
        org.junit.jupiter.api.Assertions.assertEquals(
                world.traineeFirstName(), JsonPath.read(world.body(), "$.firstName"));
    }

    @Then("the trainer profile is returned")
    public void theTrainerProfileIsReturned() {
        org.junit.jupiter.api.Assertions.assertEquals(200, world.status());
        org.junit.jupiter.api.Assertions.assertEquals(
                world.trainerUsername(), JsonPath.read(world.body(), "$.username"));
        org.junit.jupiter.api.Assertions.assertEquals("Cardio", JsonPath.read(world.body(), "$.specialization.name"));
    }

    @Then("the account is locked")
    public void theAccountIsLocked() throws Exception {
        login(world.traineeUsername(), world.traineePassword());
        org.junit.jupiter.api.Assertions.assertEquals(423, world.status());
    }

    @Then("a workload ADD event is published for the trainer lasting {int} minutes")
    public void aWorkloadAddEventIsPublished(int duration) {
        world.setLastEvent(messaging.receiveEvent(2_000));
        org.junit.jupiter.api.Assertions.assertNotNull(world.lastEvent(), "expected a workload event on the queue");
        org.junit.jupiter.api.Assertions.assertEquals("ADD", world.lastEvent().actionType().name());
        org.junit.jupiter.api.Assertions.assertEquals(world.trainerUsername(), world.lastEvent().trainerUsername());
        org.junit.jupiter.api.Assertions.assertEquals(duration, world.lastEvent().trainingDuration());
    }

    @Then("no workload event is published")
    public void noWorkloadEventIsPublished() {
        org.junit.jupiter.api.Assertions.assertNull(messaging.receiveEvent(500));
    }

    private void registerTrainee(String firstName, String lastName) throws Exception {
        http.postJson("/api/v1/trainees", """
                {"firstName":"%s","lastName":"%s"}
                """.formatted(firstName, lastName), null);
        org.junit.jupiter.api.Assertions.assertEquals(201, world.status(), world.body());
    }

    private void registerTrainer(String firstName, String lastName, long specializationId) throws Exception {
        http.postJson("/api/v1/trainers", """
                {"firstName":"%s","lastName":"%s","specializationId":%d}
                """.formatted(firstName, lastName, specializationId), null);
    }

    private void login(String username, String password) throws Exception {
        http.postJson("/api/v1/login", """
                {"username":"%s","password":"%s"}
                """.formatted(username, password), null);
    }
}
