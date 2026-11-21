# service-polices

Owns insurance policies and their guarantees. It runs locally on port `8081` with an in-memory H2 database.

Its datasource and JPA settings are served by the config server from [`config-repo/service-polices.properties`](../config-repo/service-polices.properties); see [the config server documentation](../docs/CONFIG_SERVER.md).

## Local tools

- H2 console: `http://localhost:8081/h2-console`
- Health check: `http://localhost:8081/actuator/health`
- Gateway path: `http://localhost:8080/api/policies/**`

Use JDBC URL `jdbc:h2:mem:policiesdb` in the H2 console, username `sa`, and an empty password.

## API contract

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/policies` | Create a policy. |
| `GET` | `/api/policies` | List policies. |
| `GET` | `/api/policies/{policyId}` | Retrieve a policy and its guarantees. |
| `PUT` | `/api/policies/{policyId}` | Replace policy details. |
| `DELETE` | `/api/policies/{policyId}` | Delete a policy and its guarantees. |
| `POST` | `/api/policies/{policyId}/guarantees` | Add a guarantee. |
| `PUT` | `/api/policies/{policyId}/guarantees/{guaranteeId}` | Replace a guarantee. |
| `DELETE` | `/api/policies/{policyId}/guarantees/{guaranteeId}` | Delete a guarantee. |
| `GET` | `/api/policies/{policyId}/coverage?claimType=COLLISION` | Verify an active policy has a matching guarantee. |
| `GET` | `/api/policies/{policyId}/claims` | List the claims declared on a policy (recorded from `claim-declared`). |

The coverage endpoint is the contract that `service-sinistres` will consume later through OpenFeign. A valid request always receives `200 OK` with `covered: true` or `covered: false`; an unknown policy receives `404`.

## Kafka: consumes `claim-declared`

Published by `service-sinistres` when a covered claim is saved with status `DECLARE`. Contract agreed with the claims owner:

| Item | Value |
| --- | --- |
| Topic | `claim-declared` |
| Key | `claimId` as a string |
| Consumer group | `service-polices` |
| Payload | `claimId`, `policyId`, `souscripteurEmail`, `dateSinistre` (ISO date), `type`, `montantDeclare` (number) |

```json
{"claimId":12,"policyId":3,"souscripteurEmail":"insured@example.com","dateSinistre":"2026-11-01","type":"COLLISION","montantDeclare":4200.00}
```

On each message, the service records the claim in its own `policy_claims` table and notifies the policyholder (logged for now: there is no mail server yet).

The consumer is deliberately tolerant:

- the value is read as raw JSON, so the producer's type header (`__TypeId__`) is ignored;
- unknown fields are ignored, so the producer can add fields later;
- a `claimId` that was already recorded is skipped, so a redelivered message is harmless;
- a claim on an unknown policy, or an unreadable message, is logged and skipped instead of retried forever.

Renaming or removing one of the six fields requires agreement with the policy owner.

Kafka's address comes from `spring.kafka.bootstrap-servers` in [`config-repo/service-polices.properties`](../config-repo/service-polices.properties) (override with `KAFKA_BOOTSTRAP_SERVERS`, default `localhost:9092`). Without a running Kafka broker the service still starts and serves its API; it only logs connection warnings. The tests use an embedded Kafka broker and need nothing installed.

## Example

Create a policy:

```json
{
  "souscripteurEmail": "insured@example.com",
  "typeCouverture": "AUTO",
  "montantCouvert": 50000.00,
  "prime": 600.00,
  "dateEffet": "2026-01-01",
  "statut": "ACTIVE"
}
```

Then add a `COLLISION` guarantee with its `plafond` and `franchise`.
