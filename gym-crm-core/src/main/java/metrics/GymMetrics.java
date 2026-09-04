package metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import model.Training;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
public class GymMetrics {
    private final Counter traineeRegistrations;
    private final Counter trainerRegistrations;
    private final Counter authSuccesses;
    private final Counter authFailures;
    private final Timer trainingCreation;
    private final AtomicLong traineesTotal = new AtomicLong(0);
    private final AtomicLong trainersTotal = new AtomicLong(0);
    private final AtomicLong trainingsTotal = new AtomicLong(0);
    private final AtomicLong activeUsersTotal = new AtomicLong(0);

    public GymMetrics(MeterRegistry registry) {
        this.traineeRegistrations = Counter.builder("gym.registrations")
                .tag("role", "trainee")
                .register(registry);

        this.trainerRegistrations = Counter.builder("gym.registrations")
                .tag("role", "trainer")
                .register(registry);

        this.authSuccesses = Counter.builder("gym.auth.attempts")
                .tag("result", "success")
                .register(registry);

        this.authFailures = Counter.builder("gym.auth.attempts")
                .tag("result", "failure")
                .register(registry);

        this.trainingCreation = Timer.builder("gym.training.creation")
                .register(registry);

        Gauge.builder("gym.trainees.total", traineesTotal, AtomicLong::get)
                .register(registry);
        Gauge.builder("gym.trainers.total", trainersTotal, AtomicLong::get)
                .register(registry);
        Gauge.builder("gym.trainings.total", trainingsTotal, AtomicLong::get)
                .register(registry);
        Gauge.builder("gym.active.users.total", activeUsersTotal, AtomicLong::get)
                .register(registry);
    }

    public void recordTraineeRegistration() {
        traineeRegistrations.increment();
        traineesTotal.incrementAndGet();
    }

    public void recordTrainerRegistration() {
        trainerRegistrations.increment();
        trainersTotal.incrementAndGet();
    }

    public void recordAuthResult(boolean success) {
        (success ? authSuccesses : authFailures).increment();
    }

    public Training recordTrainingCreation(Supplier<Training> trainingCreationLogic) {
        Training saved = trainingCreation.record(trainingCreationLogic);
        trainingsTotal.incrementAndGet();
        return saved;
    }

    public void updateMetrics(long traineeRegistrations,
                              long trainerRegistrations,
                              long activeUsersTotal,
                              long trainingsTotal){
        this.traineesTotal.set(traineeRegistrations);
        this.trainersTotal.set(trainerRegistrations);
        this.activeUsersTotal.set(activeUsersTotal);
        this.trainingsTotal.set(trainingsTotal);
    }

    public Double getAuthSuccesses() {
        return authSuccesses.count();
    }

    public Double getAuthFailures() {
        return authFailures.count();
    }

}
