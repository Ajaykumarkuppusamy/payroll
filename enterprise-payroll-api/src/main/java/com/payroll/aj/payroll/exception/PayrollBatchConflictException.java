package com.payroll.aj.payroll.exception;

import com.payroll.aj.common.enums.BatchStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a payroll batch execution is attempted for a
 * company/period that already has a {@code COMPLETED} or
 * {@code PROCESSING} batch.
 *
 * <p>Maps to HTTP {@code 409 Conflict}.</p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class PayrollBatchConflictException extends RuntimeException {

    private final Long companyId;
    private final int year;
    private final int month;
    private final BatchStatus existingStatus;

    public PayrollBatchConflictException(Long companyId, int year, int month,
                                         BatchStatus existingStatus) {
        super("Payroll batch already exists for company %d, period %d/%02d with status %s. "
                .formatted(companyId, year, month, existingStatus.name())
                + "A completed or in-progress batch cannot be re-executed.");
        this.companyId = companyId;
        this.year = year;
        this.month = month;
        this.existingStatus = existingStatus;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public BatchStatus getExistingStatus() {
        return existingStatus;
    }
}
