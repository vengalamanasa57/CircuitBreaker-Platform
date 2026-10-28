package com.circuitbreaker.recommendationservice.model;

import java.util.List;

/**
 * Response contract shared with the gateway's fallback: the {@code source} and
 * {@code degraded} fields let any client tell live data from fallback data.
 */
public record RecommendationResponse(String source, boolean degraded, Long forProductId,
                                     List<RecommendationItem> recommendations) {

    public static final String SOURCE = "recommendation-service";

    public static RecommendationResponse live(Long forProductId, List<RecommendationItem> items) {
        return new RecommendationResponse(SOURCE, false, forProductId, items);
    }
}
