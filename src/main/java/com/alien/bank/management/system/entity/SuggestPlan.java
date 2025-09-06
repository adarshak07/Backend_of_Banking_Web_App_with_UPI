
package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suggest_plan")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "operator_id")
    private Long operatorId;
    private String label;
    private Double price;
    private String validity;
    @Column(name = "data_info")
    private String dataInfo;
}
