# API gateway

`api-gateway` is the single public entry point for Sinistre-Pro. It runs on port `8080` and has no database or business logic.

## Routes

| Request received by the gateway | Forwarded to | Default local target |
| --- | --- | --- |
| `/api/policies/**` | `service-polices` | `http://localhost:8081` |
| `/api/claims/**` | `service-sinistres` | `http://localhost:8082` |
| `/api/indemnities/**` | `service-indemnites` | `http://localhost:8083` |

The full path is forwarded unchanged. For example, `GET /api/policies/42` reaches the policies service as `GET /api/policies/42`.

## How target addresses are chosen

The configuration provides local defaults, but environment variables can override them without changing source code:

```text
POLICIES_SERVICE_URL=http://service-polices:8080
CLAIMS_SERVICE_URL=http://service-sinistres:8080
INDEMNITIES_SERVICE_URL=http://service-indemnites:8080
```

Those Docker hostnames will be used later in Docker Compose. When the services are run directly on one machine, their planned ports are `8081`, `8082`, and `8083`.

## What belongs here

- Public route forwarding
- Health monitoring at `/actuator/health`
- Later: authentication, CORS policy, rate limiting, and request logging

## What does not belong here

- Policy, claim, or indemnity business rules
- Database access
- OpenFeign calls between services
- Kafka producers or consumers

Services communicate directly with each other through the OpenFeign and Kafka flows required by the assignment. The gateway is only for outside callers such as a frontend, Postman, or a mobile app.
