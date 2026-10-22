package com.payroll.aj.payroll.event;

import com.payroll.aj.infrastructure.kafka.KafkaTopicConfig;
import com.payroll.aj.payroll.entity.PayrollRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPayslipGenerationEvent(PayrollRecord record) {
        PayslipGenerationEvent event = new PayslipGenerationEvent(
                record.getId(),
                record.getBatch().getId(),
                record.getEmployee().getId(),
                record.getBatch().getCompany().getId(),
                record.getBatch().getTenant().getId(),
                record.getBatch().getYear(),
                record.getBatch().getMonth()
        );

        // Partition by company ID to ensure ordered processing for a single company if needed
        String key = String.valueOf(record.getBatch().getCompany().getId());

        kafkaTemplate.send(KafkaTopicConfig.TOPIC_PAYSLIP_GENERATION, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Published payslip generation event for recordId: {}", event.recordId());
                    } else {
                        log.error("Failed to publish payslip generation event for recordId: {}", event.recordId(), ex);
                    }
                });
    }
}
