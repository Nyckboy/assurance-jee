
# service-sinistres

Microservice de gestion des sinistres du projet **Sinistre-Pro**.

Le service permet de déclarer et consulter des sinistres, d'enregistrer des expertises et de suivre leur indemnisation.

Il communique avec `service-polices` via Spring Cloud OpenFeign et Eureka, et échange des événements avec les autres microservices via Apache Kafka.

**URL locale :** `http://localhost:8082`

## 1. Technologies utilisées

- Java 21
- Spring Boot 4
- Spring Data JPA
- Spring Cloud OpenFeign
- Netflix Eureka Client
- Spring Cloud Config Client
- Spring Kafka
- Apache Kafka
- H2 Database
- Maven
- JUnit 5
- Embedded Kafka

## 2. Fonctionnalités

### Gestion des sinistres

- Déclarer un nouveau sinistre.
- Consulter un sinistre par son identifiant.
- Consulter la liste des sinistres.
- Vérifier la couverture d'une police via `service-polices`.
- Refuser les déclarations non couvertes.
- Publier l'événement Kafka `claim-declared`.

### Gestion des expertises

- Enregistrer une expertise pour un sinistre existant.
- Consulter les expertises associées à un sinistre.
- Vérifier que le sinistre possède le statut `DECLARE`.
- Passer le statut du sinistre à `EN_EXPERTISE`.
- Enregistrer le montant retenu après expertise.
- Publier l'événement Kafka `expertise-completed`.

### Gestion des indemnisations

- Consommer les événements Kafka `indemnity-paid`.
- Vérifier l'existence du sinistre.
- Vérifier la correspondance du `policyId`.
- Passer le statut du sinistre à `INDEMNISE`.
- Ignorer les événements dupliqués pour un sinistre déjà indemnisé.
- Conserver le montant retenu lors de l'expertise.

## 3. Statuts des sinistres

Les statuts disponibles sont :

| Statut | Description |
|--------|-------------|
| `DECLARE` | Sinistre déclaré |
| `EN_EXPERTISE` | Expertise enregistrée |
| `INDEMNISE` | Indemnisation effectuée |
| `REFUSE` | Sinistre refusé |

Le traitement de l'expertise permet de passer de `DECLARE` à `EN_EXPERTISE`.

La réception d'un événement `indemnity-paid` valide permet de passer de `EN_EXPERTISE` à `INDEMNISE`.

## 4. API REST

URL de base :

