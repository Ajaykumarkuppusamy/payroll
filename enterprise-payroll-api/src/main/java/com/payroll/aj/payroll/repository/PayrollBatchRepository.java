package com.payroll.aj.payroll.repository;

import com.payroll.aj.common.enums.BatchStatus;
import com.payroll.aj.payroll.entity.PayrollBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link PayrollBatch} entities.
 */
@Repository
public interface PayrollBatchRepository extends JpaRepository<PayrollBatch, Long> {

    /**
     * Finds a batch for the exact company/period combination.
     * Used for the idempotency guard before execution.
     */
    Optional<PayrollBatch> findByCompanyIdAndYearAndMonth(
            Long companyId, Integer year, Integer month);

    /**
     * Lists all batches for a company, most recent first.
     * Eagerly loads {@code executedBy} for DTO mapping.
     */
    @Query("""
            SELECT pb FROM PayrollBatch pb
            JOIN FETCH pb.executedBy
            WHERE pb.company.id = :companyId
            ORDER BY pb.year DESC, pb.month DESC
            """)
    List<PayrollBatch> findByCompanyIdWithExecutor(
            @Param("companyId") Long companyId);

    /**
     * Loads a single batch with its operator for the detail endpoint.
     */
    @Query("""
            SELECT pb FROM PayrollBatch pb
            JOIN FETCH pb.executedBy
            WHERE pb.id = :batchId
            """)
    Optional<PayrollBatch> findByIdWithExecutor(@Param("batchId") Long batchId);

    /**
     * Checks whether a non-retriable batch exists for the given
     * company/period. Used as a fast idempotency check.
     */
    boolean existsByCompanyIdAndYearAndMonthAndStatusIn(
            Long companyId, Integer year, Integer month,
            List<BatchStatus> statuses);
}
