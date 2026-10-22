package com.payroll.aj.organization.controller;

import com.payroll.aj.organization.dto.CompanyResponse;
import com.payroll.aj.organization.service.OrganizationService;
import com.payroll.aj.security.userdetails.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for company metadata and user scope queries.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CompanyController {

    private final OrganizationService organizationService;

    /**
     * Returns all companies the authenticated operator has permission to view.
     */
    @GetMapping("/me/companies")
    public ResponseEntity<List<CompanyResponse>> getMyCompanies(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(organizationService.getMyCompanies(principal));
    }
}
