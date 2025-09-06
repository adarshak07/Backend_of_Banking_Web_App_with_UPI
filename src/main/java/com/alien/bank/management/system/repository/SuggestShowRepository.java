
package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.SuggestShow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuggestShowRepository extends JpaRepository<SuggestShow, Long> {
    List<SuggestShow> findByMovieId(Long movieId);
}
