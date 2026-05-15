package com.lab2.demo.service;

import com.lab2.demo.dto.CalculatedFileSignature;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

@Service
public class FileSignatureCalculationService {

    private static final int FIRST_BYTES_LENGTH = 8;

    public CalculatedFileSignature calculate(byte[] fileBytes, String originalFileName, String contentType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("File must not be empty");
        }

        int firstLength = Math.min(FIRST_BYTES_LENGTH, fileBytes.length);

        byte[] firstBytes = Arrays.copyOfRange(fileBytes, 0, firstLength);
        byte[] remainderBytes = Arrays.copyOfRange(fileBytes, firstLength, fileBytes.length);

        String firstBytesHex = toHex(firstBytes);
        String remainderHashHex = sha256Hex(remainderBytes);
        String fileSha256 = sha256Hex(fileBytes);

        long remainderLength = remainderBytes.length;
        long offsetStart = 0L;
        long offsetEnd = firstLength - 1L;

        String fileType = resolveFileType(originalFileName, contentType);

        return CalculatedFileSignature.builder()
                .firstBytesHex(firstBytesHex)
                .remainderHashHex(remainderHashHex)
                .remainderLength(remainderLength)
                .fileType(fileType)
                .offsetStart(offsetStart)
                .offsetEnd(offsetEnd)
                .fileSha256(fileSha256)
                .fileSize((long) fileBytes.length)
                .build();
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate SHA-256", ex);
        }
    }

    private String toHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes).toUpperCase();
    }

    private String resolveFileType(String originalFileName, String contentType) {
        String extension = extractExtension(originalFileName);

        if (extension != null) {
            return switch (extension) {
                case "exe", "dll" -> "PE";
                case "elf", "so" -> "ELF";
                case "pdf" -> "PDF";
                case "doc", "docx" -> "DOC";
                case "zip", "jar" -> "ARCHIVE";
                default -> resolveContentTypeOrUnknown(contentType);
            };
        }

        return resolveContentTypeOrUnknown(contentType);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private String resolveContentTypeOrUnknown(String contentType) {
        if (contentType != null && !contentType.isBlank()) { return contentType; }
        return "UNKNOWN";
    }
}