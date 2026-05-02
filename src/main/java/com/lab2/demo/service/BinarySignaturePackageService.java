package com.lab2.demo.service;

import com.lab2.demo.binary.DataBinBuilder;
import com.lab2.demo.binary.ManifestBuilder;
import com.lab2.demo.dto.BinaryDataBuildResult;
import com.lab2.demo.dto.BinarySignaturePackage;
import com.lab2.demo.binary.BinaryProtocolConstants;
import com.lab2.demo.model.MalwareSignature;
import com.lab2.demo.model.SignatureStatus;
import com.lab2.demo.repository.MalwareSignatureRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BinarySignaturePackageService {

    private final MalwareSignatureRepository signatureRepository;
    private final DataBinBuilder dataBinBuilder;
    private final ManifestBuilder manifestBuilder;

    public BinarySignaturePackageService(MalwareSignatureRepository signatureRepository,
                                         DataBinBuilder dataBinBuilder,
                                         ManifestBuilder manifestBuilder) {
        this.signatureRepository = signatureRepository;
        this.dataBinBuilder = dataBinBuilder;
        this.manifestBuilder = manifestBuilder;
    }

    public BinarySignaturePackage buildFullPackage() {
        List<MalwareSignature> signatures =
                signatureRepository.findByStatusOrderByUpdatedAtDesc(SignatureStatus.ACTUAL);

        return buildPackage(
                signatures,
                BinaryProtocolConstants.EXPORT_TYPE_FULL,
                null
        );
    }

    public BinarySignaturePackage buildIncrementPackage(Instant since) {
        if (since == null) {
            throw new IllegalArgumentException("since is required");
        }

        List<MalwareSignature> signatures =
                signatureRepository.findByUpdatedAtAfterOrderByUpdatedAtAsc(since);

        return buildPackage(
                signatures,
                BinaryProtocolConstants.EXPORT_TYPE_INCREMENT,
                since
        );
    }

    public BinarySignaturePackage buildByIdsPackage(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids must not be empty");
        }

        List<MalwareSignature> signatures = signatureRepository.findByIdIn(ids);

        return buildPackage(
                signatures,
                BinaryProtocolConstants.EXPORT_TYPE_BY_IDS,
                null
        );
    }

    private BinarySignaturePackage buildPackage(List<MalwareSignature> signatures,
                                                int exportType,
                                                Instant since) {

        BinaryDataBuildResult dataBuildResult = dataBinBuilder.build(signatures);

        byte[] dataBytes = dataBuildResult.getDataBytes();
        byte[] dataSha256 = sha256(dataBytes);

        byte[] manifestBytes = manifestBuilder.build(
                signatures,
                dataBuildResult.getRecords(),
                exportType,
                since,
                dataSha256
        );

        return new BinarySignaturePackage(manifestBytes, dataBytes);
    }

    private byte[] sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate SHA-256", ex);
        }
    }
}