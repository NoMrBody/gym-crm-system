@integration
Feature: Training to workload synchronisation
  A training created in gym-crm-core is reflected in trainer-workload-service after
  the ActiveMQ message is consumed. A failed training must not create workload.

  @positive
  Scenario: Adding a training updates the trainer workload
    Given a registered trainee in gym-crm
    And a registered trainer in gym-crm
    And the trainee is authenticated in gym-crm
    When they add a 60 minute training with the trainer
    Then the gym-crm response status is 200
    And the trainer workload shows 60 minutes in 2026-3

  @negative
  Scenario: Adding a training for an unknown trainer does not create workload
    Given a registered trainee in gym-crm
    And the trainee is authenticated in gym-crm
    When they add a training with an unknown trainer
    Then the gym-crm response status is 404
    And the unknown trainer still has no recorded workload
