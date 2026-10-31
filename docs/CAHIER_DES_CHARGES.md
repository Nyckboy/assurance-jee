# Cahier des charges — Sinistre-Pro

## 1. Description

Sinistre-Pro est une application de traitement de sinistres d'assurance. Un assuré déclare un sinistre, la garantie est vérifiée sur sa police, une expertise est réalisée, puis l'indemnité est calculée après déduction de la franchise et versée.

L'unité de traitement est le **dossier de sinistre**.

```text
DECLARE → EN_EXPERTISE → INDEMNISE
                         ↘ REFUSE
```

## 2. Services et entités

### 2.1 `service-polices`

Responsabilité : gérer les contrats d'assurance et leurs garanties.

| Entité | Responsabilité unique | Attributs |
| --- | --- | --- |
| `Policy` | Un contrat d'assurance | `id`, `souscripteurEmail`, `typeCouverture`, `montantCouvert`, `prime`, `dateEffet`, `statut` |
| `Guarantee` | Une garantie du contrat | `id`, `policyId`, `libelle`, `plafond`, `franchise` |

### 2.2 `service-sinistres` — service principal

Responsabilité : gérer les dossiers de sinistre et les expertises.

| Entité | Responsabilité unique | Attributs |
| --- | --- | --- |
| `ClaimFile` | Un dossier de sinistre | `id`, `policyId`, `souscripteurEmail`, `dateSinistre`, `type`, `montantDeclare`, `montantRetenu`, `statut` |
| `Expertise` | Une expertise du dommage | `id`, `claimId`, `expertNom`, `date`, `montantEstime`, `avis` |

### 2.3 `service-indemnites`

Responsabilité : calculer et verser les indemnités.

| Entité | Responsabilité unique | Attributs |
| --- | --- | --- |
| `Indemnity` | Une indemnité versée | `id`, `claimId`, `montantCalcule`, `franchiseAppliquee`, `dateVirement`, `statut` |

## 3. Règle métier fondamentale

> Un dossier de sinistre correspond à une police, un assuré, une date de sinistre et un statut.

Chaque service est propriétaire de ses données. Aucun service ne doit accéder directement à la base de données d'un autre service.

## 4. Communication synchrone — OpenFeign

| Service appelant | Service appelé | Objectif |
| --- | --- | --- |
| `service-sinistres` | `service-polices` | Vérifier que la `Policy` est `ACTIVE` et que le dommage entre dans une `Guarantee` avant d'ouvrir le dossier. |
| `service-indemnites` | `service-sinistres` | Récupérer le `montantRetenu` de l'expertise clôturée avant de calculer l'indemnité après déduction de la franchise. |

## 5. Communication asynchrone — Kafka

| Topic | Producteur | Consommateur(s) | Déclencheur et effet |
| --- | --- | --- | --- |
| `claim-declared` | `service-sinistres` | `service-polices` | Émis quand le dossier passe à `DECLARE`. Le service des polices journalise le sinistre sur la police et notifie le souscripteur. |
| `expertise-completed` | `service-sinistres` | `service-indemnites` | Émis quand l'expertise est clôturée avec le montant retenu. Le service des indemnités prépare le calcul. |
| `indemnity-paid` | `service-indemnites` | `service-sinistres`, `service-polices` | Émis quand le virement est effectué. Les sinistres passent à `INDEMNISE` et la police répercute le sinistre sur la prime du contrat. |

## 6. Flux fonctionnel cible

```text
1. L'assuré déclare un sinistre.
2. service-sinistres vérifie la police et la garantie auprès de service-polices.
3. Le dossier est créé avec le statut DECLARE, puis l'événement claim-declared est publié.
4. Une expertise est réalisée et le montant retenu est enregistré.
5. service-sinistres publie expertise-completed.
6. service-indemnites récupère le montant retenu, déduit la franchise et calcule l'indemnité.
7. Après le virement, service-indemnites publie indemnity-paid.
8. service-sinistres passe le dossier à INDEMNISE et service-polices applique la règle d'impact sur la prime.
```

## 7. Bonus optionnel

Utiliser Spring AI pour produire, pour l'expert, une synthèse automatique du dossier à partir de la description du sinistre et de l'avis d'expertise. Prévoir un fallback Resilience4j si le service IA est indisponible.

## 8. Références du dépôt

- [Organisation de l'équipe et règles de travail](TEAM_WORKFLOW.md)
- [Architecture et routes de l'API gateway](API_GATEWAY.md)
- [Exécution et tests locaux](LOCAL_DEVELOPMENT.md)
