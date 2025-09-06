
package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.SuggestMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuggestMenuItemRepository extends JpaRepository<SuggestMenuItem, Long> {
    List<SuggestMenuItem> findByNameContainingIgnoreCase(String q);
    List<SuggestMenuItem> findByRestaurantId(Long restaurantId);
}
