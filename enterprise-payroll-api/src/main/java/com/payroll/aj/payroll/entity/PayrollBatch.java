package com.payroll.aj.payroll.entity;

import com.payroll.aj.common.entity.BaseEntity;
import com.payroll.aj.common.enums.BatchStatus;
import com.payroll.aj.identity.entity.AppUser;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single payroll execution batch for one company
 * and pay period (year + month).
 *
 * <p>The unique constraint {@code (company_id, year, month)} enforces
 * strict idempotency — at most one batch can exist per company per
 * pay period. Re-execution is blocked when the status is
 * {@code COMPLETED} or {@code PROCESSING}.</p>
 *
 * <p>Aggregated totals ({@code totalGrossPay}, {@code totalDeductions},
 * {@code totalNetPay}) are computed from individual
 * {@link PayrollRecord} entries using
 * {@link java.math.RoundingMode#HALF_UP}.</p>
 *
 * <p>Maps to table {@code payroll_batch}.</p>
 */
@Entity
@Table(name = "payroll_batch", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pb_company_year_month",
                columnNames = {"company_id", "year", "month"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PayrollBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pb_tenant"))
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pb_company"))
    private Company company;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BatchStatus status = BatchStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executed_by", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pb_executed_by"))
    private AppUser executedBy;

    @Column(name = "total_employees", nullable = false)
    @Builder.Default
    private Integer totalEmployees = 0;

    @Column(name = "total_gross_pay", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalGrossPay = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "total_net_pay", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalNetPay = BigDecimal.ZERO;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;
}
