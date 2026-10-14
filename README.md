# mock-payment-api

[![CI](https://github.com/tazibt/mock-payment-api/actions/workflows/ci.yml/badge.svg)](https://github.com/tazibt/mock-payment-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4-green)
![License MIT](https://img.shields.io/badge/license-MIT-lightgrey)

Mock d'API de paiement en **Spring Boot**, conçu pour tester les parcours de commande e-commerce
(type SAP Commerce) **sans dépendre d'un vrai prestataire de paiement**.

Il permet de simuler à la demande un paiement accepté, refusé, en erreur ou en timeout :
utile en recette fonctionnelle, en tests d'intégration, en automatisation (CI/CD)
et en **tests de performance**, où l'on veut isoler l'application testée d'un service tiers.

## Pourquoi un mock de paiement ?

| Problème avec le vrai prestataire | Ce que le mock apporte |
|---|---|
| Environnement de test du PSP instable ou indisponible | Un service toujours disponible, en local ou en Docker |
| Impossible de provoquer un refus ou un timeout à la demande | Cas d'erreur pilotés par les données ou la configuration |
| Quotas / coûts lors des tirs de charge | Aucune limite, temps de réponse maîtrisé |
| Résultats de performance faussés par un tiers | Latence configurable et connue |

## Règles de simulation

| Requête | Réponse |
|---|---|
| Paiement standard | `200` – `ACCEPTED` + `transactionId` |
| `cardType = "EXPIRED"` | `200` – `DECLINED` + `errorCode = CARD_EXPIRED` |
| `amount = 999.99` | `504` – `TIMEOUT` + `errorCode = PSP_TIMEOUT` (après `mock.delay.timeout` ms) |
| `mock.force.status` ≠ `NONE` | Statut forcé pour **toutes** les requêtes (`DECLINED`, `ERROR` → `500`, `TIMEOUT` → `504`) |
| Requête invalide (montant absent ou non numérique) | `400` |

> Un refus de paiement renvoie `200` : c'est une réponse **métier** valide du prestataire,
> pas une erreur technique. Les codes `5xx` sont réservés aux pannes (`ERROR`, `TIMEOUT`).

## Démarrage

Prérequis : Java 21 (Maven est fourni via le wrapper `mvnw`).

```bash
./mvnw spring-boot:run          # Windows : mvnw.cmd spring-boot:run
```

Ou avec Docker :

```bash
docker build -t mock-payment-api .
docker run -p 8085:8085 mock-payment-api
```

Le service écoute sur `http://localhost:8085`.

## Exemples d'appels

```bash
# Paiement accepté
curl -X POST http://localhost:8085/payment/mock \
  -H "Content-Type: application/json" \
  -d '{"amount": 49.90, "cardType": "VISA", "customerId": "C-001"}'
# {"status":"ACCEPTED","message":"Paiement autorisé","errorCode":null,"transactionId":"TX-e25818de-49a0-4bc5-9134-82750a53c8ad"}

# Carte expirée
curl -X POST http://localhost:8085/payment/mock \
  -H "Content-Type: application/json" \
  -d '{"amount": 80.00, "cardType": "EXPIRED", "customerId": "C-200"}'
# {"status":"DECLINED","message":"Carte expirée","errorCode":"CARD_EXPIRED","transactionId":null}

# État du service (healthcheck)
curl http://localhost:8085/payment/health
# UP
```

## Configuration

Les valeurs par défaut sont dans `src/main/resources/application.properties`.
Elles peuvent être surchargées **sans recompiler** via `config/mock-config.properties`
(ou en ligne de commande : `--mock.delay.success=200`).

| Propriété | Défaut | Rôle |
|---|---|---|
| `mock.port` | `8085` | Port HTTP |
| `mock.delay.success` | `0` | Latence simulée (ms) d'un paiement accepté |
| `mock.delay.error` | `2000` | Latence (ms) d'un refus ou d'une erreur |
| `mock.delay.timeout` | `10000` | Durée (ms) avant la réponse `504` |
| `mock.force.status` | `NONE` | Force la réponse : `NONE`, `DECLINED`, `ERROR`, `TIMEOUT` |
| `mock.force.errorCode` | `CARD_EXPIRED` | Code d'erreur renvoyé en mode forcé |

> En test de performance, `mock.delay.success` permet de reproduire la latence réelle du
> prestataire (ex. 300 ms) pour obtenir des temps de réponse de bout en bout réalistes.
>
> Les **threads virtuels** Java 21 sont activés (`spring.threads.virtual.enabled=true`) :
> les délais simulés ne bloquent pas les threads Tomcat, donc le mock ne devient pas
> lui-même le goulot d'étranglement d'un tir de charge (même en mode `TIMEOUT`).

## Tests d'acceptation (ATDD)

Les règles métier ont été écrites **avant le code** sous forme de scénarios Gherkin,
puis automatisées avec **Cucumber + RestAssured** :

```
src/test/resources/features/
├── smoke.feature        # @smoke : vérifications post-déploiement
├── paiement.feature     # règles d'acceptation : accepté, refusé, timeout
└── robustesse.feature   # statuts forcés (ERROR, DECLINED, TIMEOUT) et requête invalide
```

Extrait :

```gherkin
Scénario: Une carte expirée est refusée
  Étant donné une demande de paiement de 80,00 € par carte "EXPIRED" pour le client "C-200"
  Quand j'envoie la demande de paiement
  Alors le code HTTP est 200
  Et le statut du paiement est "DECLINED"
  Et le code d'erreur est "CARD_EXPIRED"
```

```bash
./mvnw test                                                         # tous les scénarios
./mvnw test -Dtest=RunCucumberTest "-Dcucumber.filter.tags=@smoke"  # smoke tests seuls
```

Le rapport HTML est généré dans `target/cucumber-report.html`.

### Intégration continue

À chaque push, GitHub Actions exécute d'abord les **smoke tests**, puis, seulement
s'ils passent, **l'ensemble des scénarios** ; le rapport Cucumber est publié en artefact.
C'est le principe du smoke test : un échec bloque la campagne complète.

> Les scénarios étant rédigés en français (`# language: fr`), les montants s'écrivent
> avec une virgule (`49,90 €`) : Cucumber applique le format numérique de la langue choisie.

## Utilisation dans une campagne de test

- **Recette / E2E** : configurer l'application testée pour appeler le mock à la place du PSP.
- **Tests de charge (JMeter, Gatling)** : isoler l'application du prestataire et
  maîtriser sa latence via `mock.delay.success`.
- **Tests de robustesse** : passer `mock.force.status=TIMEOUT` ou `ERROR` pour vérifier
  le comportement de l'application (message utilisateur, relance, commande non créée).

## Stack

Java 21 · Spring Boot 4 · Maven · Cucumber · RestAssured · JUnit · Docker · GitHub Actions

## Licence

MIT – voir [LICENSE](LICENSE).
