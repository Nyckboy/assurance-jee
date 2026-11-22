# Architecture

Sinistre-Pro is made of three business microservices, each with its own database, behind an API gateway. They are configured by a config server, find each other through a Eureka registry, call each other with OpenFeign, and exchange events through Kafka.

The diagrams below are written in [Mermaid](https://mermaid.js.org/): GitHub renders them directly, and they live next to the code they describe.

## 1. Overall architecture

```mermaid
flowchart TB
    client(["Client<br/>Postman, frontend"])
    gateway["api-gateway :8080<br/>routes /api/** to lb://service-name"]

    subgraph platform["Platform services"]
        direction LR
        config["config-server :8888"] --- repo[("config-repo/")]
        eureka["discovery-server<br/>Eureka :8761"]
    end

    subgraph business["Business microservices"]
        direction LR
        sinistres["service-sinistres :8082"]
        polices["service-polices :8081"]
        indemnites["service-indemnites :8083"]
    end

    subgraph storage["One database per service (H2)"]
        direction LR
        dbs[("claimsdb")]
        dbp[("policiesdb")]
        dbi[("indemnitiesdb")]
    end

    kafka{{"Kafka :9092"}}
    mail["Mailpit / SMTP :1025"]

    client --> gateway
    gateway --> business
    gateway -.->|"configuration, registration"| platform
    business -.->|"configuration, registration"| platform

    sinistres -->|"Feign: coverage"| polices
    indemnites -->|"Feign: montantRetenu"| sinistres
    indemnites -->|"Feign: plafond, franchise"| polices

    sinistres -->|"claim-declared, expertise-completed"| kafka
    indemnites -->|"indemnity-paid"| kafka
    kafka -->|"claim-declared, indemnity-paid"| polices
    kafka -->|"expertise-completed"| indemnites
    kafka -->|"indemnity-paid"| sinistres

    sinistres --- dbs
    polices --- dbp
    indemnites --- dbi
    polices -->|"policyholder emails"| mail
```

| Component | Port | Role |
| --- | --- | --- |
| `api-gateway` | 8080 | Single entry point. Routes `/api/policies/**`, `/api/claims/**` and `/api/indemnities/**` to the services by their Eureka name (`lb://`), load-balanced. |
| `config-server` | 8888 | Serves each application's configuration from [`config-repo/`](../config-repo). |
| `discovery-server` | 8761 | Eureka registry: every application registers as `<name>:<port>`. |
| `service-polices` | 8081 | Policies, guarantees, coverage verification, premium rule, policyholder emails. |
| `service-sinistres` | 8082 | Claim files and expert assessments. |
| `service-indemnites` | 8083 | Indemnity calculation and payment. |
| Kafka | 9092 | Asynchronous events between the services. |
| Mailpit | 1025 / 8025 | Local SMTP server and inbox for the emails. |

## 2. Class diagrams per microservice

Each service owns its tables. **No foreign key ever crosses a service boundary.** Another service's object is referenced only by its id (`policyId`, `claimId`, `indemnityId`). Remote classes appear below as `Remote…` with the `«remote»` stereotype, linked by dashed arrows. [Section 3](#3-how-remote-references-are-managed) explains how each remote reference is checked.

### service-polices

```mermaid
classDiagram
    direction LR
    class Policy {
        Long id
        String souscripteurEmail
        String typeCouverture
        BigDecimal montantCouvert
        BigDecimal prime
        LocalDate dateEffet
        PolicyStatus statut
        increasePrime(BigDecimal rate) BigDecimal
    }
    class Guarantee {
        Long id
        String libelle
        BigDecimal plafond
        BigDecimal franchise
    }
    class PolicyClaim {
        Long id
        Long claimId
        Long policyId
        String souscripteurEmail
        LocalDate dateSinistre
        String type
        BigDecimal montantDeclare
        Instant receivedAt
    }
    class PremiumAdjustment {
        Long id
        Long claimId
        Long indemnityId
        Long policyId
        BigDecimal montantIndemnise
        LocalDate dateVirement
        BigDecimal primeAvant
        BigDecimal primeApres
        Instant appliedAt
    }
    class PolicyStatus {
        <<enumeration>>
        ACTIVE
        SUSPENDED
        EXPIRED
        TERMINATED
    }
    class RemoteClaimFile {
        <<remote>>
        owner service-sinistres
    }
    class RemoteIndemnity {
        <<remote>>
        owner service-indemnites
    }

    Policy "1" *-- "0..*" Guarantee : guarantees (JPA, cascade)
    Policy --> PolicyStatus
    PolicyClaim ..> Policy : policyId (local id)
    PremiumAdjustment ..> Policy : policyId (local id)
    PolicyClaim ..> RemoteClaimFile : claimId (copy from claim-declared)
    PremiumAdjustment ..> RemoteClaimFile : claimId (unique)
    PremiumAdjustment ..> RemoteIndemnity : indemnityId (from indemnity-paid)
```

- `Policy` → `Guarantee` is the only real JPA relation in the project: one-to-many, cascade, orphan removal.
- `PolicyClaim` is the policy's **own copy** of a claim, recorded from the `claim-declared` event. Polices never reads the claims database.
- `PremiumAdjustment` records each +10% premium increase applied from `indemnity-paid`. The unique `claimId` makes a resent event harmless.
- DTOs: `PolicyRequest`/`PolicyResponse`, `GuaranteeRequest`/`GuaranteeResponse`, `CoverageVerificationResponse` (the Feign contract), `PolicyClaimResponse`, `PremiumAdjustmentResponse`. Events: `ClaimDeclaredEvent`, `IndemnityPaidEvent`.

### service-sinistres

```mermaid
classDiagram
    direction LR
    class ClaimFile {
        Long id
        Long policyId
        String souscripteurEmail
        LocalDate dateSinistre
        String type
        BigDecimal montantDeclare
        BigDecimal montantRetenu
        ClaimStatus statut
    }
    class Expertise {
        Long id
        Long claimId
        String expertNom
        LocalDate date
        BigDecimal montantEstime
        String avis
    }
    class ClaimStatus {
        <<enumeration>>
        DECLARE
        EN_EXPERTISE
        INDEMNISE
        REFUSE
    }
    class PolicyClient {
        <<Feign>>
        target service-polices
        verifyCoverage(Long policyId, String claimType) CoverageResponse
    }
    class CoverageResponse {
        <<DTO>>
        Long policyId
        String claimType
        boolean covered
        String reason
        BigDecimal plafond
        BigDecimal franchise
    }
    class RemotePolicy {
        <<remote>>
        owner service-polices
    }

    Expertise ..> ClaimFile : claimId (local id)
    ClaimFile --> ClaimStatus
    ClaimFile ..> RemotePolicy : policyId (checked by Feign before creation)
    PolicyClient ..> CoverageResponse
    PolicyClient ..> RemotePolicy : GET coverage of the policy
```

- Lifecycle: `DECLARE` → (expertise) `EN_EXPERTISE` → (`indemnity-paid`) `INDEMNISE`. `REFUSE` is reserved for refused claims.
- Recording an expertise copies `montantEstime` into `ClaimFile.montantRetenu`, which service-indemnites reads.
- DTOs: `ClaimRequest`/`ClaimResponse` (the Feign contract read by indemnites), `ExpertiseRequest`/`ExpertiseResponse`. Events: `ClaimDeclaredEvent` and `ExpertiseCompletedEvent` (published after commit), `IndemnityPaidEvent` (consumed).

### service-indemnites

```mermaid
classDiagram
    direction LR
    class Indemnity {
        Long id
        Long claimId
        BigDecimal montantCalcule
        BigDecimal franchiseAppliquee
        LocalDate dateVirement
        IndemnityStatus statut
    }
    class OutboxEvent {
        Long id
        String topic
        String eventKey
        String payload
        LocalDateTime createdAt
        LocalDateTime publishedAt
    }
    class IndemnityStatus {
        <<enumeration>>
        EN_ATTENTE
        CALCULEE
        PAYEE
        ANNULEE
    }
    class IndemnityCalculator {
        calculate(montantRetenu, plafond, franchise) BigDecimal
    }
    class ClaimServiceClient {
        <<Feign>>
        target service-sinistres
        getClaimById(Long id) ClaimResponse
    }
    class PolicyServiceClient {
        <<Feign>>
        target service-polices
        verifyCoverage(Long policyId, String claimType) CoverageResponse
    }
    class RemoteClaimFile {
        <<remote>>
        owner service-sinistres
    }
    class RemotePolicy {
        <<remote>>
        owner service-polices
    }

    Indemnity --> IndemnityStatus
    Indemnity ..> RemoteClaimFile : claimId (unique)
    OutboxEvent ..> Indemnity : indemnity-paid payload
    ClaimServiceClient ..> RemoteClaimFile : GET claim by id
    PolicyServiceClient ..> RemotePolicy : GET coverage of the policy
    IndemnityCalculator ..> Indemnity : montantCalcule
```

- Calculation: `montantCalcule = max(0, min(montantRetenu, plafond) − franchise)`. `montantRetenu` comes from sinistres; `plafond` and `franchise` come from polices.
- `OutboxEvent`: `indemnity-paid` is saved in the same transaction as the payment, and a scheduled publisher sends it to Kafka. Nothing is lost if Kafka is down.
- Both Feign clients are protected by Retry, Circuit Breaker and timeouts (see [`config-repo/service-indemnites.properties`](../config-repo/service-indemnites.properties)).
- DTOs: `IndemnityRequest`/`IndemnityResponse`, `ClaimResponse` and `CoverageResponse` (Feign). Events: `ExpertiseCompletedEvent` (consumed), `IndemnityPaidEvent` (published).

## 3. How remote references are managed

| Reference | Owner of the target | Check | When |
| --- | --- | --- | --- |
| `ClaimFile.policyId` | service-polices | Feign `verifyCoverage`. Unknown policy → 400, not covered → 422, polices unreachable → 503. Nothing is saved unless covered. | Before a claim is created |
| `Indemnity.claimId` (calculation) | service-sinistres | Feign `getClaimById` (unknown claim → 404), then Feign `verifyCoverage` on the claim's policy | Before an indemnity is calculated |
| `Indemnity.claimId` (manual `POST /api/indemnities`) | service-sinistres | **Not checked remotely**, only that the claim has no indemnity yet | — |
| `PolicyClaim.claimId` | service-sinistres | Trusted: comes from the `claim-declared` event of the owner. Unknown `policyId` → skipped. | When the event is consumed |
| `PremiumAdjustment.claimId`, `indemnityId` | sinistres, indemnites | Trusted: comes from the `indemnity-paid` event. Unknown policy → skipped; claim already applied → skipped. | When the event is consumed |
| `IndemnityPaidEvent.claimId` in sinistres | service-sinistres (local) | Claim must exist, belong to the same policy and be `EN_EXPERTISE` | When the event is consumed |

Rules shared by every service:

- **Only ids cross service boundaries**, never JPA relations or another service's tables.
- **Synchronous checks** (Feign) happen before a creation that depends on another service, through the target's Eureka name.
- **Asynchronous copies** (Kafka) keep a local copy of what a service needs, for example the claims recorded on a policy. They are published only after the producer's transaction commits.
- **Consumers are idempotent:** a unique key (`claimId`) or a status check makes a resent event harmless. A message that can never apply is logged and skipped.

## 4. Life of a claim (sequence)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GW as api-gateway
    participant S as service-sinistres
    participant P as service-polices
    participant I as service-indemnites
    participant K as Kafka
    participant M as Mailpit

    Client->>GW: POST /api/claims
    GW->>S: lb://service-sinistres
    S->>P: Feign GET /api/policies/{id}/coverage
    P-->>S: covered, plafond, franchise
    S-->>Client: 201 claim DECLARE
    S-)K: claim-declared (after commit)
    K-)P: claim-declared
    P->>P: record PolicyClaim
    P-)M: email "Sinistre n°X enregistré"

    Client->>GW: POST /api/claims/{id}/expertises
    GW->>S: lb://service-sinistres
    S-->>Client: 201, claim EN_EXPERTISE, montantRetenu
    S-)K: expertise-completed (after commit)
    K-)I: expertise-completed
    I->>S: Feign GET /api/claims/{id}
    I->>P: Feign GET /api/policies/{id}/coverage
    I->>I: Indemnity CALCULEE = min(retenu, plafond) - franchise

    Client->>GW: POST /api/indemnities/{id}/pay
    GW->>I: lb://service-indemnites
    I->>S: Feign GET /api/claims/{id} (policyId)
    I->>I: PAYEE + OutboxEvent (same transaction)
    I-->>Client: 200 PAYEE
    I-)K: indemnity-paid (outbox publisher)
    K-)S: indemnity-paid
    S->>S: claim INDEMNISE
    K-)P: indemnity-paid
    P->>P: prime +10%, PremiumAdjustment
    P-)M: email "Indemnité versée", new premium
```

The Postman collection replays this scenario step by step and checks each arrow: folder `04 - Scénario complet` in [`postman/`](../postman).
