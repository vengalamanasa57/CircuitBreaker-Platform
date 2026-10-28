package com.circuitbreaker.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the resilient API gateway.
 *
 * <p>All traffic from the dashboard / clients enters here and is routed to the
 * backend microservices through Eureka-based service discovery, protected by
 * rate limiting, bulkheads, time limits, retries and circuit breakers.
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