`http://localhost:8082`

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/claims` | Liste des sinistres |
| GET | `/api/claims/{id}` | Détails d'un sinistre |
| POST | `/api/claims` | Déclaration d'un sinistre |
| POST | `/api/claims/{claimId}/expertises` | Création d'une expertise |
| GET | `/api/claims/{claimId}/expertises` | Liste des expertises d'un sinistre |

### 4.1. Déclarer un sinistre

**Requête :**

```http
POST /api/claims
Content-Type: application/json
```

**Corps JSON :**

```json
{
  "policyId": 1,
  "souscripteurEmail": "test@example.com",
  "dateSinistre": "2026-11-01",
  "type": "COLLISION",
  "montantDeclare": 15000
}
```

Le service contacte `service-polices` pour vérifier que la garantie couvre le sinistre.

Si la couverture est valide, le sinistre est enregistré avec le statut `DECLARE`.

**Exemple de réponse :**

```json
{
  "id": 1,
  "policyId": 1,
  "souscripteurEmail": "test@example.com",
  "dateSinistre": "2026-11-01",
  "type": "COLLISION",
  "montantDeclare": 15000,
  "montantRetenu": null,
  "statut": "DECLARE"
}
```

### 4.2. Consulter un sinistre

```http
GET /api/claims/1
```

### 4.3. Consulter tous les sinistres

```http
GET /api/claims
```

### 4.4. Créer une expertise

**Requête :**

```http
POST /api/claims/1/expertises
Content-Type: application/json
```

**Corps JSON :**

```json
{
  "expertNom": "Expert Test",
  "date": "2026-11-22",
  "montantEstime": 12000,
  "avis": "FAVORABLE"
}
```

Lors de la création :

1. Le service vérifie l'existence du sinistre.
2. Le sinistre doit être au statut `DECLARE`.
3. L'expertise est enregistrée.
4. Le statut du sinistre devient `EN_EXPERTISE`.
5. Le champ `montantRetenu` reçoit le montant estimé.
6. L'événement Kafka `expertise-completed` est publié après validation de la transaction.

**Exemple de réponse :**

```json
{
  "id": 1,
  "claimId": 1,
  "expertNom": "Expert Test",
  "date": "2026-11-22",
  "montantEstime": 12000,
  "avis": "FAVORABLE"
}
```

### 4.5. Consulter les expertises

```http
GET /api/claims/1/expertises
```

## 5. Intégration avec service-polices

Le microservice utilise Spring Cloud OpenFeign pour communiquer avec `service-polices`.

Le service distant est découvert grâce à Eureka, sous le nom `service-polices`.

### Vérification de couverture

Endpoint appelé sur le microservice des polices :

```http
GET /api/policies/{policyId}/coverage?claimType={type}
```

**Exemple de réponse :**

```json
{
  "policyId": 1,
  "claimType": "COLLISION",
  "covered": true,
  "reason": "Covered",
  "plafond": 30000.00,
  "franchise": 1000.00
}
```

Un sinistre ne peut être déclaré que si `covered` vaut `true`.

Le microservice ne consulte jamais directement la base de données de `service-polices`.

## 6. Architecture événementielle Kafka

Le service utilise Apache Kafka pour communiquer avec les autres microservices.

| Topic | Rôle de service-sinistres | Description |
|-------|--------------------------|-------------|
| `claim-declared` | Producteur | Déclaration d'un sinistre |
| `expertise-completed` | Producteur | Expertise terminée |
| `indemnity-paid` | Consommateur | Indemnisation effectuée |

### 6.1. Événement claim-declared

**Topic :** `claim-declared`

**Producteur :** `service-sinistres`

**Clé Kafka :** `claimId` converti en chaîne de caractères.

**Déclenchement :** après validation de la transaction enregistrant un sinistre couvert.

**Contrat JSON :**

```json
{
  "claimId": 12,
  "policyId": 3,
  "souscripteurEmail": "assure@example.com",
  "dateSinistre": "2026-11-01",
  "type": "COLLISION",
  "montantDeclare": 4200.00
}
```

L'événement contient exactement six champs.

`dateSinistre` utilise le format ISO-8601 `yyyy-MM-dd`.

`montantDeclare` est un nombre JSON.

Le producteur utilise `JacksonJsonSerializer` et désactive les en-têtes de type JSON.

La publication intervient via `@TransactionalEventListener` avec la phase `AFTER_COMMIT`.

Aucun événement n'est publié si la déclaration est refusée avant validation de la transaction.

### 6.2. Événement expertise-completed

**Topic :** `expertise-completed`

**Producteur :** `service-sinistres`

**Clé Kafka :** `claimId` converti en chaîne de caractères.

**Contrat JSON :**

```json
{
  "claimId": 12
}
```

Cet événement contient uniquement l'identifiant du sinistre.

Il est déclenché après l'enregistrement réussi d'une expertise et la validation de la transaction.

**Classe événement :**

`ma.sinistrepro.claims.event.ExpertiseCompletedEvent`

**Classe producteur :**

`ma.sinistrepro.claims.event.ExpertiseEventProducer`

La publication Kafka se fait après le commit de la transaction grâce à `@TransactionalEventListener(AFTER_COMMIT)`.

### 6.3. Événement indemnity-paid

**Topic :** `indemnity-paid`

**Producteur :** `service-indemnites`

**Consommateur :** `service-sinistres`

**Clé Kafka :** `claimId` converti en chaîne de caractères.

**Contrat JSON :**

```json
{
  "indemnityId": 10,
  "claimId": 12,
  "policyId": 3,
  "montantCalcule": 12000.00,
  "dateVirement": "2026-11-22"
}
```

Le consommateur utilise les classes :

- `IndemnityPaidEvent`
- `IndemnityPaidEventConsumer`

Le consommateur est déclaré avec :

```java
@KafkaListener(
    topics = "indemnity-paid",
    groupId = "${spring.application.name}"
)
```

À la réception de l'événement :

1. Le message JSON est désérialisé.
2. Les champs obligatoires sont validés.
3. Le sinistre correspondant au `claimId` est recherché.
4. Le `policyId` est comparé avec celui du sinistre.
5. Si le sinistre est déjà `INDEMNISE`, aucun changement n'est effectué.
6. Sinon, son statut doit être `EN_EXPERTISE`.
7. Le statut passe à `INDEMNISE` dans une transaction.

Le champ `montantRetenu` est conservé.

Le montant payé reste géré par `service-indemnites`.

### 6.4. Fiabilité des événements

Les producteurs `claim-declared` et `expertise-completed` publient leurs messages après le commit des transactions locales.

Cette approche évite de publier les événements associés à des transactions annulées, mais ne garantit pas une livraison exactement une fois entre la base de données et Kafka.

En cas de panne entre le commit et la publication, un événement peut être perdu.

Une architecture de production nécessiterait notamment :

- Un mécanisme Transactional Outbox pour les producteurs.
- Une stratégie de retry.
- Une Dead Letter Topic pour les messages non traitables.
- Un suivi des erreurs de publication.
- Une gestion durable des événements dupliqués.

Le service `service-indemnites` utilise une table Outbox pour enregistrer ses événements `indemnity-paid`.

Le consommateur de `service-sinistres` gère les répétitions simples d'événements lorsqu'un sinistre possède déjà le statut `INDEMNISE`.

## 7. Configuration

Fichier :

`src/main/resources/application.properties`

### Configuration locale

| Composant | Adresse |
|-----------|---------|
| service-sinistres | `http://localhost:8082` |
| service-polices | `http://localhost:8081` |
| Eureka Server | `http://localhost:8761` |
| Kafka | `localhost:9092` |
| Config Server (optionnel) | `http://localhost:8888` |

