package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "upi_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpiRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payer_vpa", nullable = false, length = 100)
    private String payerVpa;

    @Column(name = "payee_vpa", nullable = false, length = 100)
    private String payeeVpa;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UpiRequestStatus status = UpiRequestStatus.PENDING;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = new Date();
        if (status == null) status = UpiRequestStatus.PENDING;
    }
}


