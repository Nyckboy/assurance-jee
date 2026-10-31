# Run and test locally

## Start every application

From the repository root, run:

```powershell
.\scripts\start-all.ps1 -WaitForHealth
```

The script starts hidden launcher processes for the gateway and all three services. It stores their process IDs under `.run/` and writes output logs to `.run/logs/`; both are ignored by Git.

| Application | Port | Health URL |
| --- | --- | --- |
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
