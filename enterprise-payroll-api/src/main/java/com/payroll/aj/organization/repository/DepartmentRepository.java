package com.payroll.aj.organization.repository;

import com.payroll.aj.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Department} entities.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByCompanyIdAndIsActiveTrueOrderByNameAsc(Long companyId);

    List<Department> findByCompanyIdOrderByNameAsc(Long companyId);
}
