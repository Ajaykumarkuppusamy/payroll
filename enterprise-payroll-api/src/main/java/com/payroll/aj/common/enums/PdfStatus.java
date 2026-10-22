package com.payroll.aj.common.enums;

/**
 * Payslip PDF generation status for a {@code payroll_record}.
 *
 * <p>Updated asynchronously by the Kafka consumer after the
 * JasperReports PDF is generated and uploaded to MinIO.</p>
 */
public enum PdfStatus {

    PENDING("Pending"),
    GENERATING("Generating"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String displayName;

    PdfStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
