package com.payroll.aj.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Itemised employee payslip summary within a batch.
 */
public record PayrollRecordResponse(
        @JsonProperty("record_id") Long recordId,
        @JsonProperty("employee_id") Long employeeId,
        @JsonProperty("employee_code") String employeeCode,
        @JsonProperty("employee_name") String employeeName,
        @JsonProperty("base_salary") BigDecimal baseSalary,
        @JsonProperty("gross_earnings") BigDecimal grossEarnings,
        @JsonProperty("total_deductions") BigDecimal totalDeductions,
        @JsonProperty("net_salary") BigDecimal netSalary,
        @JsonProperty("pdf_status") String pdfStatus,
        @JsonProperty("payslip_pdf_url") String payslipPdfUrl,
        List<PayrollRecordDetailResponse> details
) {
}
