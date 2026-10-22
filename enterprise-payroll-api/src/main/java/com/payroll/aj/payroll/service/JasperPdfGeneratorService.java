package com.payroll.aj.payroll.service;

import com.payroll.aj.employee.entity.Employee;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.payroll.entity.PayrollRecord;
import com.payroll.aj.payroll.entity.PayrollRecordDetail;
import com.payroll.aj.tenant.entity.Tenant;
import jakarta.annotation.PostConstruct;
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

    private JasperReport compiledReport;

    @PostConstruct
    public void init() {
        try (InputStream stream = getClass().getResourceAsStream("/reports/payslip_template.jrxml")) {
            if (stream == null) {
                throw new IllegalStateException("Payslip template not found at /reports/payslip_template.jrxml");
            }
            this.compiledReport = JasperCompileManager.compileReport(stream);
            log.info("JasperReports template compiled successfully.");
        } catch (Exception e) {
            log.error("Failed to compile JasperReports template on startup", e);
            throw new RuntimeException("Failed to compile JasperReports template", e);
        }
    }

    /**
     * Generates a PDF payslip for a given payroll record and its details.
     */
    public byte[] generatePayslipPdf(PayrollRecord record, List<PayrollRecordDetail> details,
                                     Employee employee, Company company, Tenant tenant) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("tenantName", tenant.getName());
            parameters.put("companyName", company.getName());
            parameters.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
            parameters.put("employeeCode", employee.getEmployeeCode());
            parameters.put("department", employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A");
            
            String payPeriod = String.format("%02d/%d", record.getBatch().getMonth(), record.getBatch().getYear());
            parameters.put("payPeriod", payPeriod);
            
            parameters.put("grossPay", record.getGrossEarnings().toString());
            parameters.put("totalDeductions", record.getTotalDeductions().toString());
            parameters.put("netPay", record.getNetSalary().toString());

            // Map details to beans for JasperReports
            List<PayslipComponentBean> beans = details.stream()
                    .map(d -> new PayslipComponentBean(d.getComponentName(), d.getAmount(), d.getComponentType().name()))
                    .collect(Collectors.toList());

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(beans);
            JasperPrint print = JasperFillManager.fillReport(compiledReport, parameters, dataSource);

            return JasperExportManager.exportReportToPdf(print);

        } catch (JRException e) {
            log.error("Failed to generate PDF for record: {}", record.getId(), e);
            throw new RuntimeException("Error during PDF generation", e);
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PayslipComponentBean {
        private String componentName;
        private BigDecimal amount;
        private String type;
    }
}
