# Sinistre-Pro — Team workflow and AI handoff

This file is the shared working agreement for every contributor and their AI assistant. Read it before changing the project.

## Project principles

1. Each service owns its code and data. **Never read or write another service's database.**
2. Services integrate only through documented REST APIs (OpenFeign) and Kafka events.
3. Keep commits focused and do not change another member's service or shared infrastructure without agreement.
4. `main` must remain runnable. Work on a feature branch and open a pull request for review before merging.
5. Do not invent fields, endpoints, state transitions, topics, or business rules without recording and agreeing on the change first.

## Proposed ownership

Assign names to these roles before implementation:

| Role | Main responsibility |
| --- | --- |
| Policy owner | `service-polices`: policies, guarantees, coverage-verification API, consumes `claim-declared` and `indemnity-paid`. |
| Claims owner | `service-sinistres`: claim lifecycle and expertise, emits `claim-declared` / `expertise-completed`, consumes `indemnity-paid`. |
| Integration owner | Shared contracts, Docker Compose, Kafka configuration, OpenFeign integration, end-to-end testing, and resolving cross-service issues. |

The indemnity service can be owned by the integration owner or shared with the claims owner. Name the owners in the table once the team decides.

## Architecture we are implementing

```text
                  REST / OpenFeign
service-sinistres ────────────────► service-polices
       │                                  │
       │ claim-declared                   │ consumes + records/alerts
       ├──────────────► Kafka ◄───────────┤
       │
       │ expertise-completed
       ├──────────────► Kafka ───────► service-indemnites
       │                                      │
       │   REST / OpenFeign                   │ indemnity-paid
       └◄─────────────────────────────────────┴────► Kafka ──► service-polices
```

Each service will have its own Spring Boot application and database. Docker Compose will start the three services, their databases, and Kafka. In a Compose network, use service names such as `http://service-polices:8080`, never `localhost`, for one container to call another.

## Integration contract: define before coding

Before a service feature is merged, its owner must publish the following in its README or OpenAPI documentation:

- endpoint, HTTP method, request JSON, response JSON, and possible error responses;
- the Kafka topic, event name, payload fields, and the precise business trigger;
- validation rules and state transitions;
- how another service can run/test it locally.

The integration owner reviews any contract used by more than one service. Consumers must tolerate unknown JSON fields so event payloads can evolve safely.

## Required communication flows

### 1. Open a claim

1. Client calls `service-sinistres`.
2. Claims service calls `service-polices` with OpenFeign.
3. Policy service confirms the policy is `ACTIVE` and returns the matching guarantee, including its ceiling and excess.
4. Only then does claims service persist a `DECLARE` claim and publish `claim-declared`.
5. Policy service consumes the event and records/notifies it.

### 2. Complete an expertise

1. Claims service records the expert’s assessment and retained amount.
2. It moves the claim to `EN_EXPERTISE` or its agreed completed-expertise state.
3. It publishes `expertise-completed` with at least `claimId` and the data the indemnity service needs to begin processing.
4. Indemnity service consumes the event and prepares the indemnity.

### 3. Pay compensation

1. Indemnity service uses OpenFeign to retrieve the authoritative `montantRetenu` from claims service.
2. It applies the guarantee's `franchise`, calculates and persists the payment.
3. After payment, it publishes `indemnity-paid`.
4. Claims service consumes it and moves the claim to `INDEMNISE`.
5. Policy service consumes it and updates the policy's premium history/rule.

## Working with Git

1. Pull the current `main` before starting work.
2. Create a branch named `feature/<service>-<short-description>`—for example, `feature/policies-guarantee-api`.
3. Make small, single-purpose commits using `type(scope): summary`, e.g. `feat(claims): publish expertise-completed event`.
4. Run the service's tests and the relevant integration flow before opening a PR.
5. In the PR, state: service changed, contract/event impact, tests run, and any required configuration change.

## Definition of done

A feature is complete only when it:

- validates inputs and handles expected errors;
- has unit tests;
- documents any API/event it introduces or changes;
- works in Docker Compose with the dependent services;
- does not break the end-to-end claim flow.

## Prompt to give an AI assistant

Copy this with the task you assign the AI:

```text
We are building the Sinistre-Pro Spring Boot microservices class project.
First read docs/ASSIGNMENT.md and docs/TEAM_WORKFLOW.md completely.
I own [SERVICE/FEATURE]. Work only within that scope unless I explicitly ask otherwise.
Respect the documented service boundaries: no shared database access; use OpenFeign only for synchronous calls and Kafka only for the specified events.
Before changing a cross-service API or event schema, show me the proposed contract and wait for approval.
Use a feature branch, make small changes, update relevant documentation, and run focused tests before reporting completion.
```

## Decisions still to make together

- Exact package/module layout and ports.
- Database choice and schema migrations (recommended: PostgreSQL + Flyway).
- Whether the course requires Eureka, Config Server, API Gateway, and authentication.
- Event payload schemas, retry/error strategy, and idempotency approach.
- The formula and rules for updating the policy premium after a paid claim.
