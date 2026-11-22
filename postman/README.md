# Postman collection

[`Sinistre-Pro.postman_collection.json`](Sinistre-Pro.postman_collection.json) holds the saved test scenarios of the project: 51 requests and 89 assertions, in French like the evaluation grid.

## Prerequisites

1. Applications running: `.\scripts\start-all.ps1 -WaitForHealth`. Wait until Eureka lists the three services (about 30 seconds).
2. For folder 04: Kafka (`.\scripts\start-kafka.ps1`) and Mailpit. See [`docs/LOCAL_DEVELOPMENT.md`](../docs/LOCAL_DEVELOPMENT.md).

## Run it

- **Postman:** Import → select the file → right-click the collection → **Run collection**.
- **Command line** (Node.js required):

  ```powershell
  npx newman@6 run postman/Sinistre-Pro.postman_collection.json
  ```

Every business request goes through the API gateway (`{{baseUrl}}` = `http://localhost:8080`). Each run creates its own data (a unique policyholder email), so the collection can be run again and again without restarting anything.

## Folders

| Folder | Grid | What it shows |
| --- | --- | --- |
| 00 - Infrastructure | V2–V3 | Config Server serves each service's file; Eureka lists the gateway and the three services as `UP` with consistent ids (`service-polices:8081`, …); the gateway routes to `lb://` names. |
| 01 - Polices | V1 | Policy and guarantee CRUD, coverage verification, validation (400) and not found (404). |
| 02 - Sinistres | V1, V4 | Claim declaration with the Feign coverage check (201, 422 not covered, 400 unknown policy), validation, expertise, invalid status (409). |
| 03 - Indemnités | V1 | Indemnity CRUD, one indemnity per claim (409), validation, not found. |
| 04 - Scénario complet | V4–V5 | The whole life of a claim: `claim-declared` → policy records it → email; expertise → `expertise-completed` → indemnity calculated through Feign (3500.00); payment → `indemnity-paid` → claim `INDEMNISE` and premium 600.00 → 660.00 → email. Emails are checked in Mailpit. |

Steps marked ⏱ wait `eventWaitMs` (8 s by default, a collection variable) for the Kafka events to be processed. Increase it on a slow machine.

## Variables

| Variable | Default |
| --- | --- |
| `baseUrl` | `http://localhost:8080` (gateway) |
| `configUrl` | `http://localhost:8888` |
| `eurekaUrl` | `http://localhost:8761` |
| `mailpitUrl` | `http://localhost:8025` |
| `eventWaitMs` | `8000` |

The other variables (ids, `runEmail`) are filled in by the requests' scripts during a run.
