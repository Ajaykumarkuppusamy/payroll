package com.payroll.aj.organization.controller;

import com.payroll.aj.organization.dto.DepartmentResponse;
import com.payroll.aj.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for department endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DepartmentController {

    private final OrganizationService organizationService;

    /**
     * Lists all departments belonging to a company.
     */
    @GetMapping("/companies/{companyId}/departments")
    public ResponseEntity<List<DepartmentResponse>> listDepartments(
            @PathVariable Long companyId) {
        return ResponseEntity.ok(organizationService.getDepartmentsByCompany(companyId));
    }
}
