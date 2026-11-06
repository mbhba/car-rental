# Project Work Rules

## Domain Model and Architecture

- Use an object-oriented domain model. Objects own their rules and valid state.
- Do not move all logic into classes that only hold data.
- Use ports and adapters. The domain and use cases communicate through interfaces. Adapters handle HTTP and PostgreSQL.
- The domain must not depend on Spring or SQL.
- Keep period, transaction, and lock order rules from `spec.md`.

## Work Order and Tests

- Complete tasks in `spec.md` in order. Run the relevant tests after each task.
- Treat core tasks as the working solution. Keep unfinished nice-to-have tasks in the specification for discussion.
- Before implementation, check current stable technology versions. Record the result and any version changes in the README.
- Use unit tests to check behavior and business rules. Inject a clock for time tests.
- Use JaCoCo to measure production code coverage from unit tests. The minimum is 80%. The target is 90%.
- Use Testcontainers and real PostgreSQL in integration tests. Check concurrent requests and database constraints.

## Document Decisions

- Describe important decisions and trade-offs in code and in the README.
- Comments explain why a decision was made, especially for transactions, lock order, and database constraints.
- Record deliberate simplifications, omitted features, and run and test instructions in the README.