### Variables d'environnement

| Variable | Description |
|----------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | Adresse du broker Kafka |
| `EUREKA_SERVER_URL` | Adresse d'enregistrement Eureka |
| `CONFIG_SERVER_URL` | Adresse du Config Server |

`service-polices` est découvert par son nom de service via Eureka.

La configuration actuelle contient :

```properties
spring.application.name=service-sinistres
server.port=8082

spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

spring.kafka.consumer.group-id=service-sinistres
spring.kafka.consumer.auto-offset-reset=earliest

eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8761/eureka/}

spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
spring.cloud.config.enabled=false
```

Le client Config Server est désactivé dans la configuration locale actuelle.

### Base de données

Le service utilise H2 en mémoire :

```properties
spring.datasource.url=jdbc:h2:mem:claimsdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=update
```

Les données ne sont pas conservées après l'arrêt du processus.

La console H2 est configurée sur :

`http://localhost:8082/h2-console`

## 8. Tests automatisés

Les tests sont écrits avec JUnit 5, Spring Boot Test et Embedded Kafka.

Ils permettent de tester les événements sans dépendre d'un broker Kafka Docker externe.

### Tests disponibles

| Classe | Vérifications |
|--------|---------------|
| `ServiceSinistresApplicationTests` | Chargement du contexte Spring |
| `ClaimDeclaredKafkaIntegrationTest` | Publication d'un sinistre couvert et absence de publication en cas de refus |
| `ExpertiseCompletedKafkaIntegrationTest` | Publication de l'expertise, validation du contrat JSON et absence d'événement en cas de rejet |
| `IndemnityPaidKafkaIntegrationTest` | Réception Kafka, passage à `INDEMNISE`, idempotence et vérification du `policyId` |

### Exécuter tous les tests

Depuis le dossier `service-sinistres` :

```bash
bash ./mvnw clean test
```

### Exécuter uniquement les tests Kafka expertise

```bash
bash ./mvnw -Dtest=ExpertiseCompletedKafkaIntegrationTest test
```

### Exécuter uniquement les tests Kafka indemnisation

```bash
bash
./mvnw -Dtest=IndemnityPaidKafkaIntegrationTest test
```

### Résultats

Les tests ont été exécutés localement le 22 novembre 2026.

**Résultat : BUILD SUCCESS**

Les tests d'intégration utilisent un broker Kafka embarqué et ne nécessitent pas de démarrer `service-polices`.

## 9. Démarrage local

### Prérequis

- JDK 21 ou compatible
- Maven Wrapper
- Docker pour le broker Kafka local
- Eureka Server
- service-polices

### Démarrer Kafka

Si le conteneur `sinistre-kafka` existe déjà :

```bash
docker start sinistre-kafka
```

### Démarrer Eureka

Depuis la racine du dépôt :

```bash
cd discovery-server
bash ./mvnw spring-boot:run
```

### Démarrer service-polices

Dans un terminal séparé :

```bash
cd service-polices
bash ./mvnw spring-boot:run
```

### Démarrer service-sinistres

Dans un troisième terminal :

```bash
cd service-sinistres
bash ./mvnw spring-boot:run
```

Chaque application doit rester en cours d'exécution dans son terminal.

### Vérifier les services

Ouvrir le tableau de bord Eureka :

`http://localhost:8761`

Vérifier la présence de :

- `DISCOVERY-SERVER`
- `SERVICE-POLICES`
- `SERVICE-SINISTRES`

## 10. Vérification Kafka manuelle

Pour consulter les messages du topic `expertise-completed` :

```bash
docker exec sinistre-kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic expertise-completed \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" : "
```

Exemple de message reçu :

```text
1 : {"claimId":1}
```

Ce résultat a été validé lors des tests manuels du 22 novembre 2026.

## 11. Limites actuelles

- La base H2 est utilisée uniquement pour le développement et les tests.
- Les données sont perdues lors du redémarrage du service.
- La publication Kafka après commit ne fournit pas de garantie de livraison durable.
- Les mécanismes de retry et de Dead Letter Topic doivent être définis pour la production.
- Les tests d'intégration complets entre tous les microservices restent à valider par l'équipe.

## 12. Dépôt GitHub

**Dépôt :**

https://github.com/Nyckboy/assurance-jee

**Branche de développement :**

`feature/claims-management`

Les modifications de `service-sinistres` sont développées et testées sur cette branche avant leur intégration dans `main`.
