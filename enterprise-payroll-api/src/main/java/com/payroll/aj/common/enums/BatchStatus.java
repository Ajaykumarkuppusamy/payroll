package com.payroll.aj.common.enums;

/**
 * Lifecycle states of a payroll batch execution.
 *
 * <p>State transitions: {@code PROCESSING → COMPLETED | FAILED}.<br>
 * Only {@code COMPLETED} and {@code PROCESSING} batches block
 * re-execution for the same company/period (idempotency guard).</p>
 */
public enum BatchStatus {

    DRAFT("Draft"),
    PENDING("Pending"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String displayName;

    BatchStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
