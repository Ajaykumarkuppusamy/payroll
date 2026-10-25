package com.payroll.aj.payroll.service;

import com.payroll.aj.employee.entity.Employee;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.payroll.entity.PayrollRecord;
import com.payroll.aj.payroll.entity.PayrollRecordDetail;
import com.payroll.aj.tenant.entity.Tenant;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JasperPdfGeneratorService {

    private JasperReport compiledPayslipReport;
    private JasperReport compiledPayrollSummaryReport;
    private JasperReport compiledHeadcountCostReport;

    @PostConstruct
    public void init() {
        this.compiledPayslipReport = compileTemplate("/reports/payslip_template.jrxml");
        this.compiledPayrollSummaryReport = compileTemplate("/reports/payroll_summary_template.jrxml");
        this.compiledHeadcountCostReport = compileTemplate("/reports/headcount_cost_template.jrxml");
        log.info("All JasperReports templates compiled successfully.");
    }

    private JasperReport compileTemplate(String resourcePath) {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Jasper template not found at " + resourcePath);
            }
            return JasperCompileManager.compileReport(stream);
        } catch (Exception e) {
            log.error("Failed to compile Jasper template: {}", resourcePath, e);
            throw new RuntimeException("Failed to compile Jasper template: " + resourcePath, e);
        }
    }

    /**
     * Generates a PDF payslip for a given payroll record and its details (used by Kafka consumer & direct).
     */
    public byte[] generatePayslipPdf(PayrollRecord record, List<PayrollRecordDetail> details,
                                     Employee employee, Company company, Tenant tenant) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantName", tenant != null ? tenant.getName() : "Enterprise Payroll");
        parameters.put("companyName", company != null ? company.getName() : "Company");
        parameters.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
        parameters.put("employeeCode", employee.getEmployeeCode());
        parameters.put("department", employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A");
        
        String payPeriod = String.format("%02d/%d", record.getBatch().getMonth(), record.getBatch().getYear());
        parameters.put("payPeriod", payPeriod);
        
        parameters.put("grossPay", record.getGrossEarnings().toString());
        parameters.put("totalDeductions", record.getTotalDeductions().toString());
        parameters.put("netPay", record.getNetSalary().toString());

        List<PayslipComponentBean> beans = details.stream()
                .map(d -> new PayslipComponentBean(d.getComponentName(), d.getAmount(), d.getComponentType().name()))
                .collect(Collectors.toList());

        return generatePdf(compiledPayslipReport, parameters, beans);
    }

    /**
     * Generates a PDF payslip from Staged Data parameters and line items.
     */
    public byte[] generatePayslipPdfFromStaging(Map<String, Object> parameters, List<PayslipComponentBean> items) {
        return generatePdf(compiledPayslipReport, parameters, items);
    }

    /**
     * Generates a PDF Payroll Register Summary from Staged Data.
     */
    public byte[] generatePayrollSummaryPdf(Map<String, Object> parameters, List<PayrollSummaryBean> items) {
        return generatePdf(compiledPayrollSummaryReport, parameters, items);
    }

    /**
     * Generates a PDF Headcount Cost Report from Staged Data.
     */
    public byte[] generateHeadcountCostPdf(Map<String, Object> parameters, List<HeadcountCostBean> items) {
        return generatePdf(compiledHeadcountCostReport, parameters, items);
    }

    private <T> byte[] generatePdf(JasperReport report, Map<String, Object> parameters, List<T> items) {
        try {
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(items);
            JasperPrint print = JasperFillManager.fillReport(report, parameters, dataSource);
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            log.error("Failed to export Jasper report to PDF", e);
            throw new RuntimeException("Error during PDF report generation", e);
        }
    }

    // ── DTOs for Jasper Datasets ──────────────────────────────────────────────

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PayslipComponentBean {
        private String componentName;
        private BigDecimal amount;
        private String type;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PayrollSummaryBean {
        private String employeeCode;
        private String employeeName;
        private String department;
        private String panNumber;
        private String bankAccount;
        private BigDecimal grossPay;
        private BigDecimal deductions;
        private BigDecimal netPay;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HeadcountCostBean {
        private String departmentName;
        private String departmentCode;
        private Integer headcount;
        private BigDecimal totalSalary;
        private BigDecimal avgSalary;
    }
}
