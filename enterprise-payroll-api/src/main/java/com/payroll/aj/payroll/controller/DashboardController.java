package com.payroll.aj.payroll.controller;

import com.payroll.aj.payroll.dto.DashboardResponse;
import com.payroll.aj.payroll.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for company executive dashboard analytics.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Returns consolidated dashboard analytics for the company.
     */
    @GetMapping("/companies/{companyId}/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @PathVariable Long companyId) {
        return ResponseEntity.ok(dashboardService.getDashboardData(companyId));
    }
}
