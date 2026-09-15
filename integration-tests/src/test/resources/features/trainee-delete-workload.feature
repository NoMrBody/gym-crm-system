@integration
Feature: Trainee deletion cancels workload
  Deleting a trainee in gym-crm-core publishes DELETE events for cascaded trainings,
  so the trainer's monthly total drops. A missing trainee is rejected and leaves
  existing workload alone.

  @positive
  Scenario: Deleting a trainee removes the training from workload
    Given the trainee has a 60 minute training with the trainer
    When they delete the trainee
    Then the gym-crm response status is 200
    And the trainer workload has no monthly totals

  @negative
  Scenario: Deleting an unknown trainee does not change workload
    Given the trainee has a 60 minute training with the trainer
    When they delete an unknown trainee
    Then the gym-crm response status is 404
    And the existing trainer workload is unchanged at 60 minutes in 2026-3
