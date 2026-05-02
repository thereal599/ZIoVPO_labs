package com.lab2.demo.binary;

public final class BinaryProtocolConstants {

    private BinaryProtocolConstants() {
    }

    public static final int VERSION = 1;

    public static final int EXPORT_TYPE_FULL = 1;
    public static final int EXPORT_TYPE_INCREMENT = 2;
    public static final int EXPORT_TYPE_BY_IDS = 3;

    public static final int STATUS_ACTUAL = 1;
    public static final int STATUS_DELETED = 2;

    public static final String MANIFEST_MAGIC = "MF-KRUPYAKOV";
    public static final String DATA_MAGIC = "DB-KRUPYAKOV";
}