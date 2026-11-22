# API gateway

`api-gateway` is the single public entry point for Sinistre-Pro. It runs on port `8080` and has no database or business logic.

## Routes

| Request received by the gateway | Forwarded to |
| --- | --- |
| `/api/policies/**` | `lb://service-polices` |
| `/api/claims/**` | `lb://service-sinistres` |
| `/api/indemnities/**` | `lb://service-indemnites` |

The full path is forwarded unchanged. For example, `GET /api/policies/42` reaches the policies service as `GET /api/policies/42`.

## How target addresses are chosen

The routes are defined in [`config-repo/api-gateway.yml`](../config-repo/api-gateway.yml) and served by the [config server](CONFIG_SERVER.md); the gateway's local `application.yml` only keeps its name, port, and config import.

An `lb://<name>` target is resolved through the [discovery server](DISCOVERY_SERVER.md): the gateway asks Eureka for the registered instances of that service and load-balances across them (round robin). No host or port is written in the routes, so starting a second instance of a service is enough to share the load. If no instance of a service is registered, its route fails with `Unable to find instance for <name>`.

## Each request is forwarded once

The gateway never re-sends a request, even when the service answers `503`. Retries belong to the caller and to the services' own Feign calls (Retry and Circuit Breaker), and re-sending a `POST` could create a resource twice. Apache HttpClient, which the Eureka client brings in, retries some requests by default, so [`GatewayHttpClientConfig`](../api-gateway/src/main/java/ma/sinistrepro/gateway/config/GatewayHttpClientConfig.java) turns that off. `GatewayForwardingTests` checks it.

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
- The HTTP exposure is configured in [`config-repo/api-gateway.yml`](../config-repo/api-gateway.yml) under `management.endpoints.web.exposure`.
- When the gateway runs, visit `http://localhost:8080/actuator/health`. A response of `{"status":"UP"}` means that the gateway itself is running and its configured health checks are passing.

The current gateway health check does not prove that the downstream business services are running. Each service will later expose its own `/actuator/health` endpoint as well.

## Discovery

The gateway is itself an Eureka client (`spring-cloud-starter-netflix-eureka-client`, which also brings Spring Cloud LoadBalancer). See [the discovery server documentation](DISCOVERY_SERVER.md). We use explicit public routes with `lb://` targets rather than automatically exposing every discovered service under its raw service name.
