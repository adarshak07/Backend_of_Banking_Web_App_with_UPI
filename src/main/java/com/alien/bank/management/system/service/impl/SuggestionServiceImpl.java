
package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {

    private final SuggestContactRepository contactRepository;
    private final SuggestOperatorRepository operatorRepository;
    private final SuggestPlanRepository planRepository;
    private final SuggestMovieRepository movieRepository;
    private final SuggestRestaurantRepository restaurantRepository;

    @Override
    public List<SuggestContact> searchContacts(String q, int limit) {
        return contactRepository.findByNameContainingIgnoreCase(q).stream().limit(limit).toList();
    }

    @Override
    public List<SuggestOperator> searchOperators(String q, int limit) {
        return operatorRepository.findByNameContainingIgnoreCase(q).stream().limit(limit).toList();
    }

    @Override
    public List<SuggestPlan> getPlansForOperator(Long operatorId) {
        return planRepository.findByOperatorId(operatorId);
    }

    @Override
    public List<SuggestMovie> searchMovies(String q, int limit) {
        return movieRepository.findByTitleContainingIgnoreCase(q).stream().limit(limit).toList();
    }

    @Override
    public List<SuggestRestaurant> searchRestaurants(String q, int limit) {
        return restaurantRepository.findByNameContainingIgnoreCase(q).stream().limit(limit).toList();
    }
}
