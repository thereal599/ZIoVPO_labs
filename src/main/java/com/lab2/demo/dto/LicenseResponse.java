package com.lab2.demo.dto;

import com.lab2.demo.model.License;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class LicenseResponse {
    private UUID id;
    private String code;
    private UUID userId;
    private UUID ownerId;
    private UUID productId;
    private UUID typeId;
    private LocalDate firstActivationDate;
    private LocalDate endingDate;
    private Boolean blocked;
    private Integer deviceCount;
    private String description;

    public static LicenseResponse from(License license) {
        return LicenseResponse.builder()
                .id(license.getId())
                .code(license.getCode())
                .userId(license.getUser() != null ? license.getUser().getId() : null)
                .ownerId(license.getOwner().getId())
                .productId(license.getProduct().getId())
                .typeId(license.getType().getId())
                .firstActivationDate(license.getFirstActivationDate())
                .endingDate(license.getEndingDate())
                .blocked(license.getBlocked())
                .deviceCount(license.getDeviceCount())
                .description(license.getDescription())
                .build();
    }
}