package com.payroll.aj.organization.service;

import com.payroll.aj.common.enums.UserRole;
import com.payroll.aj.organization.dto.CompanyResponse;
import com.payroll.aj.organization.dto.DepartmentResponse;
import com.payroll.aj.organization.entity.Company;
import com.payroll.aj.organization.repository.CompanyRepository;
import com.payroll.aj.organization.repository.DepartmentRepository;
import com.payroll.aj.security.scope.RequireCompanyAccess;
import com.payroll.aj.security.userdetails.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service for organization structure queries: companies and departments.
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Lists active departments for a company.
     * Enforces RBAC company access.
     */
    @Transactional(readOnly = true)
    @RequireCompanyAccess("companyId")
    public List<DepartmentResponse> getDepartmentsByCompany(Long companyId) {
        return departmentRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId)
                .stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    /**
     * Lists all companies accessible by the authenticated user.
     * SUPER_ADMIN sees all companies in their tenant; other roles see their assigned company scope.
     */
    @Transactional(readOnly = true)
    public List<CompanyResponse> getMyCompanies(UserPrincipal principal) {
        List<Company> companies;
        if (principal.getRole() == UserRole.SUPER_ADMIN) {
            companies = companyRepository.findByTenantIdWithCountry(principal.getTenantId());
        } else {
            Set<Long> allowedIds = principal.getCompanyIds();
            if (allowedIds == null || allowedIds.isEmpty()) {
                return List.of();
            }
            companies = companyRepository.findActiveByIdsWithCountry(allowedIds);
        }

        return companies.stream()
                .map(CompanyResponse::from)
                .toList();
    }
}
