package com.payroll.aj.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payroll.aj.organization.entity.Company;

/**
 * Projection of a {@link Company} for company switcher and organization APIs.
 */
public record CompanyResponse(
        Long id,
        String name,
        @JsonProperty("registration_number")
        String registrationNumber,
        @JsonProperty("country_id")
        Long countryId,
        @JsonProperty("country_name")
        String countryName,
        @JsonProperty("currency_code")
        String currencyCode,
        @JsonProperty("is_active")
        Boolean isActive
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getRegistrationNumber(),
                company.getCountry().getId(),
                company.getCountry().getName(),
                company.getCountry().getCurrencyCode().name(),
                company.getIsActive()
        );
    }
}
