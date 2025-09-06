package com.alien.bank.management.system.entity;

import jakarta.persistence.*;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.ConstraintMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "reward_daily_summary", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reward_daily", columnNames = {"user_id", "day"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardDailySummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date day;

    @Column(name = "tx_count", nullable = false)
    private Integer txCount;

    @Column(name = "bonus_given", nullable = false)
    private Boolean bonusGiven;
}


