package com.lab2.demo.dto;

import com.lab2.demo.model.SignatureFile;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SignatureFileUploadResponse {

    private UUID signatureId;
    private UUID fileId;

    private String threatName;
    private String originalFileName;
    private String bucketName;
    private String objectKey;

    private String contentType;
    private Long fileSize;
    private String fileSha256;

    private Instant uploadedAt;
    private String uploadedBy;

    public static SignatureFileUploadResponse from(SignatureFile file) {
        return SignatureFileUploadResponse.builder()
                .signatureId(file.getSignature().getId())
                .fileId(file.getId())
                .threatName(file.getSignature().getThreatName())
                .originalFileName(file.getOriginalFileName())
                .bucketName(file.getBucketName())
                .objectKey(file.getObjectKey())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .fileSha256(file.getFileSha256())
                .uploadedAt(file.getUploadedAt())
                .uploadedBy(file.getUploadedBy())
                .build();
    }
}