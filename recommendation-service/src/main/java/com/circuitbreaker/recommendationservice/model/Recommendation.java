package com.circuitbreaker.recommendationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * "Customers who viewed {@code sourceProductId} also liked {@code recommendedProductId}".
 * {@code sourceProductId == 0} marks the global top-seller list.
 */
@Entity
public class Recommendation {

    public static final long GLOBAL = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceProductId;

    @Column(nullable = false)
    private Long recommendedProductId;

    @Column(nullable = false)
    private String recommendedName;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private int score;

    protected Recommendation() {
        // required by JPA
    }

    public Recommendation(Long sourceProductId, Long recommendedProductId, String recommendedName,
                          String reason, int score) {
        this.sourceProductId = sourceProductId;
        this.recommendedProductId = recommendedProductId;
        this.recommendedName = recommendedName;
        this.reason = reason;
        this.score = score;
    }

    public Long getRecommendedProductId() {
        return recommendedProductId;
    }

    public String getRecommendedName() {
        return recommendedName;
    }

    public String getReason() {
        return reason;
    }

    public int getScore() {
        return score;
    }
}
