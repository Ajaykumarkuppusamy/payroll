package com.payroll.aj.payroll.repository;

import com.payroll.aj.payroll.entity.EmployeeSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for {@link EmployeeSalaryStructure} entities.
 *
 * <p>The {@code findActiveByEmployeeIdAndDate} query uses
 * {@code JOIN FETCH} on the {@code component} relationship to
 * eagerly load the {@link com.payroll.aj.payroll.entity.SalaryComponent}
 * — essential because the
 * {@link com.payroll.aj.payroll.engine.SalaryComponentCalculationService}
 * accesses component fields (code, type, calculationType, formula)
 * outside the original transaction scope.</p>
 */
@Repository
public interface EmployeeSalaryStructureRepository
        extends JpaRepository<EmployeeSalaryStructure, Long> {

    /**
     * Finds all active salary structure assignments for an employee
     * effective on a given date. Components are eagerly fetched.
     *
     * @param employeeId the target employee
     * @param date       the effective date (typically first day of pay month)
     * @return active structures with pre-loaded components
     */
    @Query("""
            SELECT ess FROM EmployeeSalaryStructure ess
            JOIN FETCH ess.component
            WHERE ess.employee.id = :employeeId
              AND ess.isActive = true
              AND ess.effectiveFrom <= :date
              AND (ess.effectiveTo IS NULL OR ess.effectiveTo >= :date)
            """)
    List<EmployeeSalaryStructure> findActiveByEmployeeIdAndDate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);
}
