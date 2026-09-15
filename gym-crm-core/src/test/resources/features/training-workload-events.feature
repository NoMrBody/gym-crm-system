@component
Feature: Training workload events
  Adding a training is accepted by gym-crm-core and a matching ADD event is placed
  on the trainer workload queue. Failures must not publish an event.

  @positive
  Scenario: Adding a training publishes an ADD workload event
    Given a registered trainee
    And a registered trainer
    And the trainee is authenticated
    When they add a 60 minute training with the trainer
    Then the response status is 200
    And a workload ADD event is published for the trainer lasting 60 minutes

  @negative
  Scenario: Adding a training for an unknown trainer is rejected
    Given a registered trainee
    And the trainee is authenticated
    When they add a training with an unknown trainer
    Then the response status is 404
    And no workload event is published

  @negative
  Scenario: Adding a training with an invalid body is rejected
    Given a registered trainee
    And a registered trainer
    And the trainee is authenticated
    When they add a training with an invalid body
    Then the response status is 400
    And no workload event is published
