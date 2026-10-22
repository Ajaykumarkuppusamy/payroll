package com.payroll.aj.payroll.service;

import com.payroll.aj.common.enums.BatchStatus;
import com.payroll.aj.common.enums.EmploymentStatus;
import com.payroll.aj.employee.entity.Employee;
import com.payroll.aj.employee.repository.EmployeeRepository;
import com.payroll.aj.identity.entity.AppUser;
import com.payroll.aj.identity.repository.AppUserRepository;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.organization.repository.CompanyRepository;
import com.payroll.aj.payroll.dto.PayrollBatchResponse;
import com.payroll.aj.payroll.engine.ComponentLineItem;
import com.payroll.aj.payroll.engine.PayrollBreakdown;
import com.payroll.aj.payroll.engine.SalaryComponentCalculationService;
import com.payroll.aj.payroll.entity.PayrollBatch;
import com.payroll.aj.payroll.entity.PayrollRecord;
import com.payroll.aj.payroll.entity.PayrollRecordDetail;
import com.payroll.aj.payroll.exception.PayrollBatchConflictException;
import com.payroll.aj.payroll.repository.EmployeeSalaryStructureRepository;
import com.payroll.aj.payroll.repository.PayrollBatchRepository;
import com.payroll.aj.payroll.repository.PayrollRecordDetailRepository;
import com.payroll.aj.payroll.repository.PayrollRecordRepository;
import com.payroll.aj.tenant.entity.Tenant;
import com.payroll.aj.tenant.repository.TenantRepository;
import com.payroll.aj.common.enums.ComponentType;
import com.payroll.aj.common.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PayrollExecutionService}.
 *
 * <p>Uses Mockito to isolate the service from persistence and
 * calculation layers. Tests cover the execution flow, idempotency,
 * edge cases, and error propagation (which triggers
 * {@code @Transactional} rollback in production).</p>
 */
@ExtendWith(MockitoExtension.class)
class PayrollExecutionServiceTest {

    @Mock private PayrollBatchRepository batchRepository;
    @Mock private PayrollRecordRepository recordRepository;
    @Mock private PayrollRecordDetailRepository recordDetailRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeSalaryStructureRepository salaryStructureRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private SalaryComponentCalculationService calculationService;

    @InjectMocks
    private PayrollExecutionService service;

    // ── Test fixtures ────────────────────────────────────────────

    private static final Long TENANT_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final int YEAR = 2026;
    private static final int MONTH = 9;

    private Tenant tenantRef;
    private Company companyRef;
    private AppUser userRef;

    @BeforeEach
    void setUp() {
        tenantRef = Tenant.builder().name("Acme Corp").slug("acme").build();
        tenantRef.setId(TENANT_ID);

        companyRef = Company.builder().name("Acme India").build();
        companyRef.setId(COMPANY_ID);

        userRef = AppUser.builder()
                .tenant(tenantRef)
                .username("admin")
                .email("admin@acme.com")
                .passwordHash("hash")
                .fullName("Admin User")
                .role(UserRole.SUPER_ADMIN)
                .build();
        userRef.setId(USER_ID);
    }

    private Employee buildEmployee(Long id, String code, String firstName,
                                   String lastName, BigDecimal baseSalary) {
        Employee emp = Employee.builder()
                .company(companyRef)
                .employeeCode(code)
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@acme.com")
                .baseSalary(baseSalary)
                .hireDate(LocalDate.of(2024, 1, 1))
                .employmentStatus(EmploymentStatus.ACTIVE)
                .build();
        emp.setId(id);
        return emp;
    }

