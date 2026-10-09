# Microservices Lab: Webhook Delivery System

![Java](https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-Aiven-4479A1?logo=mysql&logoColor=white)
![Redis Streams](https://img.shields.io/badge/Redis_Streams-Upstash-DC382D?logo=redis&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white)
![OpenAPI](https://img.shields.io/badge/OpenAPI-Swagger_UI-85EA2D?logo=swagger&logoColor=black)
![Testcontainers](https://img.shields.io/badge/Tests-Mockito_%2B_Testcontainers-2496ED?logo=docker&logoColor=white)
![Oracle Cloud](https://img.shields.io/badge/Deployed_on-Oracle_Cloud-F80000?logo=oracle&logoColor=white)

> A deliberately small distributed system, built to study what changes when an application stops being a single process and becomes a set of independent services.

> **Resumo (PT-BR):** laboratório de sistemas distribuídos com dois serviços Spring Boot independentes, rodando em duas VMs na Oracle Cloud, que se comunicam por REST e por Redis Streams, cada um com o seu próprio banco. O objetivo é estudar o que muda quando uma aplicação deixa de ser um único processo: timeouts, falhas parciais, consistência eventual, retry e idempotência. É um projeto complementar ao [Kairos](https://github.com/arthsdev/kairos). A documentação completa dos experimentos está em inglês, abaixo.

This is the **main README of the lab**. It documents the whole system, every experiment and the decisions behind them. The second service lives in [delivery-service](https://github.com/arthsdev/delivery-service) and has a shorter README that links back here.

Companion to [**Kairos**](https://github.com/arthsdev/kairos), my full-stack portfolio project.

---

## Why this project exists

Kairos is a robust full-stack platform, but it is essentially one backend process with internal modules. This lab is the opposite on purpose: a tiny domain, so the complexity under study is the *distributed* kind (network calls, independent failures, data owned by different services, messages that arrive late, twice or not at all) and not business rules.

**The domain.** Clients register subscriptions (`eventType` → `targetUrl`), publish events, and the system delivers each event's payload to every matching target. That is all.

**Non-goals.** No frontend, no authentication, no payments, no heavy infrastructure (no Kubernetes, service mesh, Kafka, service discovery or API gateway). The size is the point.

---

## Architecture

```mermaid
flowchart LR
    C[Client] -->|POST /events<br/>POST /events/async| W
    subgraph VM1[OCI VM 1 · 1 vCPU · 1 GB]
        W[webhook-service]
    end
    subgraph VM2[OCI VM 2 · 1 vCPU · 1 GB]
        D[delivery-service]
    end
    W -->|sync: HTTP over private network| D
    W -->|async: XADD| R[(Redis Streams<br/>Upstash)]
    R -->|XREADGROUP + XACK| D
    W --- DBW[(MySQL<br/>webhook)]
    D --- DBD[(MySQL<br/>delivery)]
    D -->|HTTP POST| T[Subscriber URL]
```

| Service | Owns | Main endpoints |
|---|---|---|
| **webhook-service** (VM 1) | `subscriptions`, `events` | `POST /app/v1/subscriptions`, `POST /app/v1/events`, `POST /app/v1/events/async`, `GET /app/v1/events/{publicId}` |
| **delivery-service** (VM 2) | `delivery_attempts` | `POST /app/v1/deliveries`, plus the Redis Stream consumer |

**Deployment.** Each service is a jar running under `systemd` on its own OCI VM, reading its configuration from an `EnvironmentFile`. The VMs talk over OCI's private network. MySQL runs on Aiven (one separate instance per service) and Redis on Upstash, so neither consumes RAM on the 1 GB VMs.

**Observability (basic).** Logs through `journalctl`, one Actuator endpoint, and Swagger UI on both services. Real observability is still to come.

---

## Message contract

The contract between the two services is the **stream key and its fields**. No compiler checks it, so this repository is its source of truth and the delivery-service README repeats it with a pointer here.

| Item | Value |
|---|---|
| Stream key | `webhook.deliveries.requested` |
| Consumer group | `delivery-service` |
| Granularity | one message per *(event, subscription)* |

| Field | Meaning |
|---|---|
| `eventId` | public UUID of the event |
| `subscriptionId` | public UUID of the subscription |
| `targetUrl` | where the payload must be delivered |
| `payload` | event body, as a JSON string |
| `schemaVersion` | currently `1` |

The message is **self-sufficient on purpose**: the delivery-service cannot query the webhook-service's database, so everything it needs travels inside the message.

---

## Experiments

| # | Experiment | Status |
|---|---|---|
| 1 | Synchronous REST between two independent services | Done |
| 2 | Database per service | Done |
| 3 | Asynchronous events with Redis Streams | Done |
| 4 | Service unavailable (delivery-service down) | Done |
| 5 | Broker unavailable (Redis down) | Planned |
| 6 | Retry of failed and pending messages | Planned |
| 7 | Idempotency and duplicate messages | Planned |
| 8 | Eventual consistency, observed explicitly | Planned |
| 9 | Transactional Outbox | Planned |

### Experiment 1: Synchronous REST

**Question:** what actually changes when a local method call becomes a network call?

**Flow.** `POST /app/v1/events` creates the event, looks up the active subscriptions for its `eventType`, and calls the delivery-service over HTTP once per subscription. The delivery-service stores a `DeliveryAttempt`, hands it to an asynchronous executor and performs the HTTP delivery to the target.

**What the code does about failure.** `DeliveryServiceClient` catches `RestClientException` and returns a result instead of throwing. `EventDispatcher` counts accepted calls and maps the outcome to the event status:

| Accepted calls | Event status |
|---|---|
| none to send | `NO_SUBSCRIBERS` |
| all | `DISPATCHED` |
| some | `PARTIALLY_DISPATCHED` |
| none | `DISPATCH_FAILED` |

Both services expose OpenAPI documentation (Swagger UI) and have automated tests: unit tests with Mockito and integration tests with Testcontainers running a real MySQL (24 tests in this service and 13 in the delivery-service at the time of writing).

### Experiment 2: Database per service

Each service owns its own MySQL instance and its own Flyway migrations. They never read each other's tables and share only the HTTP contract and the message contract.

- References across the boundary use **public UUIDs** (`event_id`, `subscription_id`) with no foreign keys. Technical `BIGINT` ids stay internal to each database.
- Data a service needs from the other (`target_url`, `payload`) **travels inside the request or message and is stored as a copy**.
- Because the databases are physically separate, one service's credentials cannot see the other's schema. The rule is enforced by the infrastructure, not by discipline.

**The cost, stated honestly.** There are no joins and no transactions across the boundary. Every consequence below has its own experiment: the event status that never changes, the publish-then-save race, duplicated deliveries and the same data stored in two places.

**A decision that changed.** The initial plan was SQLite per service to save RAM. I moved to managed MySQL because it keeps the database off the 1 GB VMs, survives a VM restart, and lets the tests run against a real MySQL with Testcontainers. The price is network latency to the database: Flyway alone takes a few seconds at boot.

### Experiment 3: Asynchronous events (Redis Streams)

**Question:** what changes when the webhook-service stops calling the delivery-service and publishes an event instead?

**Flow**

```mermaid
sequenceDiagram
    participant C as Client
    participant W as webhook-service
    participant R as Redis Stream
    participant D as delivery-service
    participant T as Target URL
    C->>W: POST /events/async
    W->>R: XADD (one message per subscription)
    W-->>C: 202 Accepted, status QUEUED
    R->>D: XREADGROUP
    D->>D: create DeliveryAttempt
    D->>R: XACK
    D->>T: HTTP POST (async executor)
```

**Decisions**

| Decision | Why |
|---|---|
| Separate endpoint (`/events/async`) instead of a `deliveryMode` flag | The two paths have different guarantees and different responses (`201` vs `202`); a flag would hide that. |
| New `QUEUED` status instead of reusing `DISPATCHED` | `DISPATCHED` means "the delivery-service accepted the request". `QUEUED` means "the broker accepted the message". One status should not mean two things. |
| One message per *(event, subscription)* | The consumer stays simple, retries affect only the failed delivery, and `(eventId, subscriptionId)` is a natural idempotency key. |
| Consumer group + explicit `XACK` | The service resumes where it stopped after a restart, and a message is only confirmed after the work is done. |
| Reuse `DeliveryAttemptService.create()` | The consumer is just another entry point to the same use case as the HTTP controller; `DeliveryProcessor` is untouched. |
| Managed Redis (Upstash, TLS) | Keeps the 1 GB VMs light and gives me a second messaging style next to RabbitMQ in Kairos. |
| Long poll timeout (10 s) | Each blocking read counts as a command on the free tier. |

**What the consumer does with each message**

| Situation | Action | Reason |
|---|---|---|
| `create()` succeeds | `XACK` | the work is done |
| `create()` throws (for example the database is down) | **no** `XACK` | it may succeed later, so the message stays pending |
| Malformed or invalid message | log an error, `XACK`, discard | it can never succeed; leaving it pending forever helps nobody |

**Evidence**

The publisher logs the record id. Notice the order: the message is in the stream *before* the event row is inserted.

```text
DEBUG EventStreamPublisher : Published to stream [key=webhook.deliveries.requested, recordId=1791239181616-0, eventId=37bffd55-..., subscriptionId=a057dc0f-...]
Hibernate: insert into events (created_at,event_type,payload,public_id,status) values (?,?,?,?,?)
```

- `POST /events/async` returns `202` with `"status":"QUEUED"`.
- Locally, about 2 s passed between the `XADD` and the consumer receiving the message (Upstash is in N. Virginia). The 10 s poll timeout adds no latency, because a blocking read returns as soon as an entry arrives.
- In production (VM 1 → Upstash → VM 2) the payload reached the target URL and `XPENDING webhook.deliveries.requested delivery-service` returned no pending messages.
- On the 1 GB VM the delivery-service takes about 62 s to boot (17 s on my machine), and the first `/v3/api-docs` call takes about 3.8 s before the cache warms up.

**What I learned**

- The poll timeout is a **cost** knob, not a latency knob: a blocking read returns as soon as an entry arrives, so the 10 s timeout added no measurable latency and only reduced the number of empty commands.
- Acknowledge **after** the work, and treat the three outcomes differently. "Failed and retryable" and "failed and hopeless" need opposite handling.
- The dual write is not just theory: it is visible in a single log excerpt, in the wrong order.
- Spring wraps Redis errors, so a `BUSYGROUP` on the second boot is in the *cause chain*, not in the top exception's message. Checking only the message would have broken the second startup.
- Beans that need external infrastructure need an off switch for tests (`event-stream.consumer.enabled=false`), and integration tests should get a fake broker URL instead of the real one.
- A status can only describe what this service knows. `QUEUED` stays `QUEUED` after a successful delivery because the real outcome lives in the other service's database.

---


### Experiment 4: Service unavailable

**Question:** what does the webhook-service do when the delivery-service does not answer, and what does the client see?

"Unavailable" turned out to be two different failures, so I tested both on the real VMs:

| Failure | How I simulated it |
|---|---|
| **Process stopped** | `systemctl stop delivery-service` on VM 2 (the port refuses connections) |
| **Network drops packets** | removed the ingress rule that lets VM 1 reach port 8081 in the OCI security list (nobody answers) |

**Results**

| Scenario | Path | Result | Time |
|---|---|---|---|
| Delivery-service stopped | sync (`POST /events`) | `201`, `DISPATCH_FAILED` | ~1.1 s warm (6.8 s on the first, cold call) |
| Delivery-service healthy | sync | `201`, `DISPATCHED` | ~1.9 s warm (8.4 s right after a restart) |
| Packets dropped, **no timeout** | sync | no answer within 120 s (my client gave up); the server logged the failure only after more than 2 minutes | more than 2 min |
| Packets dropped, **2 s connect timeout** | sync | `201`, `DISPATCH_FAILED` | 3.1 s |
| Delivery-service stopped | async (`POST /events/async`) | `202`, `QUEUED` | ~1.4 s |

- **Async keeps accepting while the other service is down.** Two events published while the delivery-service was stopped were accepted in ~1.4 s and delivered about 1 s after the consumer came back, roughly 25 minutes later. Nothing was lost and nothing had to be done by hand.
- **The sync path has no retry.** An event that ended as `DISPATCH_FAILED` stayed that way after the delivery-service came back.
- **The HTTP status does not tell the story.** The client gets `201` even when the delivery failed; the outcome is only in the `status` field of the body.
  **What I changed because of it**

1. **Timeouts.** `DeliveryServiceClient` was built with no timeouts, so a dropped network held the HTTP thread and, because `createEvent` is `@Transactional`, a database connection for minutes. It now has a 2 s connect timeout and a 10 s read timeout (`DELIVERY_SERVICE_CONNECT_TIMEOUT` and `DELIVERY_SERVICE_READ_TIMEOUT`). The read timeout is 10 s and not 5 s because the first call after a restart took 8.4 s; 5 s would have rejected calls that were about to succeed.
2. **Readable failure log.** The warning printed `ex.getMessage()`, which was `null` for both failures. It now includes the event id, the subscription id and the root cause, so the two failures are distinguishable:
```text
Failed to dispatch to delivery-service [eventId=..., subscriptionId=...]: ConnectException (Connection refused: getsockopt)
Failed to dispatch to delivery-service [eventId=..., subscriptionId=...]: ConnectException (HTTP connect timed out)
```

**Side findings on the 1 GB VMs**

- Booting takes 62 to 68 s. `systemd` reports the service as started right away, but for about a minute it refuses connections, so a sync call in that window fails exactly like a stopped service. Every deploy opens that window; the async path is not affected, because the message waits in the stream.
- A Java process stopped by `systemd` exits with code 143 (SIGTERM), which `systemd` reports as `failed`. A `SuccessExitStatus=143` override fixes the unit state.
- The delivery-service stayed under 390 MB of memory at its peak with `-Xmx400m`.
  **What I learned**

- "Down" is not one failure. A stopped process fails in about a second; a dropped network hangs. Only a timeout puts a bound on the second one.
- Without timeouts the caller inherits the slowest failure of its dependency, and here it also held a database connection while waiting.
- A log line that says `null` is almost as bad as no log: "connection refused" and "connect timed out" point to different problems.
- A timeout should come from measurements, not habit. The cold start of 8.4 s decided the 10 s read timeout.
- The async path decouples availability, and the price is the meaning of the answer: `202` means "accepted", not "delivered".
- A sync timeout can mark an event `DISPATCH_FAILED` while the delivery-service is still working on it, so the two services can disagree about the same event.
## Security notes

- The delivery-service port is open only to the webhook-service's private address and to my own IP. The webhook-service is public so the API and Swagger can be demonstrated.
- **There is no authentication, by design** (it is a non-goal of the lab). A consequence worth knowing: anyone who can reach the webhook-service can register an arbitrary `targetUrl`, which makes the system an open HTTP caller. Documented, not fixed.

---

## Known limitations (and which experiment addresses each)

| Limitation | Experiment |
|---|---|
| **Dual write:** the message is published before the event row is saved. A crash in between leaves a message for an event that does not exist. | 9 |
| **`QUEUED` never changes** on this side, even after a successful delivery. | 8 |
| **Pending messages are not redelivered.** The group reads with `lastConsumed()`, so a message left pending (failed `create()`, crash) is never retried. | 6 |
| **Duplicate window:** a crash between `create()` and `XACK`, or a failed `XACK`, redelivers the message and creates a second `DeliveryAttempt`. There is no unique constraint on `(event_id, subscription_id)` yet. | 7 |
| **`XACK` means "persisted", not "delivered".** The HTTP delivery runs asynchronously after `create()`. | 6, 8 |
| **Redis down at boot** stops the delivery-service from starting at all, HTTP endpoints included. Behavior on read errors while running is untested. | 5 |
| **Malformed messages are discarded**, with no dead-letter stream. | 6 |
| **Publish timeouts** are at their defaults, so an unreachable Redis could hold an HTTP request open. | 5 |
| **No retry on the sync path.** A `DISPATCH_FAILED` event is final; nothing resends it. | 6 |
| **A read timeout can create a false failure:** the delivery-service may finish after the webhook-service gave up, leaving the event `DISPATCH_FAILED` while the delivery succeeded. | 7, 8 |
| **Startup window:** for about a minute after a restart the delivery-service accepts no connections, and sync calls in that window fail. | 6 |
| **Sync dual write:** the event is saved *after* the call to the delivery-service. A crash in between leaves a delivery for an event the webhook-service never stored. | 9 |
| **The same pair can be delivered twice over HTTP too:** `POST /deliveries` accepted the same `(event_id, subscription_id)` three times in manual tests, because there is no unique constraint. | 7 |

---

## Running locally

Requirements: JDK, Maven, Docker (Testcontainers), a MySQL database and a Redis instance.

```bash
cp .env.example .env   # fill in the values
mvn test
mvn spring-boot:run
```

| Variable | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | this service's MySQL |
| `DELIVERY_SERVICE_URL` | base URL of the delivery-service (sync path) |
| `REDIS_URL` | `rediss://...` URL of the Redis instance |
| `EVENT_STREAM_DELIVERIES_KEY` | optional override of the stream key |

The delivery-service must use the **same** `REDIS_URL` and stream key. If the two keys differ, the consumer reads an empty stream and nothing fails visibly.

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Stack

Java, Spring Boot 4.1 (Spring MVC, Spring Data JPA, Spring Data Redis), MySQL on Aiven, Flyway, Redis Streams on Upstash, SpringDoc OpenAPI, Mockito, Testcontainers, Maven, Linux, `systemd`, Oracle Cloud Infrastructure.