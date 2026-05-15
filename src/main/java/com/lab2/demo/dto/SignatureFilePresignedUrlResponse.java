package com.lab2.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SignatureFilePresignedUrlResponse {

    private UUID signatureId;
    private UUID fileId;

    private String originalFileName;
    private String bucketName;
    private String objectKey;

    private String url;
    private Instant expiresAt;
}