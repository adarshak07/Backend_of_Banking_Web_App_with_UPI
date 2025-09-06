
package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.SuggestContact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SuggestContactRepository extends JpaRepository<SuggestContact, Long> {
    List<SuggestContact> findByNameContainingIgnoreCase(String q);
    List<SuggestContact> findByPhoneContaining(String q);
}
