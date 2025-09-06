package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gift_card_product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftCardProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(name = "value_rupees", nullable = false)
    private Integer valueRupees;

    @Column(name = "cost_coins", nullable = false)
    private Integer costCoins;

    @Column(nullable = false)
    private Boolean active;
}


