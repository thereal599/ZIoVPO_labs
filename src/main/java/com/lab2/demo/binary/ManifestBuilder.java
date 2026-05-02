package com.lab2.demo.binary;

import com.lab2.demo.dto.RecordDataInfo;
import com.lab2.demo.binary.BinaryProtocolConstants;
import com.lab2.demo.binary.BinaryWriter;
import com.lab2.demo.model.MalwareSignature;
import com.lab2.demo.model.SignatureStatus;
import com.lab2.demo.service.SigningService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ManifestBuilder {

    private final SigningService signingService;

    public ManifestBuilder(SigningService signingService) {
        this.signingService = signingService;
    }

    public byte[] build(List<MalwareSignature> signatures,
                        List<RecordDataInfo> recordDataInfos,
                        int exportType,
                        Instant since,
                        byte[] dataSha256) {

        if (signatures == null) {
            throw new IllegalArgumentException("Signatures list must not be null");
        }
        if (recordDataInfos == null) {
            throw new IllegalArgumentException("Record data infos must not be null");
        }
        if (signatures.size() != recordDataInfos.size()) {
            throw new IllegalArgumentException("Signatures count and record data info count must be equal");
        }
        if (dataSha256 == null || dataSha256.length != 32) {
            throw new IllegalArgumentException("dataSha256 must be exactly 32 bytes");
        }

        byte[] unsignedManifest = buildUnsignedManifest(
                signatures,
                recordDataInfos,
                exportType,
                since,
                dataSha256
        );

        byte[] manifestSignature = signingService.signBytes(unsignedManifest);

        BinaryWriter writer = new BinaryWriter();
        writer.writeBytesRaw(unsignedManifest);
        writer.writeU32(manifestSignature.length);
        writer.writeBytesRaw(manifestSignature);

        return writer.toByteArray();
    }

    private byte[] buildUnsignedManifest(List<MalwareSignature> signatures,
                                         List<RecordDataInfo> recordDataInfos,
                                         int exportType,
                                         Instant since,
                                         byte[] dataSha256) {

        BinaryWriter writer = new BinaryWriter();
        writeHeader(writer, exportType, since, signatures.size(), dataSha256);
        Map<UUID, RecordDataInfo> recordInfoById = recordDataInfos.stream()
                .collect(Collectors.toMap(
                        RecordDataInfo::getSignatureId,
                        Function.identity()
                ));

        for (MalwareSignature signature : signatures) {
            RecordDataInfo recordDataInfo = recordInfoById.get(signature.getId());
            if (recordDataInfo == null) {
                throw new IllegalStateException("No data info for signature: " + signature.getId());
            }
            writeEntry(writer, signature, recordDataInfo);
        }
        return writer.toByteArray();
    }

    private void writeHeader(BinaryWriter writer,
                             int exportType,
                             Instant since,
                             int recordCount,
                             byte[] dataSha256) {

        writer.writeString(BinaryProtocolConstants.MANIFEST_MAGIC);
        writer.writeU16(BinaryProtocolConstants.VERSION);
        writer.writeU8(exportType);
        writer.writeI64(Instant.now().toEpochMilli());
        writer.writeI64(since == null ? -1L : since.toEpochMilli());
        writer.writeU32(recordCount);
        writer.writeBytesRaw(dataSha256);
    }

    private void writeEntry(BinaryWriter writer,
                            MalwareSignature signature,
                            RecordDataInfo recordDataInfo) {

        writer.writeUuid(signature.getId());
        writer.writeU8(toStatusCode(signature.getStatus()));
        writer.writeI64(signature.getUpdatedAt().toEpochMilli());
        writer.writeI64(recordDataInfo.getDataOffset());
        writer.writeI64(recordDataInfo.getDataLength());

        byte[] recordSignatureBytes = Base64.getDecoder()
                .decode(signature.getDigitalSignatureBase64());

        writer.writeU32(recordSignatureBytes.length);
        writer.writeBytesRaw(recordSignatureBytes);
    }

    private int toStatusCode(SignatureStatus status) {
        if (status == SignatureStatus.ACTUAL) {
            return BinaryProtocolConstants.STATUS_ACTUAL;
        }

        if (status == SignatureStatus.DELETED) {
            return BinaryProtocolConstants.STATUS_DELETED;
        }

        throw new IllegalArgumentException("Unsupported signature status: " + status);
    }
}