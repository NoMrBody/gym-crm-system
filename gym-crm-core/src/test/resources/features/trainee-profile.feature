@component
Feature: Trainee profile
  Public registration creates a trainee. The profile can be read with a valid token.

  @positive
  Scenario: A registered trainee can read their profile
    Given a registered trainee
    And the trainee is authenticated
    When they request their trainee profile
    Then the trainee profile is returned

  @negative
  Scenario: Registration without a first name is rejected
    When someone registers a trainee without a first name
    Then the response status is 400

  @negative
  Scenario: An unknown trainee cannot be found
    When an authenticated caller requests an unknown trainee
    Then the response status is 404
