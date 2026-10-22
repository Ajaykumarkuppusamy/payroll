package com.payroll.aj.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_PAYSLIP_GENERATION = "payroll.payslip.generation";
    public static final String TOPIC_PAYSLIP_GENERATION_DLT = "payroll.payslip.generation.DLT";

    @Bean
    public NewTopic payslipGenerationTopic() {
        return TopicBuilder.name(TOPIC_PAYSLIP_GENERATION)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic payslipGenerationDltTopic() {
        return TopicBuilder.name(TOPIC_PAYSLIP_GENERATION_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
