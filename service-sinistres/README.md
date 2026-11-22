# service-sinistres

Claims service (`http://localhost:8082`): creates and retrieves insurance claims and their expert assessments. It validates coverage by REST/OpenFeign against `service-polices`; it does not access any other service's database.

## Kafka contract: `claim-declared`

- **Topic:** `claim-declared`
- **Producer:** `service-sinistres`
- **Key:** the persisted `claimId` converted to a string
- **Trigger:** once per successfully persisted **covered** claim, only after its local database transaction commits. No emission if coverage is missing, denied, unavailable, or persistence rolls back
- **Value:** JSON representation of `ma.sinistrepro.claims.event.ClaimDeclaredEvent`, retaining all six properties:

```json
{"claimId":12,"policyId":3,"souscripteurEmail":"assure@example.com","dateSinistre":"2026-11-01","type":"COLLISION","montantDeclare":4200.00}
```

`dateSinistre` is an ISO-8601 date string (`yyyy-MM-dd`); `montantDeclare` is a JSON number, not a string. Type headers are disabled. The service-polices consumer tolerates unknown fields and deduplicates by `claimId`; renaming or removing any contracted field requires agreement.

The service uses Spring Kafka 4 `JacksonJsonSerializer`. `@TransactionalEventListener(AFTER_COMMIT)` prevents emission for rolled-back local transactions; **it does not provide durable exactly-once delivery across DB and Kafka**. Production-grade recovery requires a transactional outbox, idempotent delivery handling, and monitoring/retry of failed asynchronous sends.

## Configuration

Local defaults: H2 database, `service-polices` at `http://localhost:8081`, Kafka at `localhost:9092`. The optional Config Server endpoint defaults to `http://localhost:8888`. Override via `POLICIES_SERVICE_URL`, `KAFKA_BOOTSTRAP_SERVERS`, or `CONFIG_SERVER_URL`. Config Server is disabled during tests.

## Test

From this module directory:

```bash
./mvnw clean test
```

`ClaimDeclaredKafkaIntegrationTest` uses embedded Kafka and mocks `PolicyClient` to test the producer without requiring `service-polices` or external Docker Kafka. Run-time Kafka requires a reachable broker when publishing events.
