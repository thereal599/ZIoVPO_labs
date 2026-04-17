package com.lab2.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SignatureIdsRequest {

    @NotEmpty
    private List<UUID> ids;
}