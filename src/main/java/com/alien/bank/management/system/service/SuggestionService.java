
package com.alien.bank.management.system.service;

import java.util.List;
import com.alien.bank.management.system.entity.*;

public interface SuggestionService {
    List<SuggestContact> searchContacts(String q, int limit);
    List<SuggestOperator> searchOperators(String q, int limit);
    List<SuggestPlan> getPlansForOperator(Long operatorId);
    List<SuggestMovie> searchMovies(String q, int limit);
    List<SuggestRestaurant> searchRestaurants(String q, int limit);
}
