package com.circuitbreaker.recommendationservice.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.circuitbreaker.recommendationservice.model.Recommendation;
import com.circuitbreaker.recommendationservice.repository.RecommendationRepository;

/** Seeds the global top-seller list plus 3 related products for each catalogue item (ids 1..8). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final List<String> CATALOGUE = List.of(
            "Laptop", "Wireless Headphones", "Smartphone", "Mechanical Keyboard",
            "4K Monitor", "Smartwatch", "Gaming Mouse", "USB-C Hub");

    private final RecommendationRepository repository;

    public DataSeeder(RecommendationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<Recommendation> rows = new ArrayList<>();

        // Global top sellers (matches the gateway's static fallback list).
        for (int i = 0; i < 4; i++) {
            rows.add(new Recommendation(Recommendation.GLOBAL, (long) i + 1, CATALOGUE.get(i),
                    "Top seller", 100 - i));
        }

        // "Also liked": the next three catalogue items, wrapping around.
        for (int source = 0; source < CATALOGUE.size(); source++) {
            for (int offset = 1; offset <= 3; offset++) {
                int target = (source + offset) % CATALOGUE.size();
                rows.add(new Recommendation((long) source + 1, (long) target + 1, CATALOGUE.get(target),
                        "Customers also liked", 90 - offset * 10));
            }
        }
        repository.saveAll(rows);
    }
}
