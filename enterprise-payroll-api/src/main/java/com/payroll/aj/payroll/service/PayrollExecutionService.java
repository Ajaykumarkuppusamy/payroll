package com.payroll.aj.payroll.service;

import com.payroll.aj.common.enums.BatchStatus;
import com.payroll.aj.common.enums.ComponentType;
import com.payroll.aj.common.enums.EmploymentStatus;
import com.payroll.aj.employee.entity.Employee;
import com.payroll.aj.employee.repository.EmployeeRepository;
import com.payroll.aj.identity.entity.AppUser;
import com.payroll.aj.identity.repository.AppUserRepository;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.organization.repository.CompanyRepository;
import com.payroll.aj.payroll.dto.PayrollBatchResponse;
import com.payroll.aj.payroll.dto.PayrollRecordDetailResponse;
import com.payroll.aj.payroll.dto.PayrollRecordResponse;
import com.payroll.aj.payroll.engine.ComponentLineItem;
import com.payroll.aj.payroll.engine.PayrollBreakdown;
import com.payroll.aj.payroll.engine.SalaryComponentCalculationService;
import com.payroll.aj.payroll.entity.EmployeeSalaryStructure;
import com.payroll.aj.payroll.entity.PayrollBatch;
import com.payroll.aj.payroll.entity.PayrollRecord;
import com.payroll.aj.payroll.entity.PayrollRecordDetail;
import com.payroll.aj.payroll.exception.PayrollBatchConflictException;
import com.payroll.aj.payroll.event.PayrollEventPublisher;
import com.payroll.aj.payroll.repository.EmployeeSalaryStructureRepository;
import com.payroll.aj.payroll.repository.PayrollBatchRepository;
import com.payroll.aj.payroll.repository.PayrollRecordDetailRepository;
import com.payroll.aj.payroll.repository.PayrollRecordRepository;
import com.payroll.aj.security.scope.RequireCompanyAccess;
import com.payroll.aj.tenant.entity.Tenant;
import com.payroll.aj.tenant.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestrates monthly payroll batch execution.
 *
 * <h3>Execution flow</h3>
 * <ol>
 *   <li>Idempotency guard — reject if a {@code COMPLETED} or
 *       {@code PROCESSING} batch exists for the company/period.
 *       {@code FAILED} or {@code DRAFT} batches are cleaned up
 *       and retried.</li>
 *   <li>Fetch all {@code ACTIVE} employees for the company.</li>
 *   <li>For each employee, load active salary structures and invoke
 *       {@link SalaryComponentCalculationService#calculate} to
 *       produce a {@link PayrollBreakdown}.</li>
 *   <li>Persist immutable {@link PayrollRecord} and
 *       {@link PayrollRecordDetail} snapshots.</li>
 *   <li>Aggregate totals and mark batch as {@code COMPLETED}.</li>
 * </ol>
 *
 * <p>The entire execution runs within a single
 * {@link Transactional} boundary — any unhandled exception
 * triggers a full rollback.</p>
 */
@Service
@RequiredArgsConstructor
public class PayrollExecutionService {

    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final PayrollBatchRepository batchRepository;
    private final PayrollRecordRepository recordRepository;
    private final PayrollRecordDetailRepository recordDetailRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryStructureRepository salaryStructureRepository;
    private final CompanyRepository companyRepository;
    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final SalaryComponentCalculationService calculationService;
    private final PayrollEventPublisher eventPublisher;

    // ── Execution ────────────────────────────────────────────────

    /**
     * Executes a payroll run for one company and pay period.
     *
     * @param companyId        the target company
     * @param year             the pay-period year
     * @param month            the pay-period month (1–12)
     * @param executedByUserId the operator's user ID
     * @param tenantId         the operator's tenant
     * @return the completed batch response with totals
     * @throws PayrollBatchConflictException if a COMPLETED or
     *         PROCESSING batch already exists
     * @throws EntityNotFoundException if the company does not exist
     */
    @Transactional
    @RequireCompanyAccess("companyId")
    public PayrollBatchResponse executePayroll(Long companyId, int year, int month,
                                               Long executedByUserId, Long tenantId) {
        // 1. Verify company exists
        if (!companyRepository.existsById(companyId)) {
            throw new EntityNotFoundException("Company not found: " + companyId);
        }

        // 2. Idempotency guard
        Optional<PayrollBatch> existing =
                batchRepository.findByCompanyIdAndYearAndMonth(companyId, year, month);

        if (existing.isPresent()) {
            PayrollBatch oldBatch = existing.get();
            BatchStatus oldStatus = oldBatch.getStatus();

            if (oldStatus == BatchStatus.COMPLETED
                    || oldStatus == BatchStatus.PROCESSING) {
                throw new PayrollBatchConflictException(
                        companyId, year, month, oldStatus);
            }

            // DRAFT / PENDING / FAILED — clean up and retry
            recordRepository.deleteAllByBatchId(oldBatch.getId());
            batchRepository.delete(oldBatch);
            batchRepository.flush();
        }

        // 3. Create batch
        Tenant tenantRef = tenantRepository.getReferenceById(tenantId);
        Company companyRef = companyRepository.getReferenceById(companyId);
        AppUser executorRef = appUserRepository.getReferenceById(executedByUserId);

        PayrollBatch batch = PayrollBatch.builder()
                .tenant(tenantRef)
                .company(companyRef)
                .year(year)
                .month(month)
                .status(BatchStatus.PROCESSING)
                .executedBy(executorRef)
                .build();
        batch = batchRepository.save(batch);

        // 4. Fetch active employees
        List<Employee> employees = employeeRepository
                .findByCompanyIdAndEmploymentStatus(companyId, EmploymentStatus.ACTIVE);

        // 5. Calculate payroll for each employee
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        LocalDate payDate = LocalDate.of(year, month, 1);
        List<PayrollRecord> generatedRecords = new ArrayList<>();

        for (Employee employee : employees) {
            List<EmployeeSalaryStructure> structures =
                    salaryStructureRepository.findActiveByEmployeeIdAndDate(
                            employee.getId(), payDate);

            PayrollBreakdown breakdown =
                    calculationService.calculate(employee.getBaseSalary(), structures);

            // 5a. Persist record snapshot
            PayrollRecord record = PayrollRecord.builder()
                    .batch(batch)
                    .employee(employee)
                    .baseSalary(employee.getBaseSalary())
                    .grossEarnings(breakdown.grossPay())
                    .totalDeductions(breakdown.totalDeductions())
                    .netSalary(breakdown.netPay())
                    .build();
            record = recordRepository.save(record);

            // 5b. Persist detail line items
            List<PayrollRecordDetail> details = new ArrayList<>();
            for (ComponentLineItem item : breakdown.earnings()) {
                details.add(buildDetail(record, item, ComponentType.EARNING));
            }
            for (ComponentLineItem item : breakdown.deductions()) {
                details.add(buildDetail(record, item, ComponentType.DEDUCTION));
            }
            recordDetailRepository.saveAll(details);

            // 5c. Accumulate totals
            totalGross = totalGross.add(breakdown.grossPay());
            totalDeductions = totalDeductions.add(breakdown.totalDeductions());
            totalNet = totalNet.add(breakdown.netPay());

            generatedRecords.add(record);
        }

        // 6. Finalise batch
        batch.setTotalEmployees(employees.size());
        batch.setTotalGrossPay(totalGross.setScale(MONETARY_SCALE, ROUNDING));
        batch.setTotalDeductions(totalDeductions.setScale(MONETARY_SCALE, ROUNDING));
        batch.setTotalNetPay(totalNet.setScale(MONETARY_SCALE, ROUNDING));
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setExecutedAt(LocalDateTime.now());
        batchRepository.save(batch);

        // 7. Publish events for PDF generation
        for (PayrollRecord rec : generatedRecords) {
            eventPublisher.publishPayslipGenerationEvent(rec);
        }

        return PayrollBatchResponse.from(batch);
    }

    // ── Query endpoints ──────────────────────────────────────────

    /**
     * Lists all payroll batches for a company (most recent first).
     */
    @Transactional(readOnly = true)
    @RequireCompanyAccess("companyId")
    public List<PayrollBatchResponse> getBatchesByCompanyId(Long companyId) {
        return batchRepository.findByCompanyIdWithExecutor(companyId)
                .stream()
                .map(PayrollBatchResponse::from)
                .toList();
    }

    /**
     * Retrieves a single batch with full metadata.
     */
    @Transactional(readOnly = true)
    public PayrollBatchResponse getBatchById(Long batchId) {
        PayrollBatch batch = batchRepository.findByIdWithExecutor(batchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payroll batch not found: " + batchId));
        return PayrollBatchResponse.from(batch);
    }

    /**
     * Retrieves paginated employee records for a batch with
     * their component detail breakdowns.
     */
    @Transactional(readOnly = true)
    public Page<PayrollRecordResponse> getRecordsByBatchId(
            Long batchId, Pageable pageable) {

        Page<PayrollRecord> records =
                recordRepository.findByBatchIdWithEmployee(batchId, pageable);

        // Batch-load details for all records on this page
        List<Long> recordIds = records.getContent().stream()
                .map(PayrollRecord::getId)
                .toList();

        Map<Long, List<PayrollRecordDetail>> detailsByRecordId =
                recordDetailRepository.findByRecordIdIn(recordIds).stream()
                        .collect(Collectors.groupingBy(
                                d -> d.getRecord().getId()));

        return records.map(record -> toRecordResponse(record,
                detailsByRecordId.getOrDefault(record.getId(), List.of())));
    }

    // ── Mapping helpers ──────────────────────────────────────────

    private PayrollRecordDetail buildDetail(PayrollRecord record,
                                            ComponentLineItem item,
                                            ComponentType type) {
        return PayrollRecordDetail.builder()
                .record(record)
                .componentCode(item.componentCode())
                .componentName(item.componentName())
                .componentType(type)
                .amount(item.amount())
                .build();
    }

    private PayrollRecordResponse toRecordResponse(
            PayrollRecord record, List<PayrollRecordDetail> details) {
        Employee emp = record.getEmployee();

        List<PayrollRecordDetailResponse> detailDtos = details.stream()
                .map(d -> new PayrollRecordDetailResponse(
                        d.getComponentCode(),
                        d.getComponentName(),
                        d.getComponentType().name(),
                        d.getAmount()))
                .toList();

        return new PayrollRecordResponse(
                record.getId(),
                emp.getId(),
                emp.getEmployeeCode(),
                emp.getFirstName() + " " + emp.getLastName(),
                record.getBaseSalary(),
                record.getGrossEarnings(),
                record.getTotalDeductions(),
                record.getNetSalary(),
                record.getPdfStatus().name(),
                record.getPayslipPdfUrl(),
                detailDtos);
    }
}
