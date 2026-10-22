package com.payroll.aj.payroll.dto;

public record PresignedUrlResponse(
        String url,
        long expiresInSeconds
) {
}
