package com.payroll.aj.payroll.repository;

import com.payroll.aj.payroll.entity.PayrollRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link PayrollRecord} entities.
 */
@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    /**
     * Paginated records for a batch with eagerly loaded employee data.
     * Separate count query avoids the JOIN FETCH + pagination issue.
     */
    @Query(value = """
            SELECT pr FROM PayrollRecord pr
            JOIN FETCH pr.employee
            WHERE pr.batch.id = :batchId
            """,
            countQuery = """
            SELECT COUNT(pr) FROM PayrollRecord pr
            WHERE pr.batch.id = :batchId
            """)
    Page<PayrollRecord> findByBatchIdWithEmployee(
            @Param("batchId") Long batchId, Pageable pageable);

    /**
     * Bulk-deletes all records for a batch. The database-level
     * {@code ON DELETE CASCADE} on {@code payroll_record_detail}
     * automatically removes the associated detail rows.
     */
    @Modifying
    @Query("DELETE FROM PayrollRecord pr WHERE pr.batch.id = :batchId")
    void deleteAllByBatchId(@Param("batchId") Long batchId);
}
