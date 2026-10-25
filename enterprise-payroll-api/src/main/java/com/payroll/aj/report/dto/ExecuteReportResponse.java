package com.payroll.aj.report.dto;

import java.util.UUID;

public record ExecuteReportResponse(
        UUID jobId,
        String reportCode,
        String status,
        String previewUrl,
        String downloadUrl,
        String message
) {}
