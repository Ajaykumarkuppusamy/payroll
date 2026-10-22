package com.payroll.aj.tenant.repository;

import com.payroll.aj.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Tenant} entities.
 *
 * <p>Primarily used for {@code getReferenceById()} to obtain lazy
 * proxy references when creating payroll batches (avoids loading
 * the full tenant entity).</p>
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
