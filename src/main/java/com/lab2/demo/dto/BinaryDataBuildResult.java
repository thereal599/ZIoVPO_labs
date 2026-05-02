package com.lab2.demo.dto;

import java.util.List;

public class BinaryDataBuildResult {

    private final byte[] dataBytes;
    private final List<RecordDataInfo> records;

    public BinaryDataBuildResult(byte[] dataBytes, List<RecordDataInfo> records) {
        this.dataBytes = dataBytes;
        this.records = records;
    }

    public byte[] getDataBytes() {
        return dataBytes;
    }

    public List<RecordDataInfo> getRecords() {
        return records;
    }
}