# Sinistre-Pro — Project brief

## Goal

Build an insurance-claim processing system. An insured person declares a claim; the system verifies coverage on the policy, an expert assesses the damage, then compensation is calculated after deducting the excess and paid.

The unit of work is a **claim file** (`Dossier de sinistre`), whose lifecycle is:

```text
DECLARE → EN_EXPERTISE → INDEMNISE
                         ↘ REFUSE
```

## Services and responsibilities

| Service | Responsibility | Entities |
| --- | --- | --- |
| `service-polices` | Insurance policies and their guarantees | `Policy`, `Guarantee` |
| `service-sinistres` | Claim files and expert assessments; the main service | `ClaimFile`, `Expertise` |
| `service-indemnites` | Compensation calculation and payment | `Indemnity` |

### `service-polices`

**Policy:** `id`, `souscripteurEmail`, `typeCouverture`, `montantCouvert`, `prime`, `dateEffet`, `statut`.

**Guarantee:** `id`, `policyId`, `libelle`, `plafond`, `franchise`.

### `service-sinistres`

**ClaimFile:** `id`, `policyId`, `souscripteurEmail`, `dateSinistre`, `type`, `montantDeclare`, `montantRetenu`, `statut`.

**Expertise:** `id`, `claimId`, `expertNom`, `date`, `montantEstime`, `avis`.

### `service-indemnites`

**Indemnity:** `id`, `claimId`, `montantCalcule`, `franchiseAppliquee`, `dateVirement`, `statut`.

## Mandatory business rule

One claim file represents exactly **one policy, one insured person, one incident date, and one status**.

## Service communication

### Synchronous calls — OpenFeign

| Caller | Callee | Why |
| --- | --- | --- |
| `service-sinistres` | `service-polices` | Before opening a claim, verify that the policy is `ACTIVE` and the damage is covered by a `Guarantee`. |
| `service-indemnites` | `service-sinistres` | Retrieve the retained amount from a completed expertise before calculating compensation after excess. |

### Asynchronous events — Kafka

| Topic | Producer | Consumers | Trigger / effect |
| --- | --- | --- | --- |
| `claim-declared` | `service-sinistres` | `service-polices` | A claim changes to `DECLARE`; log it on the policy and notify the policyholder. |
| `expertise-completed` | `service-sinistres` | `service-indemnites` | An expertise is closed with a retained amount; prepare the compensation calculation. |
| `indemnity-paid` | `service-indemnites` | `service-sinistres`, `service-polices` | Payment is made; mark the claim `INDEMNISE` and reflect the claim in the policy premium. |

## Optional bonus

Use Spring AI to generate an automatic summary of the claim description and expert opinion for the expert, with a Resilience4j fallback.
