# Resume & Interview Guide

## Resume entry (copy / adapt)

**CircuitBreaker Platform - Fault-Tolerant Microservices Gateway** | Java 17, Spring Boot 3, Spring Cloud Gateway, Resilience4j, Eureka, React, Docker, Zipkin
*GitHub: <your-link>*

- Designed and built a cloud-native e-commerce platform of 3 Spring Boot microservices behind a **Spring Cloud Gateway**, with **Eureka** service discovery and client-side load balancing, orchestrated with **Docker Compose**.
- Implemented layered resilience (**circuit breaker, bulkhead, time limiter, retry, per-client token-bucket rate limiter**) with graceful fallbacks; under injected latency the gateway tripped the breaker after 3 timeouts and served fallbacks in ~10 ms instead of 3 s while unaffected routes stayed healthy.
- Built a **chaos-engineering dashboard** (React, Chart.js) with live breaker state (CLOSED / OPEN / HALF-OPEN), failure-rate metrics, transition timeline and one-click latency/error injection to demonstrate recovery.
- Added **distributed tracing** (Micrometer + Zipkin), Prometheus metrics, active health probing, RFC 7807 error handling, optimistic-locking stock reservation, unit/integration tests and a GitHub Actions CI pipeline.

Shorter (2-bullet) version:
- Built a fault-tolerant microservices platform (Spring Cloud Gateway, Eureka, Resilience4j, Docker) using circuit breakers, bulkheads, rate limiting, retries and fallbacks to prevent cascading failures.
- Created a React chaos dashboard and Zipkin tracing to inject failures live and visualise breaker state transitions and recovery.

## 60-second pitch

"In microservices, one slow dependency can exhaust threads and take the whole system down - a cascading failure. I built a gateway that prevents that. Each route has a rate limiter, a bulkhead, a time limit and a circuit breaker. When the Recommendation service gets slow, the breaker opens after a few failures and the gateway returns top-seller fallback data in about 10 ms, while checkout-critical services keep working. After 10 seconds it probes the service and closes again by itself. I also built a dashboard that lets me break the service on purpose and watch it happen."

## Likely questions and answers

**What is a circuit breaker and how does yours behave?**
Three states. CLOSED: calls pass and results are recorded in a sliding window of 6. Once at least 4 calls are recorded and 50 % fail (or time out), it goes OPEN and rejects calls immediately for 10 s. Then HALF_OPEN lets 3 trial calls through; success closes it, failure re-opens it.

**Why a time limiter as well?**
A breaker only learns about failure when calls end. Without a timeout, a hanging call neither succeeds nor fails, so threads pile up. The 3 s limit converts "slow" into "failed".

**Difference between bulkhead and rate limiter?**
Bulkhead limits *concurrent in-flight* calls to protect capacity from a slow backend. Rate limiter limits *requests per time* per client to protect from abuse. Different failure modes.

**Why is Retry inside the circuit breaker?**
So the breaker records one outcome per client request. Outside it, each retry attempt would count and the breaker would trip too eagerly; retries would also keep hammering an already open circuit.

**Why not Resilience4j's RateLimiter?**
It is global per instance. I needed one token bucket per client IP per route, so I wrote a filter (token bucket with injectable clock, tested deterministically, idle eviction to avoid a memory leak).

**What are the limits of your rate limiter?**
State is in-memory on one gateway instance; scaling out needs a shared store (Redis). `X-Forwarded-For` is trusted only behind a known proxy because it can be forged.

**Why does the fallback for products return 503 but recommendations return 200?**
Recommendations are optional and have a safe default. Product/inventory data is authoritative; fabricating it could cause wrong orders, so fail honestly.

**How does service discovery work here?**
Services register with Eureka and send heartbeats. The gateway resolves `lb://service-name` through Spring Cloud LoadBalancer using the registry. I also probe `/actuator/health` directly because Eureka keeps dead instances until the lease expires.

**How would you scale or harden this for production?**
Redis-backed rate limiting, PostgreSQL per service, OAuth2/JWT at the gateway, remove the chaos route, restore Eureka defaults, Prometheus/Grafana alerts on breaker state, Kubernetes with readiness probes, load tests with k6.

**How do you prevent two customers buying the last item?**
`Inventory` has a `@Version` column (optimistic locking) and `reserve` runs in a transaction; concurrent writers get a conflict (409) instead of overselling.

**How does tracing work?**
Micrometer Tracing (Brave bridge) adds trace/span IDs, propagates them in headers through the gateway to services, and reports to Zipkin; the IDs also appear in logs.

## Be ready to say honestly

- The databases are in-memory H2 and the fallback is static; both are documented extension points.
- Eureka timers are tuned for demos.
- Latency numbers (3 s timeout, ~10 ms fallback) were measured locally against mock backends, not under production load.

## How to present it

1. **Pin the repo** on your GitHub profile; put the GIF and 3-4 screenshots at the top of the README.
2. **In an interview, demo for 90 seconds**: healthy -> inject latency -> breaker OPEN with fast fallbacks -> heal -> CLOSED, pointing at the timeline.
3. **Know three numbers**: 3 s time limit; opens after 3 slow calls (50 % of a 6-call window); fallback served in ~10 ms vs 3 s waiting.
4. **Have two honest improvements ready**: Redis-backed rate limiting and PostgreSQL instead of H2.
5. **Run `scripts/smoke-test.ps1` before any demo** so you know the system is healthy.

## Skills this project evidences (use for resume keywords)

Java 17, Spring Boot 3, Spring Cloud Gateway, Spring Cloud LoadBalancer, Netflix Eureka, Resilience4j (CircuitBreaker, Bulkhead, TimeLimiter, Retry), reactive programming (WebFlux/Reactor), REST API design, Spring Data JPA, Bean Validation, optimistic locking, RFC 7807 errors, Micrometer, Prometheus, Zipkin/distributed tracing, Docker and Docker Compose, Maven multi-module builds, JUnit 5, GitHub Actions CI, React 19, Vite, Chart.js, chaos engineering.
