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
 * Legal entity within a {@link Country}.
 *
 * <p>The architecture supports <em>multiple companies per country</em>,
 * enabling conglomerates with several legal entities in the same
 * jurisdiction. Each company is uniquely identified within its country
 * by {@code registration_number}.</p>
 *
 * <p>Maps to table {@code company}.</p>
 *
 * @see Department
 * @see com.payroll.aj.employee.entity.Employee
 */
@Entity
@Table(name = "company", uniqueConstraints = {
        @UniqueConstraint(name = "uq_company_country_reg",
                columnNames = {"country_id", "registration_number"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_company_country"))
    private Country country;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
