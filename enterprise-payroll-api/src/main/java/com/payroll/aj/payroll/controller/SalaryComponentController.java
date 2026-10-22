package com.payroll.aj.payroll.controller;

import com.payroll.aj.payroll.dto.CreateSalaryComponentRequest;
import com.payroll.aj.payroll.dto.SalaryComponentResponse;
import com.payroll.aj.payroll.service.SalaryComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for salary component template catalog.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SalaryComponentController {

    private final SalaryComponentService salaryComponentService;

    /**
     * Lists all salary components for the company's jurisdiction.
     */
    @GetMapping("/companies/{companyId}/components")
    public ResponseEntity<List<SalaryComponentResponse>> listComponents(
            @PathVariable Long companyId) {
        return ResponseEntity.ok(salaryComponentService.getComponentsByCompany(companyId));
    }

    /**
     * Creates a new salary component template.
     */
    @PostMapping("/companies/{companyId}/components")
    public ResponseEntity<SalaryComponentResponse> createComponent(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateSalaryComponentRequest request) {
        SalaryComponentResponse response = salaryComponentService.createComponent(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
