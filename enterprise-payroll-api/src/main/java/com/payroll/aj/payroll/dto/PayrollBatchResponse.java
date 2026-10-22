package com.payroll.aj.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payroll.aj.payroll.entity.PayrollBatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing batch metadata, status, and totals.
 */
public record PayrollBatchResponse(
        @JsonProperty("batch_id") Long batchId,
        @JsonProperty("company_id") Long companyId,
        int year,
        int month,
        String status,
        @JsonProperty("total_employees") int totalEmployees,
        @JsonProperty("total_gross_pay") BigDecimal totalGrossPay,
        @JsonProperty("total_deductions") BigDecimal totalDeductions,
        @JsonProperty("total_net_pay") BigDecimal totalNetPay,
        @JsonProperty("executed_at") LocalDateTime executedAt,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("executed_by_username") String executedByUsername
) {

    /**
     * Factory method mapping from entity to response DTO.
     *
     * <p>Requires {@code executedBy} and {@code company} to be
     * initialised (via JOIN FETCH or within a transaction).</p>
     */
    public static PayrollBatchResponse from(PayrollBatch batch) {
        return new PayrollBatchResponse(
                batch.getId(),
                batch.getCompany().getId(),
                batch.getYear(),
                batch.getMonth(),
                batch.getStatus().name(),
                batch.getTotalEmployees(),
                batch.getTotalGrossPay(),
                batch.getTotalDeductions(),
                batch.getTotalNetPay(),
                batch.getExecutedAt(),
                batch.getCreatedAt(),
                batch.getExecutedBy().getUsername());
    }
}
