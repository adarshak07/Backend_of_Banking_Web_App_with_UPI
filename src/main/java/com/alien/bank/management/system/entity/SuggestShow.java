
package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suggest_show")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestShow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "movie_id")
    private Long movieId;
    @Column(name = "theatre_id")
    private Long theatreId;
    @Column(name = "show_time")
    private String showTime;
    private Double price;
}
