package com.circuitbreaker.gateway.filter;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import reactor.core.publisher.Mono;

/**
 * Bulkhead (concurrency limiter) for a route.
 *
 * <p>Limits how many requests may be in flight to one backend at the same time,
 * so a slow service cannot absorb every connection/thread of the gateway and
 * starve the healthy routes. Excess requests are rejected immediately with 503.
 *
 * <p>The limits come from {@code resilience4j.bulkhead.instances.<name>} in
 * {@code application.yml}. The permit is released on completion, error
 * <em>and</em> cancellation (e.g. when the time limiter aborts the call).
 */
@Component
public class BulkheadGatewayFilterFactory
        extends AbstractGatewayFilterFactory<BulkheadGatewayFilterFactory.Config> {

    private final BulkheadRegistry bulkheadRegistry;

    public BulkheadGatewayFilterFactory(BulkheadRegistry bulkheadRegistry) {
        super(Config.class);
        this.bulkheadRegistry = bulkheadRegistry;
    }

    @Override
    public GatewayFilter apply(Config config) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(config.getName());

        return (exchange, chain) -> {
            if (!bulkhead.tryAcquirePermission()) {
                exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                exchange.getResponse().getHeaders().set("X-Bulkhead-Rejected", "true");
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                byte[] body = ("{\"status\":503,\"error\":\"Bulkhead Full\",\"message\":\"Too many concurrent requests to "
                        + config.getName() + ". Please retry shortly.\"}").getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            }
            return chain.filter(exchange).doFinally(signal -> bulkhead.onComplete());
        };
    }

    public static class Config {

        private String name = "default";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
