package com.lab2.demo.binary;

public final class HexUtils {

    private HexUtils() {}

    public static byte[] decodeHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Hex value must not be null");
        }

        String normalized = hex.trim();

        if (normalized.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex value must contain even number of characters");
        }

        byte[] result = new byte[normalized.length() / 2];

        for (int i = 0; i < normalized.length(); i += 2) {
            int high = Character.digit(normalized.charAt(i), 16);
            int low = Character.digit(normalized.charAt(i + 1), 16);

            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex value: " + hex);
            }

            result[i / 2] = (byte) ((high << 4) + low);
        }

        return result;
    }
}