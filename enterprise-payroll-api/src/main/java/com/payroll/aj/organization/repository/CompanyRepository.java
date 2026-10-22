package com.payroll.aj.organization.repository;

import com.payroll.aj.organization.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository for {@link Company} entities.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query("SELECT c FROM Company c JOIN FETCH c.country WHERE c.id IN :ids AND c.isActive = true ORDER BY c.id ASC")
    List<Company> findActiveByIdsWithCountry(@Param("ids") Collection<Long> ids);

    @Query("SELECT c FROM Company c JOIN FETCH c.country WHERE c.country.tenant.id = :tenantId AND c.isActive = true ORDER BY c.id ASC")
    List<Company> findByTenantIdWithCountry(@Param("tenantId") Long tenantId);
}

