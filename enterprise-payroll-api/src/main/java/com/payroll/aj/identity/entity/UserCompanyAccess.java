package com.payroll.aj.identity.entity;

import com.payroll.aj.organization.entity.Company;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * RBAC data-boundary mapping: grants an {@link AppUser} access to
 * a specific {@link Company}.
 *
 * <p>When a user with role {@code COMPANY_HR} makes a request,
 * the security layer verifies that the target company exists in
 * this mapping table. Requests outside the granted scope return
 * {@code 403 Forbidden} (spec §3).</p>
 *
 * <p>Maps to table {@code user_company_access}.</p>
 */
@Entity
@Table(name = "user_company_access", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ucoa_user_company",
                columnNames = {"user_id", "company_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserCompanyAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ucoa_user"))
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ucoa_company"))
    private Company company;
}
