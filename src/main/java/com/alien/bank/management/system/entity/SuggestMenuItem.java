
package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suggest_menu_item")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestMenuItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "restaurant_id")
    private Long restaurantId;
    private String name;
    private Double price;
}
