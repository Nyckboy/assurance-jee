# Config server

`config-server` is a Spring Cloud Config Server that serves the configuration of every Sinistre-Pro application from one place. It runs on port `8888` and has no database or business logic.

## How it works

The server uses the `native` profile: it reads plain files from the [`config-repo/`](../config-repo) folder at the repository root instead of from a separate Git repository.

```text
config-repo/
  application.properties        ← shared by every application
  api-gateway.yml               ← only for spring.application.name=api-gateway
  service-polices.properties    ← only for spring.application.name=service-polices
```

At startup, each connected application calls `http://localhost:8888/{spring.application.name}/default` and receives:

1. its own file (`service-polices.properties`, `api-gateway.yml`, ...), then
2. the shared `application.properties`.

A value in the service's own file wins over the shared file. Values served by the config server win over the service's local `src/main/resources/application.properties`.

You can view what a service receives in a browser, for example `http://localhost:8888/service-polices/default`.

## Connected applications

| Application | Connected | Central file |
| --- | --- | --- |
| `api-gateway` | Yes | `config-repo/api-gateway.yml` (routes, actuator exposure) |
| `service-polices` | Yes | `config-repo/service-polices.properties` (H2 datasource, JPA) |
| `service-sinistres` | Not yet: owner to connect | — |
| `service-indemnites` | Not yet: owner to connect | — |

Each connected application keeps only `spring.application.name`, `server.port`, and the config import locally.

## Connect your service (for service owners)

Merge the latest `main` into your feature branch, then make these two changes in your service.

1. In your `pom.xml`, add the config client dependency:

   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-config</artifactId>
   </dependency>
   ```

   Your `pom.xml` also needs the `spring-cloud-dependencies` BOM in `<dependencyManagement>` (with `<spring-cloud.version>2025.1.4</spring-cloud.version>`). `service-sinistres` already has it.

2. In `src/main/resources/application.properties`, add:

   ```properties
   spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
   ```

That is enough to connect. The `optional:` prefix means your service still starts with its local settings when the config server is not running, so this change cannot break your service or your tests.

Then, when you are ready, **you** decide which settings to move into a new `config-repo/<your-service-name>.properties` file (datasource, Kafka, Feign, business settings, ...). Keep `spring.application.name`, `server.port`, and the import line in your local file. Do not keep the same property in both places.

## Startup order

The config server must be `UP` before the other applications start. If it is not, `optional:` makes them silently fall back to their local settings; for the gateway, that means it starts without routes. `scripts/start-all.ps1` starts the config server first and waits for it automatically.

## Overrides

| Variable | Used by | Default |
| --- | --- | --- |
| `CONFIG_SERVER_URL` | every connected application | `http://localhost:8888` |
| `CONFIG_REPO_LOCATION` | `config-server` | `file:../config-repo/` (relative to the `config-server` directory) |

In Docker Compose these will become, for example, `CONFIG_SERVER_URL=http://config-server:8888` and a mounted `CONFIG_REPO_LOCATION`.

## Relationship with Eureka

The config server keeps a fixed address and does not register with Eureka. When Eureka is added, its URL (`eureka.client.serviceUrl.defaultZone`) can be set once in the shared `config-repo/application.properties` for all services.
