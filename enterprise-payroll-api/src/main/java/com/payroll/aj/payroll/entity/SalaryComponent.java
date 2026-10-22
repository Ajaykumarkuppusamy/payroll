package com.payroll.aj.payroll.entity;

import com.payroll.aj.common.entity.BaseEntity;
import com.payroll.aj.common.enums.CalculationType;
import com.payroll.aj.common.enums.ComponentType;
import com.payroll.aj.organization.entity.Country;
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

/**
 * Reusable salary component template scoped to a tenant and country.
 *
 * <p>Component codes follow the ARCHITECTURE_SPEC §4 standard:
 * {@code EAR-{COUNTRY}-{CODE}} for earnings and
 * {@code DED-{COUNTRY}-{CODE}} for deductions. The {@code {CODE}}
 * suffix is used as the SpEL formula variable name during evaluation
 * (e.g., {@code #HRA01}, {@code #PF01}).</p>
 *
 * <h3>Calculation types</h3>
 * <ul>
 *   <li>{@code FLAT_AMOUNT} — {@code defaultValue} is the flat monetary
 *       amount</li>
 *   <li>{@code PERCENTAGE_OF_BASE} — {@code defaultValue} is the
 *       percentage (e.g., {@code 40.00} for 40%); the engine divides
 *       by 100</li>
 *   <li>{@code CUSTOM_FORMULA} — {@code formulaExpression} is a SpEL
 *       expression evaluated against a {@code Map<String, BigDecimal>}
 *       context</li>
 * </ul>
 *
 * <p>Maps to table {@code salary_component}.</p>
 */
@Entity
@Table(name = "salary_component", uniqueConstraints = {
        @UniqueConstraint(name = "uq_sc_tenant_code",
                columnNames = {"tenant_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SalaryComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sc_tenant"))
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sc_country"))
    private Country country;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ComponentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false, length = 30)
    private CalculationType calculationType;

    @Column(name = "formula_expression", columnDefinition = "TEXT")
    private String formulaExpression;

    @Column(name = "default_value", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal defaultValue = BigDecimal.ZERO;

    @Column(name = "is_taxable", nullable = false)
    @Builder.Default
    private Boolean isTaxable = true;

    @Column(name = "is_statutory", nullable = false)
    @Builder.Default
    private Boolean isStatutory = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
