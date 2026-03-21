package com.lab2.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateLicenseRequest {

    @NotNull
    private UUID productId;

    @NotNull
    private UUID typeId;

    @NotNull
    private UUID ownerId;

    @NotNull
    @Min(1)
    private Integer deviceCount;

    private String description;
}