@component
Feature: Trainer workload queries
  Monthly totals are read over HTTP and require a valid bearer token.

  @positive
  Scenario: A summary can be read with a valid token
    Given the trainer has a 90 minute training on "2026-05-02"
    When someone requests the trainer summary with a valid token
    Then the response status is 200
    And the summary duration for 2026-5 is 90 minutes

  @negative
  Scenario: A summary request without a token is rejected
    Given a trainer with no recorded workload
    When someone requests the trainer summary without a token
    Then the response status is 401

  @negative
  Scenario: A summary request with a garbage token is rejected
    Given a trainer with no recorded workload
    When someone requests the trainer summary with a garbage token
    Then the response status is 401

  @negative
  Scenario: An unknown trainer cannot be found
    When someone requests the summary of an unknown trainer
    Then the response status is 404

  @negative
  Scenario: An out-of-range month is rejected
    Given the trainer has a 90 minute training on "2026-05-02"
    When someone requests the monthly workload for year 2026 month 13
    Then the response status is 400
