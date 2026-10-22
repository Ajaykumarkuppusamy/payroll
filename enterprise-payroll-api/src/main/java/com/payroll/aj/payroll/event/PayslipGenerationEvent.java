package com.payroll.aj.payroll.event;

/**
 * Event published when a new payroll record is created, signaling that
 * a PDF payslip needs to be generated asynchronously.
 */
public record PayslipGenerationEvent(
        Long recordId,
        Long batchId,
        Long employeeId,
        Long companyId,
        Long tenantId,
        int year,
        int month
) {
}
