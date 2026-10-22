package com.payroll.aj.payroll.repository;

import com.payroll.aj.payroll.entity.PayrollRecordDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link PayrollRecordDetail} entities.
 */
@Repository
public interface PayrollRecordDetailRepository
        extends JpaRepository<PayrollRecordDetail, Long> {

    /**
     * Finds all detail line items for a single payroll record.
     */
    List<PayrollRecordDetail> findByRecordId(Long recordId);

    /**
     * Batch-loads details for multiple records in a single query.
     * Used when mapping a page of records to DTOs.
     */
    @Query("SELECT prd FROM PayrollRecordDetail prd WHERE prd.record.id IN :recordIds")
    List<PayrollRecordDetail> findByRecordIdIn(
            @Param("recordIds") List<Long> recordIds);
}
