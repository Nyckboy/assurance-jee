# Discovery server (Eureka)

`discovery-server` is a Spring Cloud Netflix Eureka server. Every Sinistre-Pro application registers with it under its `spring.application.name`, so the gateway and the OpenFeign clients can call a service by name instead of by a fixed `host:port`. It runs on port `8761` and has no database or business logic.

- Dashboard: `http://localhost:8761` lists every registered instance and its status.
- Registry as JSON: `http://localhost:8761/eureka/apps` (send `Accept: application/json`).

```text
service-polices    ─registers─┐
service-sinistres  ─registers─┼── discovery-server :8761
service-indemnites ─registers─┤
api-gateway        ─registers─┘
        │
        └── routes /api/** to lb://<service-name>, load-balanced across the registered instances
```

## Configuration

All Eureka settings are served by the [config server](CONFIG_SERVER.md):

| File | Content |
| --- | --- |
| `config-repo/application.properties` | Shared client settings: the registry URL, register by IP address, and instance id `<spring.application.name>:<server.port>`. |
| `config-repo/discovery-server.properties` | The server itself: no self-registration, and quick eviction of stopped instances for local development. |

The instance id makes several instances of one service easy to tell apart on the dashboard, for example `service-polices:8081` and `service-polices:8091`.

## Registered applications

| Application | Registered | Gateway route |
| --- | --- | --- |
| `api-gateway` | Yes | — |
| `service-polices` | Yes | `/api/policies/**` → `lb://service-polices` |
| `service-sinistres` | Yes | `/api/claims/**` → `lb://service-sinistres` |
| `service-indemnites` | Yes | `/api/indemnities/**` → `lb://service-indemnites` |

Until a service registers, the gateway cannot find any instance of it and that route fails.

## Register your service (for service owners)

Merge the latest `main` into your feature branch first. Your service must already be [connected to the config server](CONFIG_SERVER.md#connect-your-service-for-service-owners), because that is where it receives the Eureka address.

1. In your `pom.xml`, add the Eureka client dependency:

   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
   </dependency>
   ```

2. In `src/test/resources/config/application.properties`, add:

   ```properties
   eureka.client.enabled=false
   ```

   Tests then never try to reach a running registry.

No annotation and no other property are needed: the client registers automatically, under your `spring.application.name`.

3. Check: run `.\scripts\start-all.ps1 -WaitForHealth`, then open `http://localhost:8761`. Your service should be listed as `UP`.

### OpenFeign clients

Once both sides are registered, a Feign client can drop its fixed `url` and use the target's service name only:

```java
@FeignClient(name = "service-polices")
```

Feign then resolves the instance through Eureka and load-balances across instances. Keep the `url` until the target service is registered.

## Startup order

`scripts/start-all.ps1` starts the config server, waits for it, then the discovery server, waits for it, then everything else. A service started before the registry still registers once the registry is up, because Eureka clients retry, but it can take up to about 30 seconds to appear.

## Overrides

| Variable | Used by | Default |
| --- | --- | --- |
| `EUREKA_URL` | every registered application | `http://localhost:8761/eureka/` |

In Docker Compose this will become, for example, `EUREKA_URL=http://discovery-server:8761/eureka/`.
