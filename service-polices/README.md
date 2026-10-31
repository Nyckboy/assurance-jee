# service-polices

Owns insurance policies and their guarantees. It runs locally on port `8081` with an in-memory H2 database.

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

The coverage endpoint is the contract that `service-sinistres` will consume later through OpenFeign. A valid request always receives `200 OK` with `covered: true` or `covered: false`; an unknown policy receives `404`.

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
