package com.payroll.aj.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_staging_data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStagingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "section", nullable = false, length = 50)
    private String section;

    @Column(name = "col1")
    private String col1;

    @Column(name = "col2")
    private String col2;

    @Column(name = "col3")
    private String col3;

    @Column(name = "col4")
    private String col4;

    @Column(name = "col5")
    private String col5;

    @Column(name = "num1", precision = 19, scale = 2)
    private BigDecimal num1;

    @Column(name = "num2", precision = 19, scale = 2)
    private BigDecimal num2;

    @Column(name = "num3", precision = 19, scale = 2)
    private BigDecimal num3;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
