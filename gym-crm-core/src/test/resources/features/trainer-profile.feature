@component
Feature: Trainer profile
  Public registration creates a trainer against a known specialization. The profile
  can be read with a valid token.

  @positive
  Scenario: A registered trainer can read their profile
    Given a registered trainer
    When they request their trainer profile
    Then the trainer profile is returned

  @negative
  Scenario: Registration with an unknown specialization is rejected
    When someone registers a trainer with an unknown specialization
    Then the response status is 404

  @negative
  Scenario: An unauthenticated caller cannot read a trainer profile
    When an unauthenticated caller requests a trainer profile
    Then the response status is 401
