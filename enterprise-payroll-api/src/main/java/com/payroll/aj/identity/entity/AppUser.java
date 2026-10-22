package com.payroll.aj.identity.entity;

import com.payroll.aj.common.entity.BaseEntity;
import com.payroll.aj.common.enums.UserRole;
import com.payroll.aj.common.enums.UserStatus;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * System operator account — <strong>not</strong> an Employee.
 *
 * <p>Per the architecture spec §3, Users are people who operate the
 * payroll system (Super Admin, Payroll Manager, HR). They are scoped
 * to a {@link Tenant} and their data access is bounded by
 * {@link UserCountryAccess} and {@link UserCompanyAccess} mappings.</p>
 *
 * <h3>Authentication</h3>
 * <p>{@code passwordHash} stores a BCrypt-encoded credential.
 * Only users with {@code status == ACTIVE} may authenticate.</p>
 *
 * <h3>RBAC</h3>
 * <ul>
 *   <li>{@code SUPER_ADMIN} — full tenant-wide access</li>
 *   <li>{@code COUNTRY_PAYROLL_MGR} — access restricted to
 *       assigned countries</li>
 *   <li>{@code COMPANY_HR} — access restricted to assigned
 *       companies</li>
 * </ul>
 *
 * <p>Maps to table {@code app_user}.</p>
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AppUser extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_app_user_tenant"))
    private Tenant tenant;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
}
