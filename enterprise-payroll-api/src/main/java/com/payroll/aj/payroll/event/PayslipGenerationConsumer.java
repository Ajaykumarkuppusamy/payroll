package com.payroll.aj.payroll.event;

import com.payroll.aj.common.enums.PdfStatus;
import com.payroll.aj.infrastructure.kafka.KafkaTopicConfig;
import com.payroll.aj.infrastructure.storage.StorageService;
import com.payroll.aj.payroll.entity.PayrollRecord;
import com.payroll.aj.payroll.entity.PayrollRecordDetail;
import com.payroll.aj.payroll.repository.PayrollRecordDetailRepository;
import com.payroll.aj.payroll.repository.PayrollRecordRepository;
import com.payroll.aj.payroll.service.JasperPdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayslipGenerationConsumer {

    private final PayrollRecordRepository recordRepository;
    private final PayrollRecordDetailRepository detailRepository;
    private final JasperPdfGeneratorService pdfGeneratorService;
    private final StorageService storageService;

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAYSLIP_GENERATION, groupId = "${spring.kafka.consumer.group-id:payroll-pdf-workers}")
    @Transactional
    public void consumePayslipEvent(PayslipGenerationEvent event) {
        log.info("Received payslip generation event for recordId: {}", event.recordId());

        PayrollRecord record = recordRepository.findById(event.recordId()).orElse(null);
        if (record == null) {
            log.warn("Payroll record {} not found. Skipping PDF generation.", event.recordId());
            return;
        }

        try {
            record.setPdfStatus(PdfStatus.GENERATING);
            recordRepository.saveAndFlush(record);

            List<PayrollRecordDetail> details = detailRepository.findByRecordId(record.getId());

            byte[] pdfBytes = pdfGeneratorService.generatePayslipPdf(
                    record,
                    details,
                    record.getEmployee(),
                    record.getBatch().getCompany(),
                    record.getBatch().getTenant()
            );

            String objectPath = String.format("tenants/%d/companies/%d/%d/%02d/%s_payslip.pdf",
                    event.tenantId(),
                    event.companyId(),
                    event.year(),
                    event.month(),
                    record.getEmployee().getEmployeeCode()
            );

            storageService.uploadPdf(objectPath, pdfBytes, "application/pdf");

            record.setPayslipPdfUrl(objectPath);
            record.setPdfStatus(PdfStatus.COMPLETED);
            recordRepository.save(record);
            
            log.info("Successfully generated and uploaded payslip for recordId: {}", event.recordId());

        } catch (Exception e) {
            log.error("Failed to process payslip for recordId: {}", event.recordId(), e);
            record.setPdfStatus(PdfStatus.FAILED);
            recordRepository.save(record);
            throw new RuntimeException("Payslip generation failed", e); // Will be routed to DLT by Kafka retry mechanisms (if configured)
        }
    }
}
