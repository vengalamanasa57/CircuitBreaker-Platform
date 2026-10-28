package com.circuitbreaker.recommendationservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.circuitbreaker.recommendationservice.chaos.ChaosState;
import com.circuitbreaker.recommendationservice.model.RecommendationResponse;
import com.circuitbreaker.recommendationservice.service.RecommendationService;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService service;
    private final ChaosState chaos;

    public RecommendationController(RecommendationService service, ChaosState chaos) {
        this.service = service;
        this.chaos = chaos;
    }

    /** Global top sellers. */
    @GetMapping
    public RecommendationResponse topSellers() {
        chaos.applyTo();
        return service.topSellers();
    }

    /** "Customers who viewed this product also liked ..." */
    @GetMapping("/{productId}")
    public RecommendationResponse forProduct(@PathVariable Long productId) {
        chaos.applyTo();
        return service.forProduct(productId);
    }

    /** Manual test hook: always answers after 5 s (longer than the gateway's 3 s time limit). */
    @GetMapping("/slow")
    public RecommendationResponse slow() throws InterruptedException {
        Thread.sleep(5_000);
        return service.topSellers();
    }

    /** Manual test hook: always fails with HTTP 500. */
    @GetMapping("/fail")
    public RecommendationResponse fail() {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Recommendation service failure");
    }
}
