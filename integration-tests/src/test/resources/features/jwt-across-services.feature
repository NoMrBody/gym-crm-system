@integration
Feature: JWT across services
  A bearer token issued by gym-crm-core is accepted by trainer-workload-service.
  Missing or garbage tokens are rejected.

  @positive
  Scenario: A gym-crm login token authorizes a workload query
    Given the trainee has a 45 minute training with the trainer
    When they log in to gym-crm
    And they request the trainer workload with the gym-crm token
    Then the workload response status is 200

  @negative
  Scenario: Workload rejects a missing token
    Given the trainee has a 45 minute training with the trainer
    When they request the trainer workload without a token
    Then the workload response status is 401

  @negative
  Scenario: Workload rejects a garbage token
    Given the trainee has a 45 minute training with the trainer
    When they request the trainer workload with a garbage token
    Then the workload response status is 401
