package com.payroll.aj.payroll.service;

import com.payroll.aj.common.enums.EmploymentStatus;
import com.payroll.aj.employee.dto.EmployeeResponse;
import com.payroll.aj.employee.entity.Employee;
import com.payroll.aj.employee.repository.EmployeeRepository;
import com.payroll.aj.organization.repository.CompanyRepository;
import com.payroll.aj.payroll.dto.DashboardResponse;
import com.payroll.aj.payroll.dto.DepartmentDistribution;
import com.payroll.aj.payroll.dto.PayrollBatchResponse;
import com.payroll.aj.payroll.entity.PayrollBatch;
import com.payroll.aj.payroll.repository.PayrollBatchRepository;
import com.payroll.aj.security.scope.RequireCompanyAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service for aggregating executive dashboard statistics for a company.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollBatchRepository payrollBatchRepository;

    /**
     * Aggregates real-time KPIs and workforce stats for the given company.
     */
    @Transactional(readOnly = true)
    @RequireCompanyAccess("companyId")
    public DashboardResponse getDashboardData(Long companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new NoSuchElementException("Company not found with ID: " + companyId);
        }

        long totalEmployees = employeeRepository.countByCompanyId(companyId);
        long activeEmployees = employeeRepository.countByCompanyIdAndEmploymentStatus(companyId, EmploymentStatus.ACTIVE);
        long onLeaveEmployees = employeeRepository.countByCompanyIdAndEmploymentStatus(companyId, EmploymentStatus.ON_LEAVE);
        long probationEmployees = employeeRepository.countByCompanyIdAndEmploymentStatus(companyId, EmploymentStatus.PROBATION);

        BigDecimal monthlyPayrollEstimate = employeeRepository.sumBaseSalaryByCompanyIdAndStatus(companyId, EmploymentStatus.ACTIVE);
        BigDecimal avgSalary = activeEmployees > 0
                ? monthlyPayrollEstimate.divide(BigDecimal.valueOf(activeEmployees), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<PayrollBatch> batches = payrollBatchRepository.findByCompanyIdWithExecutor(companyId);
        PayrollBatchResponse lastBatch = batches.isEmpty() ? null : PayrollBatchResponse.from(batches.get(0));

        List<DepartmentDistribution> departmentDistribution = employeeRepository.countEmployeesByDepartment(companyId)
                .stream()
                .map(row -> new DepartmentDistribution((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<EmployeeResponse> recentEmployees = employeeRepository.findByCompanyIdWithDepartment(companyId)
                .stream()
                .sorted(Comparator.comparing(Employee::getHireDate).reversed())
                .limit(5)
                .map(EmployeeResponse::from)
                .toList();

        return new DashboardResponse(
                companyId,
                totalEmployees,
                activeEmployees,
                onLeaveEmployees,
                probationEmployees,
                monthlyPayrollEstimate,
                avgSalary,
                lastBatch,
                departmentDistribution,
                recentEmployees
        );
    }
}
