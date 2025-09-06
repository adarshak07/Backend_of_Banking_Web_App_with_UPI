
package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suggest_restaurant")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestRestaurant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String cuisine;
    private Double rating;
}
