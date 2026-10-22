package com.payroll.aj.employee.controller;

import com.payroll.aj.employee.dto.EmployeeResponse;
import com.payroll.aj.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for employee HR Directory operations.
 *
 * <p>All endpoints require a valid JWT (enforced globally by
 * {@link com.payroll.aj.security.config.SecurityConfig}).
 * Company-scoped access is additionally verified at the service layer
 * via {@link com.payroll.aj.security.scope.RequireCompanyAccess} on
 * {@link EmployeeService}.</p>
 *
 * <p>Base path: {@code /api/v1}</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Returns the full employee roster for a company.
     *
     * <p><strong>GET</strong> {@code /api/v1/companies/{companyId}/employees}</p>
     *
     * <p>Includes employees of all employment statuses so the HR
     * Directory can display the complete workforce, not just active staff.
     * Payroll batch execution continues to filter for {@code ACTIVE} only
     * within {@link com.payroll.aj.payroll.service.PayrollExecutionService}.</p>
     *
     * @param companyId the company whose roster to fetch; must be within
     *                  the operator's {@code allowed_company_ids} JWT claim
     * @return 200 OK with the list of {@link EmployeeResponse} records;
     *         403 Forbidden if the operator lacks access to this company
     */
    @GetMapping("/companies/{companyId}/employees")
    public ResponseEntity<List<EmployeeResponse>> listEmployees(
            @PathVariable Long companyId) {

        return ResponseEntity.ok(employeeService.getEmployeesByCompany(companyId));
    }

    /**
     * Creates and onboards a new employee in the company.
     */
    @org.springframework.web.bind.annotation.PostMapping("/companies/{companyId}/employees")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @PathVariable Long companyId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.payroll.aj.employee.dto.CreateEmployeeRequest request) {

        EmployeeResponse created = employeeService.createEmployee(companyId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing employee's details.
     */
    @org.springframework.web.bind.annotation.PutMapping("/companies/{companyId}/employees/{employeeId}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long companyId,
            @PathVariable Long employeeId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.payroll.aj.employee.dto.UpdateEmployeeRequest request) {

        EmployeeResponse updated = employeeService.updateEmployee(companyId, employeeId, request);
        return ResponseEntity.ok(updated);
    }
}

