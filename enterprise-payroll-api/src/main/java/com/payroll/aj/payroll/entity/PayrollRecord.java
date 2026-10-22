package com.payroll.aj.payroll.entity;

import com.payroll.aj.common.enums.PdfStatus;
import com.payroll.aj.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
 * Immutable payroll snapshot for a single employee within a batch.
 *
 * <p>Does <strong>not</strong> extend {@code BaseEntity} because this
 * entity has only {@code created_at} (no {@code updated_at}) per the
 * ledger's immutability design. The only mutable fields are
 * {@code payslipPdfUrl} and {@code pdfStatus}, updated asynchronously
 * by the Kafka PDF-generation consumer.</p>
 *
 * <p>All monetary values are denormalised snapshots taken at
 * calculation time — changes to the employee's base salary or
 * components after the run do not affect existing records.</p>
 *
 * <p>Maps to table {@code payroll_record}.</p>
 */
@Entity
@Table(name = "payroll_record", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pr_batch_employee",
                columnNames = {"batch_id", "employee_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pr_batch"))
    private PayrollBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pr_employee"))
    private Employee employee;

    @Column(name = "base_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "gross_earnings", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossEarnings;

    @Column(name = "total_deductions", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "payslip_pdf_url", length = 500)
    private String payslipPdfUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "pdf_status", nullable = false, length = 20)
    @Builder.Default
    private PdfStatus pdfStatus = PdfStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
