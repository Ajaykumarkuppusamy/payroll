package com.payroll.aj.employee.entity;

import com.payroll.aj.common.entity.BaseEntity;
import com.payroll.aj.common.enums.EmploymentStatus;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.organization.entity.Department;
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
import java.time.LocalDate;

/**
 * The payroll subject — an individual whose salary is calculated.
 *
 * <p><strong>Employee ≠ User.</strong> Per the architecture spec §3,
 * employees are payroll targets; they do not log into the system.
 * System operators are modeled by
 * {@link com.payroll.aj.identity.entity.AppUser}.</p>
 *
 * <h3>Financial Precision</h3>
 * <p>{@code baseSalary} is stored as {@code NUMERIC(19,2)} and all
 * downstream salary-component calculations must use
 * {@link java.math.RoundingMode#HALF_UP} as mandated by the spec.</p>
 *
 * <h3>Payroll Eligibility</h3>
 * <p>Only employees with {@code employmentStatus == ACTIVE} are
 * included in batch payroll runs (spec §5, step 3).</p>
 *
 * <p>Maps to table {@code employee}.</p>
 */
@Entity
@Table(name = "employee", uniqueConstraints = {
        @UniqueConstraint(name = "uq_employee_company_code",
                columnNames = {"company_id", "employee_code"}),
        @UniqueConstraint(name = "uq_employee_email",
                columnNames = {"email"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_department"))
    private Department department;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "base_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 255)
    private String bankName;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;
}
