package com.payroll.aj.payroll.service;

import com.payroll.aj.common.enums.CalculationType;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.organization.repository.CompanyRepository;
import com.payroll.aj.payroll.dto.CreateSalaryComponentRequest;
import com.payroll.aj.payroll.dto.SalaryComponentResponse;
import com.payroll.aj.payroll.engine.SpelFormulaEvaluatorService;
import com.payroll.aj.payroll.entity.SalaryComponent;
import com.payroll.aj.payroll.repository.SalaryComponentRepository;
import com.payroll.aj.security.scope.RequireCompanyAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service for querying and creating salary component templates.
 */
@Service
@RequiredArgsConstructor
public class SalaryComponentService {

    private final SalaryComponentRepository salaryComponentRepository;
    private final CompanyRepository companyRepository;
    private final SpelFormulaEvaluatorService formulaEvaluator;

    /**
     * Lists all salary components configured for the company's jurisdiction country.
     */
    @Transactional(readOnly = true)
    @RequireCompanyAccess("companyId")
    public List<SalaryComponentResponse> getComponentsByCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company not found with ID: " + companyId));

        return salaryComponentRepository
                .findByCountryIdOrderByTypeAscNameAsc(company.getCountry().getId())
                .stream()
                .map(SalaryComponentResponse::from)
                .toList();
    }

    /**
     * Creates a new salary component template in the company's country.
     */
    @Transactional
    @RequireCompanyAccess("companyId")
    public SalaryComponentResponse createComponent(Long companyId, CreateSalaryComponentRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company not found with ID: " + companyId));

        String code = request.code().trim().toUpperCase();
        if (salaryComponentRepository.existsByTenantIdAndCode(company.getCountry().getTenant().getId(), code)) {
            throw new IllegalArgumentException("Salary component with code '" + code + "' already exists");
        }

        if (request.calculationType() == CalculationType.CUSTOM_FORMULA) {
            if (request.formulaExpression() == null || request.formulaExpression().isBlank()) {
                throw new IllegalArgumentException("Formula expression is required for CUSTOM_FORMULA calculation type");
            }
            // Syntax validation
            formulaEvaluator.extractVariableReferences(request.formulaExpression());
        }

        SalaryComponent component = SalaryComponent.builder()
                .tenant(company.getCountry().getTenant())
                .country(company.getCountry())
                .code(code)
                .name(request.name().trim())
                .type(request.type())
                .calculationType(request.calculationType())
                .formulaExpression(request.calculationType() == CalculationType.CUSTOM_FORMULA ? request.formulaExpression().trim() : null)
                .defaultValue(request.defaultValue() != null ? request.defaultValue() : BigDecimal.ZERO)
                .isTaxable(request.isTaxable() != null ? request.isTaxable() : true)
                .isStatutory(request.isStatutory() != null ? request.isStatutory() : false)
                .isActive(true)
                .build();

        SalaryComponent saved = salaryComponentRepository.save(component);
        return SalaryComponentResponse.from(saved);
    }
}
