package com.circuitbreaker.recommendationservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.circuitbreaker.recommendationservice.model.Recommendation;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findBySourceProductIdOrderByScoreDesc(Long sourceProductId);
}
