package com.circuitbreaker.recommendationservice.model;

public record RecommendationItem(Long productId, String name, String reason) {

    public static RecommendationItem from(Recommendation r) {
        return new RecommendationItem(r.getRecommendedProductId(), r.getRecommendedName(), r.getReason());
    }
}
