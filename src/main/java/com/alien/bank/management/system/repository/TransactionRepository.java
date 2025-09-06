package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.Transaction;
import com.alien.bank.management.system.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ✅ Simple history lookup
    List<Transaction> findByAccountIdOrderByTimestampDesc(Long accountId);

    // New: Find transactions for an account between dates, sorted by timestamp asc
    List<Transaction> findByAccountIdAndTimestampBetweenOrderByTimestampAsc(Long accountId, Date fromDate, Date toDate);

    // New: Find recent N transactions (Spring will limit via Pageable when used)
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    // ✅ Enhanced querying with enum type
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
           "AND (:fromDate IS NULL OR t.timestamp >= :fromDate) " +
           "AND (:toDate IS NULL OR t.timestamp <= :toDate) " +
           "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
           "AND (:transactionTypes IS NULL OR t.type IN :transactionTypes) " +  // enum, no .name()
           "AND (:searchText IS NULL OR LOWER(t.notes) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    Page<Transaction> findTransactionsWithFilters(
            @Param("accountId") Long accountId,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("transactionTypes") List<TransactionType> transactionTypes,
            @Param("searchText") String searchText,
            Pageable pageable
    );

    // ✅ Summary statistics query (enum constants directly)
    @Query("SELECT " +
           "COUNT(t) as totalTransactions, " +
           "COALESCE(SUM(CASE WHEN t.type = com.alien.bank.management.system.entity.TransactionType.DEPOSIT THEN t.amount ELSE 0 END), 0) as totalDeposits, " +
           "COALESCE(SUM(CASE WHEN t.type = com.alien.bank.management.system.entity.TransactionType.WITHDRAW THEN t.amount ELSE 0 END), 0) as totalWithdrawals, " +
           "COALESCE(SUM(CASE WHEN t.type = com.alien.bank.management.system.entity.TransactionType.FEE THEN t.amount ELSE 0 END), 0) as totalFees, " +
           "COALESCE(SUM(CASE WHEN t.type = com.alien.bank.management.system.entity.TransactionType.INTEREST THEN t.amount ELSE 0 END), 0) as totalInterest " +
           "FROM Transaction t WHERE t.account.id = :accountId " +
           "AND (:fromDate IS NULL OR t.timestamp >= :fromDate) " +
           "AND (:toDate IS NULL OR t.timestamp <= :toDate)")
    TransactionSummaryProjection getTransactionSummary(
            @Param("accountId") Long accountId,
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate
    );

    // ✅ Opening balance (fetch only first row)
    @Query("SELECT t.balanceAfter - t.amount FROM Transaction t " +
           "WHERE t.account.id = :accountId " +
           "AND t.timestamp < :fromDate " +
           "ORDER BY t.timestamp DESC")
    List<Double> findOpeningBalanceRaw(@Param("accountId") Long accountId, @Param("fromDate") Date fromDate);

    // ✅ Closing balance (fetch only first row)
    @Query("SELECT t.balanceAfter FROM Transaction t " +
           "WHERE t.account.id = :accountId " +
           "AND t.timestamp <= :toDate " +
           "ORDER BY t.timestamp DESC")
    List<Double> findClosingBalanceRaw(@Param("accountId") Long accountId, @Param("toDate") Date toDate);

    // 👉 convenience default methods to unwrap first element
    default Double getOpeningBalance(Long accountId, Date fromDate) {
        List<Double> result = findOpeningBalanceRaw(accountId, fromDate);
        return result.isEmpty() ? 0.0 : result.get(0);
    }

    default Double getClosingBalance(Long accountId, Date toDate) {
        List<Double> result = findClosingBalanceRaw(accountId, toDate);
        return result.isEmpty() ? 0.0 : result.get(0);
    }

    // ✅ Interface for summary projection
    interface TransactionSummaryProjection {
        Long getTotalTransactions();
        Double getTotalDeposits();
        Double getTotalWithdrawals();
        Double getTotalFees();
        Double getTotalInterest();
    }

    // Sum of outgoing transfers/payments within a date range for daily limit enforcement
    @Query("SELECT COALESCE(SUM(t.amount),0) FROM Transaction t WHERE t.account.id = :accountId AND t.timestamp BETWEEN :from AND :to AND (t.type = com.alien.bank.management.system.entity.TransactionType.TRANSFER_OUT OR t.type = com.alien.bank.management.system.entity.TransactionType.PAYMENT)")
    Double sumOutgoingForRange(@Param("accountId") Long accountId, @Param("from") Date from, @Param("to") Date to);
}
