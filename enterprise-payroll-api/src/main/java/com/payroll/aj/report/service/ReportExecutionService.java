package com.payroll.aj.report.service;

import com.payroll.aj.infrastructure.storage.StorageService;
import com.payroll.aj.payroll.service.JasperPdfGeneratorService;
import com.payroll.aj.report.dto.ExecuteReportRequest;
import com.payroll.aj.report.dto.ExecuteReportResponse;
import com.payroll.aj.report.dto.ReportCatalogItem;
import com.payroll.aj.report.entity.ReportStagingData;
import com.payroll.aj.report.repository.ReportStagingDataRepository;
import com.payroll.aj.security.userdetails.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExecutionService {

    private final ReportStagingDataRepository stagingRepository;
    private final JasperPdfGeneratorService jasperService;
    private final StorageService storageService;

    public List<ReportCatalogItem> getCatalog() {
        return List.of(
                new ReportCatalogItem(
                        "EMP_PAYSLIP",
                        "Employee Monthly Payslip",
                        "Individual statutory monthly payslip with gross earnings, line item breakdowns, and deductions snapshot.",
                        "Payroll",
                        List.of("companyId", "batchId", "employeeId"),
                        List.of("PDF")
                ),
                new ReportCatalogItem(
                        "PAYROLL_SUMMARY",
                        "Company Payroll Master Register",
                        "Comprehensive company-wide payroll ledger per pay period detailing all employees, bank accounts, PAN, and totals.",
                        "Audit & Financials",
                        List.of("companyId", "batchId"),
                        List.of("PDF")
                ),
                new ReportCatalogItem(
                        "HEADCOUNT_COST",
                        "Department Cost & Headcount Report",
                        "Departmental headcount allocation, total base salary cost, and average salary per employee.",
                        "Executive Analytics",
                        List.of("companyId"),
                        List.of("PDF")
                )
        );
    }

    @Transactional
    public ExecuteReportResponse executeReport(ExecuteReportRequest request, UserPrincipal principal) {
        UUID jobId = UUID.randomUUID();
        log.info("Starting report execution for code: {}, jobId: {}", request.reportCode(), jobId);

        byte[] pdfBytes;

        switch (request.reportCode().toUpperCase()) {
            case "EMP_PAYSLIP" -> {
                if (request.batchId() == null || request.employeeId() == null) {
                    throw new IllegalArgumentException("batchId and employeeId are required for EMP_PAYSLIP report");
                }
                stagingRepository.populatePayslipStaging(jobId, request.batchId(), request.employeeId());
                List<ReportStagingData> staged = stagingRepository.findByJobIdOrderByIdAsc(jobId);
                if (staged.isEmpty()) {
                    throw new IllegalStateException("No report data staged for payslip");
                }

                ReportStagingData header = staged.stream()
                        .filter(r -> "HEADER".equals(r.getSection()))
                        .findFirst()
                        .orElse(staged.get(0));

                Map<String, Object> params = new HashMap<>();
                params.put("employeeCode", header.getCol1());
                params.put("employeeName", header.getCol2());
                params.put("department", header.getCol3());
                params.put("payPeriod", header.getCol4());
                params.put("companyName", header.getCol5());
                params.put("tenantName", "Enterprise Payroll");
                params.put("grossPay", String.format("%.2f", header.getNum1()));
                params.put("totalDeductions", String.format("%.2f", header.getNum2()));
                params.put("netPay", String.format("%.2f", header.getNum3()));

                List<JasperPdfGeneratorService.PayslipComponentBean> items = staged.stream()
                        .filter(r -> !"HEADER".equals(r.getSection()))
                        .map(r -> new JasperPdfGeneratorService.PayslipComponentBean(
                                r.getCol1(),
                                r.getNum1() != null ? r.getNum1() : BigDecimal.ZERO,
                                r.getSection()
                        ))
                        .toList();

                pdfBytes = jasperService.generatePayslipPdfFromStaging(params, items);
            }
            case "PAYROLL_SUMMARY" -> {
                if (request.batchId() == null) {
                    throw new IllegalArgumentException("batchId is required for PAYROLL_SUMMARY report");
                }
                stagingRepository.populatePayrollSummaryStaging(jobId, request.batchId());
                List<ReportStagingData> staged = stagingRepository.findByJobIdOrderByIdAsc(jobId);

                ReportStagingData header = staged.stream()
                        .filter(r -> "HEADER".equals(r.getSection()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Payroll summary header not found in staging"));

                Map<String, Object> params = new HashMap<>();
                params.put("companyName", header.getCol1());
                params.put("batchStatus", header.getCol3());
                params.put("payPeriod", header.getCol4());
                params.put("executedBy", header.getCol5());
                params.put("totalGross", String.format("%.2f", header.getNum1() != null ? header.getNum1() : BigDecimal.ZERO));
                params.put("totalDeductions", String.format("%.2f", header.getNum2() != null ? header.getNum2() : BigDecimal.ZERO));
                params.put("totalNet", String.format("%.2f", header.getNum3() != null ? header.getNum3() : BigDecimal.ZERO));

                List<JasperPdfGeneratorService.PayrollSummaryBean> items = staged.stream()
                        .filter(r -> "ROW".equals(r.getSection()))
                        .map(r -> new JasperPdfGeneratorService.PayrollSummaryBean(
                                r.getCol1(),
                                r.getCol2(),
                                r.getCol3(),
                                r.getCol4(),
                                r.getCol5(),
                                r.getNum1() != null ? r.getNum1() : BigDecimal.ZERO,
                                r.getNum2() != null ? r.getNum2() : BigDecimal.ZERO,
                                r.getNum3() != null ? r.getNum3() : BigDecimal.ZERO
                        ))
                        .toList();

                pdfBytes = jasperService.generatePayrollSummaryPdf(params, items);
            }
            case "HEADCOUNT_COST" -> {
                if (request.companyId() == null) {
                    throw new IllegalArgumentException("companyId is required for HEADCOUNT_COST report");
                }
                stagingRepository.populateHeadcountCostStaging(jobId, request.companyId());
                List<ReportStagingData> staged = stagingRepository.findByJobIdOrderByIdAsc(jobId);

                String companyName = staged.isEmpty() ? "Company" : staged.get(0).getCol3();
                int totalHeadcount = 0;
                BigDecimal totalCost = BigDecimal.ZERO;

                List<JasperPdfGeneratorService.HeadcountCostBean> items = new ArrayList<>();
                for (ReportStagingData r : staged) {
                    int hc = 0;
                    try {
                        hc = Integer.parseInt(r.getCol4());
                    } catch (Exception ignored) {}
                    totalHeadcount += hc;
                    BigDecimal cost = r.getNum1() != null ? r.getNum1() : BigDecimal.ZERO;
                    totalCost = totalCost.add(cost);

                    items.add(new JasperPdfGeneratorService.HeadcountCostBean(
                            r.getCol1(),
                            r.getCol2(),
                            hc,
                            cost,
                            r.getNum2() != null ? r.getNum2() : BigDecimal.ZERO
                    ));
                }

                Map<String, Object> params = new HashMap<>();
                params.put("companyName", companyName);
                params.put("totalHeadcount", String.valueOf(totalHeadcount));
                params.put("totalCost", String.format("%.2f", totalCost));

                pdfBytes = jasperService.generateHeadcountCostPdf(params, items);
            }
            default -> throw new IllegalArgumentException("Unsupported reportCode: " + request.reportCode());
        }

        // Upload generated PDF to MinIO
        String objectPath = "reports/" + jobId + ".pdf";
        storageService.uploadPdf(objectPath, pdfBytes, "application/pdf");
        log.info("Report PDF successfully stored in MinIO: {}", objectPath);

        return new ExecuteReportResponse(
                jobId,
                request.reportCode(),
                "COMPLETED",
                "/api/v1/reports/preview/" + jobId,
                "/api/v1/reports/download/" + jobId,
                "Report generated successfully."
        );
    }

    public byte[] getReportPdf(UUID jobId) {
        String objectPath = "reports/" + jobId + ".pdf";
        return storageService.downloadPdf(objectPath);
    }
}
