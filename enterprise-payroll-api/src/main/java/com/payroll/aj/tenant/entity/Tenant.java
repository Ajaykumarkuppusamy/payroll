package com.payroll.aj.tenant.entity;

import com.payroll.aj.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Root of the multi-tenant hierarchy.
 *
 * <p>A Tenant represents a SaaS client organization. All downstream
 * entities (Country → Company → Department → Employee) are implicitly
 * scoped to a single Tenant through the relational chain.</p>
 *
 * <p>Maps to table {@code tenant}.</p>
 *
 * @see com.payroll.aj.organization.entity.Country
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
