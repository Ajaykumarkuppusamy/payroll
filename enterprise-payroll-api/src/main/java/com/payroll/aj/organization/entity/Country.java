package com.payroll.aj.organization.entity;

import com.payroll.aj.common.entity.BaseEntity;
import com.payroll.aj.common.enums.CurrencyCode;
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

/**
 * Jurisdiction node in the tenant hierarchy.
 *
 * <p>Each Country belongs to exactly one {@link Tenant} and defines
 * the ISO 3166-1 alpha-2 code plus the default {@link CurrencyCode}
 * used for salary computations in that jurisdiction.</p>
 *
 * <p>A Tenant may not register the same ISO code twice
 * (enforced by {@code uq_country_tenant_iso}).</p>
 *
 * <p>Maps to table {@code country}.</p>
 *
 * @see Company
 */
@Entity
@Table(name = "country", uniqueConstraints = {
        @UniqueConstraint(name = "uq_country_tenant_iso",
                columnNames = {"tenant_id", "iso_code"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Country extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_country_tenant"))
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(name = "iso_code", nullable = false, length = 2)
    private String isoCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_code", nullable = false, length = 3)
    private CurrencyCode currencyCode;
}
