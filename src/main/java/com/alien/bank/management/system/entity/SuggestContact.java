
package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suggest_contact")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestContact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String phone;
    private String avatar;
    @Column(name = "last_used")
    private java.util.Date lastUsed;
}
