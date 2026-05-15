package com.lab2.demo.service;

import com.lab2.demo.config.MinioProperties;
import com.lab2.demo.dto.*;
import com.lab2.demo.exception.NotFoundException;
import com.lab2.demo.model.MalwareSignature;
import com.lab2.demo.model.MalwareSignatureAudit;
import com.lab2.demo.model.SignatureFile;
import com.lab2.demo.model.SignatureStatus;
import com.lab2.demo.repository.MalwareSignatureAuditRepository;
import com.lab2.demo.repository.MalwareSignatureRepository;
import com.lab2.demo.repository.SignatureFileRepository;
import com.lab2.demo.service.SigningService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@Service
public class SignatureFileService {

    private final FileSignatureCalculationService calculationService;
    private final MinioStorageService minioStorageService;
    private final MalwareSignatureRepository signatureRepository;
    private final SignatureFileRepository signatureFileRepository;
    private final MalwareSignatureAuditRepository auditRepository;
    private final SigningService signingService;
    private final MinioProperties minioProperties;

    public SignatureFileService(FileSignatureCalculationService calculationService,
                                MinioStorageService minioStorageService,
                                MalwareSignatureRepository signatureRepository,
                                SignatureFileRepository signatureFileRepository,
                                MalwareSignatureAuditRepository auditRepository,
                                SigningService signingService,
                                MinioProperties minioProperties) {
        this.calculationService = calculationService;
        this.minioStorageService = minioStorageService;
        this.signatureRepository = signatureRepository;
        this.signatureFileRepository = signatureFileRepository;
        this.auditRepository = auditRepository;
        this.signingService = signingService;
        this.minioProperties = minioProperties;
    }

    @Transactional
    public SignatureFileUploadResponse uploadAndCreateFromFile(MultipartFile file,
                                                               String threatName,
                                                               String uploadedBy) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File must not be empty");
            }

            byte[] fileBytes = file.getBytes();

            String originalFileName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "unknown-file";

            String finalThreatName = threatName != null && !threatName.isBlank()
                    ? threatName
                    : originalFileName;

            CalculatedFileSignature calculated = calculationService.calculate(
                    fileBytes,
                    originalFileName,
                    file.getContentType()
            );

            MalwareSignature signature = createMalwareSignature(finalThreatName, calculated);
            MalwareSignature savedSignature = signatureRepository.save(signature);

            String objectKey = buildObjectKey(savedSignature.getId(), originalFileName);

            minioStorageService.uploadFile(
                    objectKey,
                    fileBytes,
                    file.getContentType()
            );

            SignatureFile signatureFile = new SignatureFile();
            signatureFile.setId(UUID.randomUUID());
            signatureFile.setSignature(savedSignature);
            signatureFile.setBucketName(minioProperties.getBucket());
            signatureFile.setObjectKey(objectKey);
            signatureFile.setOriginalFileName(originalFileName);
            signatureFile.setContentType(file.getContentType());
            signatureFile.setFileSize(calculated.getFileSize());
            signatureFile.setFileSha256(calculated.getFileSha256());
            signatureFile.setUploadedAt(Instant.now());
            signatureFile.setUploadedBy(uploadedBy);

            SignatureFile savedFile = signatureFileRepository.save(signatureFile);

            saveAudit(
                    savedSignature.getId(),
                    uploadedBy,
                    "{\"changed\":[\"createdFromFile\"]}",
                    "Signature created from uploaded file"
            );

            return SignatureFileUploadResponse.from(savedFile);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload file and create signature", ex);
        }
    }

    public List<SignatureFilePresignedUrlResponse> getPresignedUrls(SignatureFileUrlsRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("ids must not be empty");
        }

        List<SignatureFile> files = signatureFileRepository.findBySignatureIdIn(request.getIds());

        return files.stream()
                .map(this::buildPresignedUrlResponse)
                .toList();
    }

    private MalwareSignature createMalwareSignature(String threatName,
                                                    CalculatedFileSignature calculated) {
        MalwareSignature signature = new MalwareSignature();

        signature.setId(UUID.randomUUID());
        signature.setThreatName(threatName);
        signature.setFirstBytesHex(calculated.getFirstBytesHex());
        signature.setRemainderHashHex(calculated.getRemainderHashHex());
        signature.setRemainderLength(calculated.getRemainderLength());
        signature.setFileType(calculated.getFileType());
        signature.setOffsetStart(calculated.getOffsetStart());
        signature.setOffsetEnd(calculated.getOffsetEnd());
        signature.setStatus(SignatureStatus.ACTUAL);

        String digitalSignature = signSignature(signature);
        signature.setDigitalSignatureBase64(digitalSignature);

        signature.setUpdatedAt(Instant.now());

        return signature;
    }

    private String signSignature(MalwareSignature signature) {
        MalwareSignatureSigningPayload payload = MalwareSignatureSigningPayload.builder()
                .threatName(signature.getThreatName())
                .firstBytesHex(signature.getFirstBytesHex())
                .remainderHashHex(signature.getRemainderHashHex())
                .remainderLength(signature.getRemainderLength())
                .fileType(signature.getFileType())
                .offsetStart(signature.getOffsetStart())
                .offsetEnd(signature.getOffsetEnd())
                .status(signature.getStatus())
                .build();

        return signingService.sign(payload);
    }

    private SignatureFilePresignedUrlResponse buildPresignedUrlResponse(SignatureFile file) {
        String url = minioStorageService.generatePresignedUrl(file.getObjectKey());
        Instant expiresAt = minioStorageService.calculateExpirationTime();

        return SignatureFilePresignedUrlResponse.builder()
                .signatureId(file.getSignature().getId())
                .fileId(file.getId())
                .originalFileName(file.getOriginalFileName())
                .bucketName(file.getBucketName())
                .objectKey(file.getObjectKey())
                .url(url)
                .expiresAt(expiresAt)
                .build();
    }

    private void saveAudit(UUID signatureId,
                           String changedBy,
                           String fieldsChanged,
                           String description) {
        MalwareSignatureAudit audit = new MalwareSignatureAudit();
        audit.setSignatureId(signatureId);
        audit.setChangedBy(changedBy);
        audit.setChangedAt(Instant.now());
        audit.setFieldsChanged(fieldsChanged);
        audit.setDescription(description);

        auditRepository.save(audit);
    }

    private String buildObjectKey(UUID signatureId, String originalFileName) {
        return "signatures/"
                + signatureId
                + "/"
                + UUID.randomUUID()
                + "-"
                + sanitizeFileName(originalFileName);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "uploaded-file";
        }

        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}