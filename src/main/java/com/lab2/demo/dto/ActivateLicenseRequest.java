package com.lab2.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivateLicenseRequest {

    @NotBlank
    private String activationKey;

    @NotBlank
    private String deviceMac;

    @NotBlank
    private String deviceName;
}