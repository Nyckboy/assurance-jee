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

## Health endpoint: where it is in the code

There is deliberately **no `HealthController` Java class** in this project. Spring Boot Actuator creates the endpoint automatically.

- The dependency that provides it is `spring-boot-starter-actuator` in [`api-gateway/pom.xml`](../api-gateway/pom.xml).
- The HTTP exposure is configured in [`api-gateway/src/main/resources/application.yml`](../api-gateway/src/main/resources/application.yml) under `management.endpoints.web.exposure`.
- When the gateway runs, visit `http://localhost:8080/actuator/health`. A response of `{"status":"UP"}` means that the gateway itself is running and its configured health checks are passing.

The current gateway health check does not prove that the downstream business services are running. Each service will later expose its own `/actuator/health` endpoint as well.

## Planned infrastructure: Eureka discovery server

**Do not implement this until the services have their first APIs.** This is the agreed next connectivity milestone before OpenFeign integration.

Eureka lets services register their current host and port at runtime. The gateway and other services then resolve a logical service name instead of depending on fixed URLs.

```text
service-polices ──registers──┐
service-sinistres ─registers─┼── Eureka Server :8761
service-indemnites ─registers┘
                             ↑
                         api-gateway
```

### Planned implementation checklist

1. Create a `discovery-server` Spring Boot module on port `8761` with the Eureka Server dependency and `@EnableEurekaServer`.
2. Add the Eureka Client dependency to `api-gateway` and all three business services.
3. Configure every client with `eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/` for local development.
4. Run the server, then the services; confirm registered applications at `http://localhost:8761`.
5. Add Spring Cloud LoadBalancer to the gateway and change the three route targets to logical names:

   ```text
   lb://service-polices
   lb://service-sinistres
   lb://service-indemnites
   ```

6. Keep the public paths `/api/policies/**`, `/api/claims/**`, and `/api/indemnities/**` unchanged. Only the gateway's internal target changes.

With this setup, the gateway discovers the actual running host and port through Eureka. If an instance stops sending heartbeats, Eureka eventually removes it from the registry. We will use explicit public routes with `lb://` targets rather than automatically exposing every discovered service under its raw service name.
