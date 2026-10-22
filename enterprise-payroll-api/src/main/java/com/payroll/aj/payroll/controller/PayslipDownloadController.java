package com.payroll.aj.payroll.controller;

import com.payroll.aj.common.enums.UserRole;
import com.payroll.aj.employee.entity.Employee;
import com.payroll.aj.employee.repository.EmployeeRepository;
import com.payroll.aj.infrastructure.storage.StorageService;
import com.payroll.aj.payroll.dto.PresignedUrlResponse;
import com.payroll.aj.payroll.entity.PayrollRecord;
import com.payroll.aj.payroll.repository.PayrollRecordRepository;
import com.payroll.aj.security.userdetails.UserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PayslipDownloadController {

    private final PayrollRecordRepository recordRepository;
    private final EmployeeRepository employeeRepository;
    private final StorageService storageService;

    @GetMapping("/payroll/records/{recordId}/payslip")
    public ResponseEntity<PresignedUrlResponse> getPayslipUrl(
            @PathVariable Long recordId,
            @AuthenticationPrincipal UserPrincipal principal) {

        PayrollRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Payroll record not found"));

        Employee employee = record.getEmployee();

        // Security Check: Is the user authorized to view this payslip?
        boolean isAuthorized = false;

        if (principal.getRole() == UserRole.SUPER_ADMIN) {
            isAuthorized = true;
        } else if (principal.getRole() == UserRole.COMPANY_HR || principal.getRole() == UserRole.COUNTRY_PAYROLL_MGR) {
            if (principal.getCompanyIds().contains(employee.getCompany().getId())) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (record.getPayslipPdfUrl() == null || record.getPdfStatus() != com.payroll.aj.common.enums.PdfStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 if not yet generated
        }

        Duration expiry = Duration.ofMinutes(15);
        String url = storageService.generatePresignedUrl(record.getPayslipPdfUrl(), expiry);

        return ResponseEntity.ok(new PresignedUrlResponse(url, expiry.getSeconds()));
    }
}
