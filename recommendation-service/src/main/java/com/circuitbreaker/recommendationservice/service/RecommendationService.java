package com.circuitbreaker.recommendationservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.circuitbreaker.recommendationservice.model.Recommendation;
import com.circuitbreaker.recommendationservice.model.RecommendationItem;
import com.circuitbreaker.recommendationservice.model.RecommendationResponse;
import com.circuitbreaker.recommendationservice.repository.RecommendationRepository;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final RecommendationRepository repository;

    public RecommendationService(RecommendationRepository repository) {
        this.repository = repository;
    }

    public RecommendationResponse topSellers() {
        return RecommendationResponse.live(null, items(Recommendation.GLOBAL));
    }

    public RecommendationResponse forProduct(Long productId) {
        return RecommendationResponse.live(productId, items(productId));
    }

    private List<RecommendationItem> items(Long sourceProductId) {
        return repository.findBySourceProductIdOrderByScoreDesc(sourceProductId).stream()
                .map(RecommendationItem::from)
                .toList();
    }
}
