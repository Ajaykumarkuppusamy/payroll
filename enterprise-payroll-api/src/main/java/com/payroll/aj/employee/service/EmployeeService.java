package com.payroll.aj.employee.service;

import com.payroll.aj.common.enums.EmploymentStatus;
import com.payroll.aj.employee.dto.CreateEmployeeRequest;
import com.payroll.aj.employee.dto.EmployeeResponse;
import com.payroll.aj.employee.dto.UpdateEmployeeRequest;
import com.payroll.aj.employee.entity.Employee;
import com.payroll.aj.employee.repository.EmployeeRepository;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.organization.entity.Department;
import com.payroll.aj.organization.repository.CompanyRepository;
import com.payroll.aj.organization.repository.DepartmentRepository;
import com.payroll.aj.payroll.entity.EmployeeSalaryStructure;
import com.payroll.aj.payroll.entity.SalaryComponent;
import com.payroll.aj.payroll.repository.EmployeeSalaryStructureRepository;
import com.payroll.aj.payroll.repository.SalaryComponentRepository;
import com.payroll.aj.security.scope.RequireCompanyAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Application service for HR Directory operations.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final EmployeeSalaryStructureRepository employeeSalaryStructureRepository;

    /**
     * Returns all employees for the given company, regardless of employment status.
     */
    @Transactional(readOnly = true)
    @RequireCompanyAccess("companyId")
    public List<EmployeeResponse> getEmployeesByCompany(Long companyId) {
        return employeeRepository.findByCompanyIdWithDepartment(companyId)
                .stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    /**
     * Onboards a new employee into the company and initializes their default salary structure.
     */
    @Transactional
    @RequireCompanyAccess("companyId")
    public EmployeeResponse createEmployee(Long companyId, CreateEmployeeRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company not found with ID: " + companyId));

        if (employeeRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new IllegalArgumentException("Employee with email '" + request.email() + "' already exists");
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new NoSuchElementException("Department not found with ID: " + request.departmentId()));

        if (!department.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Department " + request.departmentId() + " does not belong to company " + companyId);
        }

        long employeeCount = employeeRepository.countByCompanyId(companyId);
        String employeeCode = String.format("EMP%03d", employeeCount + 1);

        Employee employee = Employee.builder()
                .company(company)
                .department(department)
                .employeeCode(employeeCode)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(request.email().trim().toLowerCase())
                .baseSalary(request.baseSalary())
                .bankAccountNumber(request.bankAccountNumber())
                .bankName(request.bankName())
                .bankIfscCode(request.bankIfscCode())
                .taxId(request.taxId())
                .employmentStatus(request.employmentStatus() != null ? request.employmentStatus() : EmploymentStatus.ACTIVE)
                .hireDate(request.hireDate())
                .build();

        Employee saved = employeeRepository.save(employee);

        // Auto-assign active salary components of the country to new employee
        List<SalaryComponent> components = salaryComponentRepository
                .findByCountryIdAndIsActiveTrueOrderByTypeAscNameAsc(company.getCountry().getId());

        for (SalaryComponent comp : components) {
            EmployeeSalaryStructure structure = EmployeeSalaryStructure.builder()
                    .employee(saved)
                    .component(comp)
                    .effectiveFrom(saved.getHireDate())
                    .isActive(true)
                    .build();
            employeeSalaryStructureRepository.save(structure);
        }

        return EmployeeResponse.from(saved);
    }

    /**
     * Updates an existing employee's details.
     */
    @Transactional
    @RequireCompanyAccess("companyId")
    public EmployeeResponse updateEmployee(Long companyId, Long employeeId, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found with ID: " + employeeId + " in company " + companyId));

        if (employeeRepository.existsByEmailAndIdNot(request.email().trim().toLowerCase(), employeeId)) {
            throw new IllegalArgumentException("Employee with email '" + request.email() + "' already exists");
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new NoSuchElementException("Department not found with ID: " + request.departmentId()));

        if (!department.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Department does not belong to company " + companyId);
        }

        employee.setDepartment(department);
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(request.email().trim().toLowerCase());
        employee.setBaseSalary(request.baseSalary());
        employee.setEmploymentStatus(request.employmentStatus());
        employee.setHireDate(request.hireDate());
        employee.setTerminationDate(request.terminationDate());
        employee.setBankAccountNumber(request.bankAccountNumber());
        employee.setBankName(request.bankName());
        employee.setBankIfscCode(request.bankIfscCode());
        employee.setTaxId(request.taxId());

        Employee updated = employeeRepository.save(employee);
        return EmployeeResponse.from(updated);
    }
}
