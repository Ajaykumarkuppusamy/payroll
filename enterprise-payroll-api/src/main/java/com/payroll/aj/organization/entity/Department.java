package com.payroll.aj.organization.entity;

import com.payroll.aj.common.entity.BaseEntity;
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

/**
 * Logical organizational unit within a {@link Company}.
 *
 * <p>Departments are used for grouping employees and may serve as
 * a filter during payroll reporting. Each department is uniquely
 * identified within its company by a short {@code code}
 * (e.g., {@code "ENG"}, {@code "FIN"}).</p>
 *
 * <p>Maps to table {@code department}.</p>
 *
 * @see com.payroll.aj.employee.entity.Employee
 */
@Entity
@Table(name = "department", uniqueConstraints = {
        @UniqueConstraint(name = "uq_department_company_code",
                columnNames = {"company_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_department_company"))
    private Company company;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
