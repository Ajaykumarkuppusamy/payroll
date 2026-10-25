package com.payroll.aj.report.dto;

import java.util.List;

public record ReportCatalogItem(
        String code,
        String title,
        String description,
        String category,
        List<String> requiredParameters,
        List<String> supportedFormats
) {}
