package com.payroll.aj.report.repository;

import com.payroll.aj.report.entity.ReportStagingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportStagingDataRepository extends JpaRepository<ReportStagingData, Long> {

    List<ReportStagingData> findByJobIdOrderByIdAsc(UUID jobId);

    List<ReportStagingData> findByJobIdAndSectionOrderByIdAsc(UUID jobId, String section);

    @Modifying
    @Query(value = "CALL sp_populate_payslip_staging(:jobId, :batchId, :employeeId)", nativeQuery = true)
    void populatePayslipStaging(@Param("jobId") UUID jobId,
                                @Param("batchId") Long batchId,
                                @Param("employeeId") Long employeeId);

    @Modifying
    @Query(value = "CALL sp_populate_payroll_summary_staging(:jobId, :batchId)", nativeQuery = true)
    void populatePayrollSummaryStaging(@Param("jobId") UUID jobId,
                                       @Param("batchId") Long batchId);

    @Modifying
    @Query(value = "CALL sp_populate_headcount_cost_staging(:jobId, :companyId)", nativeQuery = true)
    void populateHeadcountCostStaging(@Param("jobId") UUID jobId,
                                      @Param("companyId") Long companyId);

    @Modifying
    @Query("DELETE FROM ReportStagingData r WHERE r.jobId = :jobId")
    void deleteByJobId(@Param("jobId") UUID jobId);
}
