package com.payroll.aj.payroll.repository;

import com.payroll.aj.payroll.entity.SalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SalaryComponent} template entities.
 */
@Repository
public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, Long> {

    List<SalaryComponent> findByCountryIdAndIsActiveTrueOrderByTypeAscNameAsc(Long countryId);

    List<SalaryComponent> findByCountryIdOrderByTypeAscNameAsc(Long countryId);

    boolean existsByTenantIdAndCode(Long tenantId, String code);

    Optional<SalaryComponent> findByIdAndTenantId(Long id, Long tenantId);
}
