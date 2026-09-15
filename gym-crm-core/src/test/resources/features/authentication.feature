@component
Feature: Authentication
  Trainees and trainers receive a bearer token at registration and can log in again
  with the generated password. Repeated failures lock the account.

  @positive
  Scenario: A registered trainee can log in and use the token
    Given a registered trainee
    When they log in with the generated password
    Then they receive a bearer token
    And they request their trainee profile
    And the trainee profile is returned

  @negative
  Scenario: Login with the wrong password is rejected
    Given a registered trainee
    When they log in with the wrong password
    Then the response status is 401

  @negative
  Scenario: Three failed logins lock the account
    Given a registered trainee
    When they fail to log in 3 times
    Then the account is locked
    And the response status is 423

  @negative
  Scenario: A protected resource rejects a missing token
    When an unauthenticated caller requests a trainee profile
    Then the response status is 401

  @negative
  Scenario: A protected resource rejects a garbage token
    When they request a protected resource with a garbage token
    Then the response status is 401
