# Run and test locally

## Start every application

From the repository root, run:

```powershell
.\scripts\start-all.ps1 -WaitForHealth
```

The script starts hidden launcher processes for the config server, the discovery server, the gateway, and all three services. It starts the config server first and waits until it is `UP`, because the other applications load their configuration from it (see [the config server documentation](CONFIG_SERVER.md)). It then starts the discovery server and waits for it, because the other applications register with it (see [the discovery server documentation](DISCOVERY_SERVER.md)). It stores their process IDs under `.run/` and writes output logs to `.run/logs/`; both are ignored by Git.

Registered services are listed on the Eureka dashboard at `http://localhost:8761`. A service can take up to about 30 seconds to appear there after it reports `UP`, and the gateway can only route to it once it appears.

| Application | Port | Health URL |
| --- | --- | --- |
| Config server | `8888` | `http://localhost:8888/actuator/health` |
| Discovery server (Eureka) | `8761` | `http://localhost:8761/actuator/health` |
| API gateway | `8080` | `http://localhost:8080/actuator/health` |
| Policy service | `8081` | `http://localhost:8081/actuator/health` |
| Claims service | `8082` | `http://localhost:8082/actuator/health` |
| Indemnity service | `8083` | `http://localhost:8083/actuator/health` |

Stop only the processes launched by the script:

```powershell
.\scripts\stop-all.ps1
```

If PowerShell blocks local scripts, launch it without changing your machine-wide execution policy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1 -WaitForHealth
```

## Test the policy API through the gateway

After all applications report `UP`, run:

```powershell
.\scripts\test-policies.ps1
```

The script creates an active AUTO policy, attaches a COLLISION guarantee, and verifies coverage **through the gateway** at port `8080`. A successful run prints the policy ID, ceiling, and franchise.

For manual testing, use the policy API documentation in [`service-polices/README.md`](../service-polices/README.md). The policy service's temporary H2 console is available at `http://localhost:8081/h2-console` with JDBC URL `jdbc:h2:mem:policiesdb`, user `sa`, and an empty password.

## See the emails (Mailpit)

`service-polices` emails policyholders (claim recorded, indemnity paid) through SMTP on `localhost:1025`. [Mailpit](https://mailpit.axllent.org/) is a local mail server: it catches every email and shows it in a browser inbox, so nothing reaches a real address.

1. Download the Windows build from the [Mailpit releases](https://github.com/axllent/mailpit/releases) (`mailpit-windows-amd64.zip`), and unzip it outside the repository.
2. Run `mailpit.exe`. It listens for SMTP on port `1025` and serves the inbox at `http://localhost:8025`.
3. Start the applications. Each email the policy service sends appears in the inbox.

Without Mailpit, the policy service still works: each failed email is logged as a warning. Another SMTP server can be used with `MAIL_HOST` and `MAIL_PORT`.
