package com.payroll.aj.payroll.controller;

import com.payroll.aj.payroll.dto.PayrollBatchResponse;
import com.payroll.aj.payroll.dto.PayrollRecordResponse;
import com.payroll.aj.payroll.dto.PayrollRunRequest;
import com.payroll.aj.payroll.service.PayrollExecutionService;
import com.payroll.aj.security.userdetails.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for payroll batch execution and retrieval.
 *
 * <p>All endpoints require a valid JWT (enforced by
 * {@link com.payroll.aj.security.config.SecurityConfig}).
 * Company-scoped endpoints additionally verify RBAC access via
 * {@link com.payroll.aj.security.scope.RequireCompanyAccess}
 * on the underlying service methods.</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PayrollExecutionController {

    private final PayrollExecutionService executionService;

    /**
     * Triggers a monthly payroll run for a company.
     *
     * <p><strong>POST</strong>
     * {@code /api/v1/companies/{companyId}/payroll/run}</p>
     *
     * @return 201 Created with the completed batch response
     * @throws com.payroll.aj.payroll.exception.PayrollBatchConflictException
     *         409 if a COMPLETED or PROCESSING batch already exists
     */
    @PostMapping("/companies/{companyId}/payroll/run")
    public ResponseEntity<PayrollBatchResponse> runPayroll(
            @PathVariable Long companyId,
            @Valid @RequestBody PayrollRunRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        PayrollBatchResponse response = executionService.executePayroll(
                companyId,
                request.year(),
                request.month(),
                principal.getUserId(),
                principal.getTenantId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all historical payroll batches for a company.
     *
     * <p><strong>GET</strong>
     * {@code /api/v1/companies/{companyId}/payroll/batches}</p>
     */
    @GetMapping("/companies/{companyId}/payroll/batches")
    public ResponseEntity<List<PayrollBatchResponse>> listBatches(
            @PathVariable Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                executionService.getBatchesByCompanyId(companyId));
    }

    /**
     * Retrieves detailed status of a single payroll batch.
     *
     * <p><strong>GET</strong>
     * {@code /api/v1/payroll/batches/{batchId}}</p>
     */
    @GetMapping("/payroll/batches/{batchId}")
    public ResponseEntity<PayrollBatchResponse> getBatch(
            @PathVariable Long batchId) {

        return ResponseEntity.ok(executionService.getBatchById(batchId));
    }

    /**
     * Retrieves paginated employee payroll records within a batch,
     * including itemised component breakdowns.
     *
     * <p><strong>GET</strong>
     * {@code /api/v1/payroll/batches/{batchId}/records}</p>
     *
     * @param pageable pagination parameters (default page size 20)
     */
    @GetMapping("/payroll/batches/{batchId}/records")
    public ResponseEntity<Page<PayrollRecordResponse>> getBatchRecords(
            @PathVariable Long batchId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                executionService.getRecordsByBatchId(batchId, pageable));
    }
}
