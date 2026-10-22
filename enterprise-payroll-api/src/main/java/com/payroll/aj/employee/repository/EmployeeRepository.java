package com.payroll.aj.employee.repository;

import com.payroll.aj.common.enums.EmploymentStatus;
import com.payroll.aj.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Employee} entities.
 *
 * <p>The {@code findByCompanyIdAndEmploymentStatus} method is the
 * primary query used during payroll batch execution to fetch all
 * active employees for a target company.</p>
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Finds all employees belonging to a company with the given
     * employment status.
     *
     * @param companyId the target company
     * @param status    the employment status filter (typically ACTIVE)
     * @return employees matching both criteria
     */
    List<Employee> findByCompanyIdAndEmploymentStatus(Long companyId,
                                                      EmploymentStatus status);

    /**
     * Finds all employees belonging to a company regardless of status.
     *
     * <p>Used by the HR Directory endpoint to return a full roster
     * including ON_LEAVE, PROBATION, and TERMINATED employees.</p>
     *
     * <p>JOIN FETCH on {@code department} to avoid N+1 queries when
     * mapping {@code departmentName} in {@link com.payroll.aj.employee.dto.EmployeeResponse}.</p>
     *
     * @param companyId the target company's surrogate key
     * @return all employees for the company, department eagerly loaded
     */
    @Query("SELECT e FROM Employee e JOIN FETCH e.department WHERE e.company.id = :companyId ORDER BY e.id ASC")
    List<Employee> findByCompanyIdWithDepartment(@Param("companyId") Long companyId);

    Optional<Employee> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndEmploymentStatus(Long companyId, EmploymentStatus status);

    @Query("SELECT COALESCE(SUM(e.baseSalary), 0) FROM Employee e WHERE e.company.id = :companyId AND e.employmentStatus = :status")
    java.math.BigDecimal sumBaseSalaryByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") EmploymentStatus status);

    @Query("SELECT e.department.name, COUNT(e) FROM Employee e WHERE e.company.id = :companyId GROUP BY e.department.name ORDER BY COUNT(e) DESC")
    List<Object[]> countEmployeesByDepartment(@Param("companyId") Long companyId);
}
