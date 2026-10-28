package com.circuitbreaker.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Per-client, per-route token-bucket rate limiter (protects against scraping /
 * DDoS-style bursts). Rejected requests get {@code 429 Too Many Requests} and
 * never reach the circuit breaker, so abusive clients cannot trip it.
 *
 * <p>Why custom instead of Resilience4j's RateLimiter? Resilience4j limits are
 * global to a named instance; here every client IP needs its own bucket.
 *
 * <p>Usage in a route:
 * <pre>
 * - name: InMemoryRateLimit
 *   args: { capacity: 20, refillPerSecond: 10 }
 * </pre>
 *
 * <p>Limitations (documented on purpose): state is local to one gateway instance.
 * For a horizontally scaled gateway, back the buckets with Redis.
 */
@Component
public class InMemoryRateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<InMemoryRateLimitGatewayFilterFactory.Config> {

    private static final long IDLE_EVICTION_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final long CLEANUP_EVERY_N_REQUESTS = 1_000;

    private final boolean trustForwardedFor;

    public InMemoryRateLimitGatewayFilterFactory(
            @Value("${platform.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor) {
        super(Config.class);
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    public GatewayFilter apply(Config config) {
        // One bucket map per route instance => limits are per client AND per route.
        Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
        AtomicLong requestCounter = new AtomicLong();

        return (exchange, chain) -> {
            evictIdleBucketsOccasionally(buckets, requestCounter);

            TokenBucket bucket = buckets.computeIfAbsent(resolveClientKey(exchange),
                    key -> new TokenBucket(config.getCapacity(), config.getRefillPerSecond()));

            boolean allowed = bucket.tryConsume();
            var headers = exchange.getResponse().getHeaders();
            headers.set("X-RateLimit-Limit", String.valueOf(config.getCapacity()));
            headers.set("X-RateLimit-Remaining", String.valueOf(bucket.remaining()));

            if (!allowed) {
                headers.set("Retry-After", "1");
                return reject(exchange);
            }
            return chain.filter(exchange);
        };
    }

    private static void evictIdleBucketsOccasionally(Map<String, TokenBucket> buckets, AtomicLong counter) {
        if (counter.incrementAndGet() % CLEANUP_EVERY_N_REQUESTS == 0) {
            buckets.entrySet().removeIf(e -> e.getValue().isIdleFor(IDLE_EVICTION_NANOS));
        }
    }

    private static Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Slow down and retry.\"}"
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String resolveClientKey(ServerWebExchange exchange) {
        if (trustForwardedFor) {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        var remote = exchange.getRequest().getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }
        return "anonymous";
    }

    public static class Config {

        private int capacity = 10;
        private int refillPerSecond = 5;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillPerSecond() {
            return refillPerSecond;
        }

        public void setRefillPerSecond(int refillPerSecond) {
            this.refillPerSecond = refillPerSecond;
        }
    }
}
