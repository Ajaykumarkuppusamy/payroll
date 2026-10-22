package com.payroll.aj.payroll.entity;

import com.payroll.aj.common.entity.BaseEntity;
import com.payroll.aj.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;

/**
 * Per-employee assignment of a {@link SalaryComponent}.
 *
 * <p>Defines which components are active for an employee and allows
 * overriding the component's default value with a per-employee amount.
 * Date-bounded via {@code effectiveFrom} / {@code effectiveTo} to
 * support salary revisions over time.</p>
 *
 * <h3>Override semantics</h3>
 * <ul>
 *   <li>{@code FLAT_AMOUNT} — {@code overrideAmount} replaces the
 *       component's {@code defaultValue}</li>
 *   <li>{@code PERCENTAGE_OF_BASE} — {@code overrideAmount} replaces
 *       the percentage (e.g., 45.00 instead of the default 40.00)</li>
 *   <li>{@code CUSTOM_FORMULA} — {@code overrideAmount} bypasses
 *       formula evaluation entirely</li>
 * </ul>
 *
 * <p>Maps to table {@code employee_salary_structure}.</p>
 */
@Entity
@Table(name = "employee_salary_structure", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ess_emp_comp_from",
                columnNames = {"employee_id", "component_id", "effective_from"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmployeeSalaryStructure extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ess_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ess_component"))
    private SalaryComponent component;

    @Column(name = "override_amount", precision = 19, scale = 2)
    private BigDecimal overrideAmount;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
