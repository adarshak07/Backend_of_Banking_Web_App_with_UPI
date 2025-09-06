
package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.SuggestMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuggestMovieRepository extends JpaRepository<SuggestMovie, Long> {
    List<SuggestMovie> findByTitleContainingIgnoreCase(String q);
}