    private void stubCommonMocks() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(batchRepository.findByCompanyIdAndYearAndMonth(COMPANY_ID, YEAR, MONTH))
                .thenReturn(Optional.empty());
        when(tenantRepository.getReferenceById(TENANT_ID)).thenReturn(tenantRef);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(companyRef);
        when(appUserRepository.getReferenceById(USER_ID)).thenReturn(userRef);
        when(batchRepository.save(any(PayrollBatch.class))).thenAnswer(inv -> {
            PayrollBatch b = inv.getArgument(0);
            if (b.getId() == null) b.setId(999L);
            return b;
        });
        when(recordRepository.save(any(PayrollRecord.class))).thenAnswer(inv -> {
            PayrollRecord r = inv.getArgument(0);
            if (r.getId() == null) r.setId(System.nanoTime());
            return r;
        });
        when(salaryStructureRepository.findActiveByEmployeeIdAndDate(anyLong(), any()))
                .thenReturn(List.of());
    }

    // ── Successful execution ─────────────────────────────────────

    @Nested
    @DisplayName("Successful payroll execution")
    class SuccessfulExecutionTests {

        @Test
        @DisplayName("End-to-end: 2 employees → correct totals & COMPLETED status")
        void successfulRunWithMultipleEmployees() {
            stubCommonMocks();

            Employee emp1 = buildEmployee(1L, "EMP001", "John", "Doe",
                    new BigDecimal("50000.00"));
            Employee emp2 = buildEmployee(2L, "EMP002", "Jane", "Smith",
                    new BigDecimal("60000.00"));

            when(employeeRepository.findByCompanyIdAndEmploymentStatus(
                    COMPANY_ID, EmploymentStatus.ACTIVE))
                    .thenReturn(List.of(emp1, emp2));

            PayrollBreakdown breakdown1 = new PayrollBreakdown(
                    new BigDecimal("70000.00"),
                    new BigDecimal("6000.00"),
                    new BigDecimal("64000.00"),
                    List.of(new ComponentLineItem("EAR-IND-HRA", "HRA",
                            ComponentType.EARNING, new BigDecimal("20000.00"))),
                    List.of(new ComponentLineItem("DED-IND-PF", "PF",
                            ComponentType.DEDUCTION, new BigDecimal("6000.00"))));

            PayrollBreakdown breakdown2 = new PayrollBreakdown(
                    new BigDecimal("84000.00"),
                    new BigDecimal("7200.00"),
                    new BigDecimal("76800.00"),
                    List.of(new ComponentLineItem("EAR-IND-HRA", "HRA",
                            ComponentType.EARNING, new BigDecimal("24000.00"))),
                    List.of(new ComponentLineItem("DED-IND-PF", "PF",
                            ComponentType.DEDUCTION, new BigDecimal("7200.00"))));

            when(calculationService.calculate(
                    eq(new BigDecimal("50000.00")), anyList()))
                    .thenReturn(breakdown1);
            when(calculationService.calculate(
                    eq(new BigDecimal("60000.00")), anyList()))
                    .thenReturn(breakdown2);

            // When
            PayrollBatchResponse response = service.executePayroll(
                    COMPANY_ID, YEAR, MONTH, USER_ID, TENANT_ID);

            // Then
            assertNotNull(response);
            assertEquals(BatchStatus.COMPLETED.name(), response.status());
            assertEquals(2, response.totalEmployees());
            assertEquals(new BigDecimal("154000.00"), response.totalGrossPay());
            assertEquals(new BigDecimal("13200.00"), response.totalDeductions());
            assertEquals(new BigDecimal("140800.00"), response.totalNetPay());

            // Verify persistence calls
            verify(recordRepository).save(any(PayrollRecord.class));
            verify(recordDetailRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Empty employee list: batch completes with zero totals")
        void emptyEmployeeList() {
            stubCommonMocks();

            when(employeeRepository.findByCompanyIdAndEmploymentStatus(
                    COMPANY_ID, EmploymentStatus.ACTIVE))
                    .thenReturn(List.of());

            PayrollBatchResponse response = service.executePayroll(
                    COMPANY_ID, YEAR, MONTH, USER_ID, TENANT_ID);

            assertEquals(BatchStatus.COMPLETED.name(), response.status());
            assertEquals(0, response.totalEmployees());
            assertEquals(new BigDecimal("0.00"), response.totalGrossPay());
            assertEquals(new BigDecimal("0.00"), response.totalDeductions());
            assertEquals(new BigDecimal("0.00"), response.totalNetPay());

            verify(recordRepository, never()).save(any(PayrollRecord.class));
        }
    }

    // ── Idempotency ──────────────────────────────────────────────

    @Nested
    @DisplayName("Idempotency enforcement")
    class IdempotencyTests {

        @Test
        @DisplayName("COMPLETED batch → throws 409 PayrollBatchConflictException")
        void completedBatchBlocked() {
            when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);

            PayrollBatch completedBatch = PayrollBatch.builder()
                    .tenant(tenantRef)
                    .company(companyRef)
                    .year(YEAR)
                    .month(MONTH)
                    .status(BatchStatus.COMPLETED)
                    .executedBy(userRef)
                    .build();
            completedBatch.setId(50L);

            when(batchRepository.findByCompanyIdAndYearAndMonth(COMPANY_ID, YEAR, MONTH))
                    .thenReturn(Optional.of(completedBatch));

            PayrollBatchConflictException ex = assertThrows(
                    PayrollBatchConflictException.class,
                    () -> service.executePayroll(
                            COMPANY_ID, YEAR, MONTH, USER_ID, TENANT_ID));

            assertEquals(BatchStatus.COMPLETED, ex.getExistingStatus());
            verify(recordRepository, never()).save(any());
        }

        @Test
        @DisplayName("PROCESSING batch → throws 409 PayrollBatchConflictException")
        void processingBatchBlocked() {
            when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);

            PayrollBatch processingBatch = PayrollBatch.builder()
                    .tenant(tenantRef)
                    .company(companyRef)
                    .year(YEAR)
                    .month(MONTH)
                    .status(BatchStatus.PROCESSING)
                    .executedBy(userRef)
                    .build();
            processingBatch.setId(51L);

            when(batchRepository.findByCompanyIdAndYearAndMonth(COMPANY_ID, YEAR, MONTH))
                    .thenReturn(Optional.of(processingBatch));

            assertThrows(PayrollBatchConflictException.class,
                    () -> service.executePayroll(
                            COMPANY_ID, YEAR, MONTH, USER_ID, TENANT_ID));
        }

        @Test
        @DisplayName("FAILED batch → cleaned up and retried successfully")
        void failedBatchRetried() {
            stubCommonMocks();

            PayrollBatch failedBatch = PayrollBatch.builder()
                    .tenant(tenantRef)
                    .company(companyRef)
                    .year(YEAR)
                    .month(MONTH)
                    .status(BatchStatus.FAILED)
                    .executedBy(userRef)
                    .build();
            failedBatch.setId(52L);

            // First call returns the FAILED batch, subsequent calls return empty
            when(batchRepository.findByCompanyIdAndYearAndMonth(COMPANY_ID, YEAR, MONTH))
                    .thenReturn(Optional.of(failedBatch));

            when(employeeRepository.findByCompanyIdAndEmploymentStatus(
                    COMPANY_ID, EmploymentStatus.ACTIVE))
                    .thenReturn(List.of());

            PayrollBatchResponse response = service.executePayroll(
                    COMPANY_ID, YEAR, MONTH, USER_ID, TENANT_ID);

            // Old batch was cleaned up
            verify(recordRepository).deleteAllByBatchId(52L);
            verify(batchRepository).delete(failedBatch);

            // New batch was created and completed
            assertEquals(BatchStatus.COMPLETED.name(), response.status());
        }
    }

    // ── Error propagation (triggers @Transactional rollback) ─────

    @Nested
    @DisplayName("Error propagation")
    class ErrorPropagationTests {

        @Test
        @DisplayName("Calculation failure propagates (triggers rollback)")
        void calculationFailurePropagates() {
            stubCommonMocks();

            Employee emp = buildEmployee(1L, "EMP001", "John", "Doe",
                    new BigDecimal("50000.00"));

            when(employeeRepository.findByCompanyIdAndEmploymentStatus(
                    COMPANY_ID, EmploymentStatus.ACTIVE))
                    .thenReturn(List.of(emp));

            when(calculationService.calculate(any(), anyList()))
                    .thenThrow(new IllegalStateException(
                            "Circular dependency detected"));

            assertThrows(IllegalStateException.class,
                    () -> service.executePayroll(
                            COMPANY_ID, YEAR, MONTH, USER_ID, TENANT_ID));
        }

        @Test
        @DisplayName("Non-existent company throws EntityNotFoundException")
        void nonExistentCompanyThrows() {
            when(companyRepository.existsById(COMPANY_ID)).thenReturn(false);

            assertThrows(jakarta.persistence.EntityNotFoundException.class,
                    () -> service.executePayroll(
                            COMPANY_ID, YEAR, MONTH, USER_ID, TENANT_ID));
        }
    }
}
