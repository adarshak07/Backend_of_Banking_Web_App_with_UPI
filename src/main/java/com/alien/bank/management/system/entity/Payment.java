package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_payment_ref_id", columnList = "ref_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    private PaymentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMethod method; // UPI or CARD

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status; // PENDING/SUCCESS/FAILED

    @Column(name = "ref_id", nullable = false, unique = true, length = 64)
    private String refId;

    // store JSON as text for MySQL compatibility; Flyway created JSON type, fallback works via Hibernate
    @Column(columnDefinition = "json")
    private String meta;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = new Date();
    }
}


