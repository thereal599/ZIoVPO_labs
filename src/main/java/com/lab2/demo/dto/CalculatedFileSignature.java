package com.lab2.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalculatedFileSignature {

    private String firstBytesHex;
    private String remainderHashHex;
    private Long remainderLength;

    private String fileType;
    private Long offsetStart;
    private Long offsetEnd;

    private String fileSha256;
    private Long fileSize;
}