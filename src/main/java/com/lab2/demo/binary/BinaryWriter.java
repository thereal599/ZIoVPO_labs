package com.lab2.demo.binary;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BinaryWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public void writeU8(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("uint8 out of range: " + value);
        }
        out.write(value & 0xFF);
    }

    public void writeU16(int value) {
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException("uint16 out of range: " + value);
        }
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    public void writeU32(long value) {
        if (value < 0 || value > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("uint32 out of range: " + value);
        }
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    public void writeI64(long value) {
        out.write((int) ((value >>> 56) & 0xFF));
        out.write((int) ((value >>> 48) & 0xFF));
        out.write((int) ((value >>> 40) & 0xFF));
        out.write((int) ((value >>> 32) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) (value & 0xFF));
    }

    public void writeUuid(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID must not be null");
        }
        writeI64(uuid.getMostSignificantBits());
        writeI64(uuid.getLeastSignificantBits());
    }

    public void writeString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("String must not be null");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeU32(bytes.length);
        writeBytesRaw(bytes);
    }

    public void writeByteArray(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Byte array must not be null");
        }
        writeU32(bytes.length);
        writeBytesRaw(bytes);
    }

    public void writeBytesRaw(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Byte array must not be null");
        }
        out.writeBytes(bytes);
    }

    public int size() {
        return out.size();
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }
}