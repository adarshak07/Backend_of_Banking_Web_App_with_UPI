
package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.SuggestTheatre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuggestTheatreRepository extends JpaRepository<SuggestTheatre, Long> {
    List<SuggestTheatre> findByNameContainingIgnoreCase(String q);
}
