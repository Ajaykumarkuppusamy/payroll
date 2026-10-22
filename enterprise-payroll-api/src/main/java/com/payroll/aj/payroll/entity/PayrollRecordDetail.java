package com.payroll.aj.payroll.entity;

import com.payroll.aj.common.enums.ComponentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Immutable line-item snapshot of a single salary component within
 * a {@link PayrollRecord}.
 *
 * <p>Fully denormalised — stores the component code, name, type, and
 * calculated amount at the time of the payroll run. No audit columns;
 * this entity is write-once.</p>
 *
 * <p>The FK to {@code payroll_record} uses {@code ON DELETE CASCADE}
 * so detail rows are automatically cleaned up if the parent record
 * is removed during a batch retry.</p>
 *
 * <p>Maps to table {@code payroll_record_detail}.</p>
 */
@Entity
@Table(name = "payroll_record_detail")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PayrollRecordDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_prd_record"))
    private PayrollRecord record;

    @Column(name = "component_code", nullable = false, length = 50)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 255)
    private String componentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 20)
    private ComponentType componentType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
}
