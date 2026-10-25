package com.payroll.aj.report.controller;

import com.payroll.aj.report.dto.ExecuteReportRequest;
import com.payroll.aj.report.dto.ExecuteReportResponse;
import com.payroll.aj.report.dto.ReportCatalogItem;
import com.payroll.aj.report.service.ReportExecutionService;
import com.payroll.aj.security.userdetails.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportExecutionService reportExecutionService;

    @GetMapping("/catalog")
    public ResponseEntity<List<ReportCatalogItem>> getCatalog() {
        return ResponseEntity.ok(reportExecutionService.getCatalog());
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecuteReportResponse> executeReport(
            @Valid @RequestBody ExecuteReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExecuteReportResponse response = reportExecutionService.executeReport(request, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/preview/{jobId}")
    public ResponseEntity<byte[]> previewReport(@PathVariable UUID jobId) {
        byte[] pdfBytes = reportExecutionService.getReportPdf(jobId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"report_" + jobId + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/download/{jobId}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID jobId) {
        byte[] pdfBytes = reportExecutionService.getReportPdf(jobId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report_" + jobId + ".pdf\"")
                .body(pdfBytes);
    }

    /**
     * Direct payslip PDF download shortcut by batchId and employeeId.
     */
    @GetMapping("/payslip/{batchId}/{employeeId}")
    public ResponseEntity<byte[]> directPayslipDownload(
            @PathVariable Long batchId,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExecuteReportRequest request = new ExecuteReportRequest("EMP_PAYSLIP", null, batchId, employeeId, "PDF");
        ExecuteReportResponse response = reportExecutionService.executeReport(request, principal);
        byte[] pdfBytes = reportExecutionService.getReportPdf(response.jobId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip_" + batchId + "_" + employeeId + ".pdf\"")
                .body(pdfBytes);
    }
}
