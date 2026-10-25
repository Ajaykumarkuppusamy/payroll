package com.payroll.aj.report.dto;

import jakarta.validation.constraints.NotBlank;

public record ExecuteReportRequest(
        @NotBlank(message = "reportCode is required")
        String reportCode,
        Long companyId,
        Long batchId,
        Long employeeId,
        String format
) {}
