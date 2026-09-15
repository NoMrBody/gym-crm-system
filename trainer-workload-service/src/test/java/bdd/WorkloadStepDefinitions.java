package bdd;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorkloadStepDefinitions {

    private static final String TRAINING_DATE = "2026-03-12";

    private final WorkloadWorld world;
    private final WorkloadHttpSupport http;
    private final WorkloadMessagingSupport messaging;

    public WorkloadStepDefinitions(WorkloadWorld world,
                                   WorkloadHttpSupport http,
                                   WorkloadMessagingSupport messaging) {
        this.world = world;
        this.http = http;
        this.messaging = messaging;
    }

    @Given("a trainer with no recorded workload")
    public void aTrainerWithNoRecordedWorkload() {
        world.newTrainer();
    }

    @Given("the trainer has a {int} minute training on {string}")
    public void theTrainerHasATrainingOn(int duration, String date) {
        world.newTrainer();
        messaging.send(world.trainerUsername(), world.trainerFirstName(), world.trainerLastName(),
                date, duration, "ADD");
        assertTrue(messaging.awaitTrainer(world.trainerUsername(), 5_000),
                "workload ADD was not applied for " + world.trainerUsername());
    }

    @When("an ADD event of {int} minutes on {string} arrives for that trainer")
    public void anAddEventArrives(int duration, String date) {
        messaging.send(world.trainerUsername(), world.trainerFirstName(), world.trainerLastName(),
                date, duration, "ADD");
        assertTrue(messaging.awaitTrainer(world.trainerUsername(), 5_000),
                "workload ADD was not applied for " + world.trainerUsername());
    }

    @When("a DELETE event of {int} minutes on {string} arrives for that trainer")
    public void aDeleteEventArrives(int duration, String date) {
        messaging.send(world.trainerUsername(), world.trainerFirstName(), world.trainerLastName(),
                date, duration, "DELETE");
        messaging.awaitQuietPeriod(500);
    }

    @When("an invalid ADD event arrives")
    public void anInvalidAddEventArrives() {
        messaging.sendInvalid(TRAINING_DATE, 60);
        messaging.awaitQuietPeriod(1_000);
    }

    @When("someone requests the trainer summary with a valid token")
    public void someoneRequestsTheTrainerSummaryWithAValidToken() throws Exception {
        world.setTokenMode("valid");
        http.getSummary(world.trainerUsername());
    }

    @When("someone requests the trainer summary without a token")
    public void someoneRequestsTheTrainerSummaryWithoutAToken() throws Exception {
        world.setTokenMode("none");
        http.getSummary(world.trainerUsername() == null ? "Anyone" : world.trainerUsername());
    }

    @When("someone requests the trainer summary with a garbage token")
    public void someoneRequestsTheTrainerSummaryWithAGarbageToken() throws Exception {
        world.setTokenMode("garbage");
        http.getSummary(world.trainerUsername() == null ? "Anyone" : world.trainerUsername());
    }

    @When("someone requests the monthly workload for year {int} month {int}")
    public void someoneRequestsTheMonthlyWorkload(int year, int month) throws Exception {
        world.setTokenMode("valid");
        http.getMonthly(world.trainerUsername(), year, month);
    }

    @When("someone requests the summary of an unknown trainer")
    public void someoneRequestsTheSummaryOfAnUnknownTrainer() throws Exception {
        world.newTrainer();
        world.setTokenMode("valid");
        http.getSummary("Ghost.Trainer");
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, world.status(), world.body());
    }

    @Then("the summary duration for {int}-{int} is {int} minutes")
    public void theSummaryDurationForIsMinutes(int year, int month, int duration) throws Exception {
        world.setTokenMode("valid");
        http.getSummary(world.trainerUsername());
        assertEquals(200, world.status(), world.body());
        assertEquals(world.trainerUsername(), JsonPath.read(world.body(), "$.trainerUsername"));
        assertEquals(duration, findMonthDuration(year, month));
    }

    @Then("the monthly duration is {int} minutes")
    public void theMonthlyDurationIsMinutes(int duration) {
        assertEquals(200, world.status(), world.body());
        assertEquals(duration, (Integer) JsonPath.read(world.body(), "$.trainingSummaryDuration"));
    }

    @Then("the trainer summary has no monthly totals")
    public void theTrainerSummaryHasNoMonthlyTotals() throws Exception {
        assertTrue(messaging.awaitTrainer(world.trainerUsername(), 5_000),
                "expected a workload profile for " + world.trainerUsername());
        world.setTokenMode("valid");
        http.getSummary(world.trainerUsername());
        assertEquals(200, world.status(), world.body());
        assertEquals(0, (Integer) JsonPath.read(world.body(), "$.years.length()"));
    }

    @Then("the trainer still has no recorded workload")
    public void theTrainerStillHasNoRecordedWorkload() throws Exception {
        world.setTokenMode("valid");
        http.getSummary(world.trainerUsername());
        assertEquals(404, world.status(), world.body());
    }

    @Then("the summary first name is {string}")
    public void theSummaryFirstNameIs(String firstName) {
        assertEquals(firstName, JsonPath.read(world.body(), "$.trainerFirstName"));
    }

    private int findMonthDuration(int year, int month) {
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
        assertFalse(true, "No " + year + "-" + month + " entry in " + world.body());
        return -1;
    }
}
