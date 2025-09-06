
package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suggest_operator")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestOperator {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String circle;
}
