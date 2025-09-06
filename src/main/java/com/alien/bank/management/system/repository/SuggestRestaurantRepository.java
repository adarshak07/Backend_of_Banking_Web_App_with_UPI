
package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.SuggestRestaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuggestRestaurantRepository extends JpaRepository<SuggestRestaurant, Long> {
    List<SuggestRestaurant> findByNameContainingIgnoreCase(String q);
}
