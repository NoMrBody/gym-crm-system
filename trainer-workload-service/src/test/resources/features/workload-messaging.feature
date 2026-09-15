@component
Feature: Trainer workload messaging
  Workload updates arrive over JMS. Valid ADD and DELETE events change monthly totals;
  poison messages are dropped.

  @positive
  Scenario: An ADD event is applied and can be read back
    Given a trainer with no recorded workload
    When an ADD event of 60 minutes on "2026-03-12" arrives for that trainer
    And someone requests the trainer summary with a valid token
    Then the response status is 200
    And the summary first name is "Alice"
    And the summary duration for 2026-3 is 60 minutes
    When someone requests the monthly workload for year 2026 month 3
    Then the monthly duration is 60 minutes

  @negative
  Scenario: An invalid event is dropped
    Given a trainer with no recorded workload
    When an invalid ADD event arrives
    Then the trainer still has no recorded workload

  @positive
  Scenario: A DELETE event removes the monthly total
    Given the trainer has a 60 minute training on "2026-03-12"
    When a DELETE event of 60 minutes on "2026-03-12" arrives for that trainer
    Then the trainer summary has no monthly totals

  @negative
  Scenario: A DELETE event with no prior month still records the trainer
    Given a trainer with no recorded workload
    When a DELETE event of 45 minutes on "2026-03-12" arrives for that trainer
    Then the trainer summary has no monthly totals
