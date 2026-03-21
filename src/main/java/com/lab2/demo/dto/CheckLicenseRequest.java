package com.lab2.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CheckLicenseRequest {

    @NotBlank
    private String deviceMac;

    @NotNull
    private UUID productId;
}