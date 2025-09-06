package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table( // keep your existing table name; just add an index for fast lookups
    indexes = {
        @Index(name = "idx_txn_account_ts", columnList = "account_id, timestamp")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // store the operation kind
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionType type;   // DEPOSIT / WITHDRAW

    // amount moved in this operation
    @Column(nullable = false, updatable = false)
    private Double amount;

    // critical for the history screen
    @Column(nullable = false, updatable = false)
    private Double balanceAfter;

    // when it happened
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date timestamp;

    // optional notes you already had
    @Column
    private String notes;

    // owner account
    @ManyToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false, updatable = false)
    private Account account;

    @PrePersist
    void onCreate() {
        if (timestamp == null) timestamp = new Date();
    }
}