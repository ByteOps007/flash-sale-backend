# Flash Sale & Inventory Engine — Backend

A real-time, event-driven flash sale and inventory system built to survive genuine concurrent-traffic bursts without overselling — the classic hard problem behind every ticketing site, sneaker drop, and Black Friday sale.

**Frontend repo:** [flash-sale-frontend](https://github.com/ByteOps007/flash-sale-frontend)

<!-- Drop your demo GIF here once recorded -->
<!-- ![Demo](docs/demo.gif) -->

## What this actually solves

Flash sales create a specific hard problem: thousands of people can hit "Buy" on the same item within the same second. A naive implementation (`read stock → check if > 0 → decrement → save`) will oversell under concurrency, because two requests can both read "1 left" before either one writes back the decrement.

This project solves that with a **two-layer concurrency-safe purchase flow**:

1. **Redis fast-path gate** — every purchase attempt first hits an atomic `DECR` against a Redis-cached stock count. Redis is single-threaded, so this is race-free even under thousands of simultaneous requests, and it rejects most over-capacity requests *before they ever touch the database*.
2. **PostgreSQL source of truth** — requests that pass the Redis gate are confirmed against Postgres with a single atomic conditional `UPDATE ... WHERE stock >= quantity`. If Redis and Postgres ever drift out of sync, this layer catches it and the request is rejected cleanly.

Every confirmed purchase is published to **Kafka**, consumed by a listener, and broadcast to connected clients over **WebSocket (STOMP)** — so stock counts update live in the browser with no polling.

## Architecture

```
                    ┌─────────────┐
   Buy click ─────▶ │  Next.js    │
                    │  Frontend   │◀──────────────┐
                    └──────┬──────┘                │
                           │ REST                   │ WebSocket
                           ▼                         │ (live stock updates)
                    ┌─────────────┐                 │
                    │ Spring Boot │─────────────────┘
                    │   REST API  │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼             ▼
        ┌──────────┐ ┌──────────┐ ┌───────────┐
        │  Redis   │ │ Postgres │ │   Kafka   │
        │(fast-path│ │ (source  │ │ (order-   │
        │  DECR)   │ │ of truth)│ │  events)  │
        └──────────┘ └──────────┘ └─────┬─────┘
                                         │
                                         ▼
                                ┌─────────────────┐
                                │ StockUpdateListener │
                                │  → WebSocket broadcast │
                                └─────────────────┘
```

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3, Java 17 |
| Database | PostgreSQL |
| Cache / concurrency gate | Redis |
| Event streaming | Apache Kafka |
| Real-time push | WebSocket (STOMP) |
| Frontend | Next.js 14, TypeScript, Tailwind CSS |
| Local infra | Docker Compose |
| Load testing | k6 |

## Load testing & what I learned

I didn't just build this and assume it worked — I load-tested it with k6 and found (then fixed) three real concurrency issues, in order of how deep they were hiding:

**1. Embedded Tomcat's connection capacity.**
Spring Boot's default embedded Tomcat caps out at 200 threads + 100 accept-queue = 300 simultaneous connections. Firing exactly 300 concurrent requests caused ~168 of them to be refused *before reaching application code at all*. Fixed by raising `server.tomcat.threads.max` and `server.tomcat.accept-count`.

**2. Optimistic-locking contention on a single row.**
My first DB-layer implementation used JPA's `@Version`-based optimistic locking: read the row, then write it back only if the version hadn't changed. Under heavy contention, dozens of Redis-approved requests hit the *same* product row simultaneously — only one write can win per version, so most legitimate buyers were wrongly rejected as "conflicts," even though real stock was still available. A bounded retry loop helped, but not enough at real concurrency levels (100 requests → only ~29 confirmed against a stock of 50).

**3. Spring's self-invocation `@Transactional` bypass.**
Switching to a single atomic `UPDATE ... WHERE stock >= quantity` query should have solved #2 outright — but confirmed orders dropped to *zero*. The cause: `@Transactional` only works when a method is called *through* Spring's proxy. My transactional method was being called via `this.method()` from within the same class, which silently bypasses the proxy entirely — so no transaction was ever actually open, and the `@Modifying` query (which requires an active transaction) failed on every call. Fixed by extracting the transactional logic into its own Spring bean, so the call goes through a real proxy.

**Final result:** 100 concurrent requests against 50 units of stock → exactly 50 `CONFIRMED`, exactly 50 `OUT_OF_STOCK`, zero conflicts, zero errors.

```
==================================================
  FLASH SALE LOAD TEST RESULTS
==================================================
  CONFIRMED orders:          50
  OUT_OF_STOCK rejections:   50
  CONFLICT_RETRY (DB layer): 0
  HTTP-level failures:       0
==================================================
```

## Getting started

### Prerequisites
- Docker Desktop
- Java 17
- Maven (or use IntelliJ's bundled Maven)

### 1. Start local infra
```bash
docker-compose up -d
```
This starts Postgres, Redis, Kafka, Zookeeper, and a Kafka UI dashboard at `http://localhost:8081`.

### 2. Run the backend
Open in IntelliJ, let Maven import dependencies, then run `FlashSaleEngineApplication`. It starts on `http://localhost:8080`.

### 3. Create a product
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Limited Sneakers","price":149.99,"stock":50}'
```

### 4. Purchase it
```bash
curl -X POST http://localhost:8080/api/purchase \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"userId":"user123","quantity":1}'
```

### 5. Run the frontend
See the [frontend repo](https://github.com/ByteOps007/flash-sale-frontend) for setup — it connects to this backend on `localhost:8080` and shows live stock updates.

## API reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/products` | List all products |
| `GET` | `/api/products/{id}` | Get a single product |
| `POST` | `/api/products` | Create a product (also seeds Redis stock) |
| `DELETE` | `/api/products/{id}` | Delete a product |
| `POST` | `/api/purchase` | Attempt a purchase (two-layer concurrency-safe flow) |
| `WS` | `/ws` (STOMP, topic `/topic/stock-updates`) | Live purchase event feed |

## Load testing it yourself

A k6 script is included for reproducing the results above:

```bash
k6 run --env PRODUCT_ID=<id> --env VUS=100 stress-test.js
```

## Known limitations / next steps

- Products created before a stock-cache resync feature was in place can drift out of sync between Redis and Postgres (a known, understood tradeoff of a two-store design — a `/resync-stock` endpoint would address this).
- Auth (Clerk) isn't wired into the purchase flow yet — `userId` is currently passed directly by the client.
- Single-instance Tomcat has a real capacity ceiling; a production deployment would sit this behind a load balancer / reverse proxy for genuinely massive bursts.

## Author

**Ansh Bhardwaj** — [GitHub](https://github.com/ByteOps007)
