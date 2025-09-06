package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "upi_vpa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpiVpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String vpa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "upi_pin_hash", length = 255)
    private String upiPinHash;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = new Date();
        if (isDefault == null) isDefault = false;
    }
}


