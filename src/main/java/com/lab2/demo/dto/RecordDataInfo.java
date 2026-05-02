package com.lab2.demo.dto;

import java.util.UUID;

public class RecordDataInfo {

    private final UUID signatureId;
    private final long dataOffset;
    private final long dataLength;

    public RecordDataInfo(UUID signatureId, long dataOffset, long dataLength) {
        this.signatureId = signatureId;
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
    }

    public UUID getSignatureId() {
        return signatureId;
    }

    public long getDataOffset() {
        return dataOffset;
    }

    public long getDataLength() {
        return dataLength;
    }
}