package com.circuitbreaker.gateway.monitoring;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Actively probes {@code /actuator/health} on every discovered instance of the
 * monitored services.
 *
 * <p>We do not rely on Eureka's status alone: Eureka keeps a crashed instance
 * listed until its lease expires, whereas a direct probe notices within a second.
 */
@Component
public class ServiceHealthProbe {

    public record ServiceHealth(String service, String status, int instances, int healthyInstances) {
    }

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(1);

    private final ReactiveDiscoveryClient discoveryClient;
    private final List<String> monitoredServices;
    private final WebClient webClient = WebClient.create();

    public ServiceHealthProbe(ReactiveDiscoveryClient discoveryClient,
                              @Value("${platform.monitored-services:}") String monitoredServices) {
        this.discoveryClient = discoveryClient;
        this.monitoredServices = Arrays.stream(monitoredServices.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Mono<List<ServiceHealth>> probeAll() {
        return Flux.fromIterable(monitoredServices)
                .concatMap(this::probe)          // concatMap keeps the configured order
                .collectList();
    }

    private Mono<ServiceHealth> probe(String serviceId) {
        return discoveryClient.getInstances(serviceId)
                .collectList()
                .onErrorReturn(List.of())
                .flatMap(instances -> {
                    if (instances.isEmpty()) {
                        return Mono.just(new ServiceHealth(serviceId, "DOWN", 0, 0));
                    }
                    return Flux.fromIterable(instances)
                            .flatMap(this::isHealthy)
                            .filter(Boolean::booleanValue)
                            .count()
                            .map(healthy -> new ServiceHealth(
                                    serviceId,
                                    statusOf(healthy.intValue(), instances.size()),
                                    instances.size(),
                                    healthy.intValue()));
                });
    }

    private Mono<Boolean> isHealthy(ServiceInstance instance) {
        return webClient.get()
                .uri(instance.getUri() + "/actuator/health")
                .retrieve()
                .toBodilessEntity()
                .map(response -> true)
                .timeout(PROBE_TIMEOUT)
                .onErrorReturn(false);
    }

    private static String statusOf(int healthy, int total) {
        if (healthy == 0) {
            return "DOWN";
        }
        return healthy == total ? "UP" : "DEGRADED";
    }
}
