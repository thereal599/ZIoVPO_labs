package com.lab2.demo.binary;

import com.lab2.demo.dto.BinaryDataBuildResult;
import com.lab2.demo.dto.RecordDataInfo;
import com.lab2.demo.binary.BinaryProtocolConstants;
import com.lab2.demo.binary.BinaryWriter;
import com.lab2.demo.binary.HexUtils;
import com.lab2.demo.model.MalwareSignature;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataBinBuilder {

    public BinaryDataBuildResult build(List<MalwareSignature> signatures) {
        if (signatures == null) {
            throw new IllegalArgumentException("Signatures list must not be null");
        }

        BinaryWriter writer = new BinaryWriter();
        writeHeader(writer, signatures.size());
        List<RecordDataInfo> records = new ArrayList<>();
        int payloadStart = writer.size();

        for (MalwareSignature signature : signatures) {
            int recordStart = writer.size();
            writeRecord(writer, signature);
            int recordEnd = writer.size();
            long dataOffset = recordStart - payloadStart;
            long dataLength = recordEnd - recordStart;
            records.add(new RecordDataInfo(
                    signature.getId(),
                    dataOffset,
                    dataLength
            ));
        }

        return new BinaryDataBuildResult(writer.toByteArray(), records);
    }

    private void writeHeader(BinaryWriter writer, int recordCount) {
        writer.writeString(BinaryProtocolConstants.DATA_MAGIC);
        writer.writeU16(BinaryProtocolConstants.VERSION);
        writer.writeU32(recordCount);
    }

    private void writeRecord(BinaryWriter writer, MalwareSignature signature) {
        if (signature == null) {
            throw new IllegalArgumentException("Signature must not be null");
        }

        byte[] firstBytes = HexUtils.decodeHex(signature.getFirstBytesHex());
        byte[] remainderHash = HexUtils.decodeHex(signature.getRemainderHashHex());

        writer.writeString(signature.getThreatName());
        writer.writeByteArray(firstBytes);
        writer.writeByteArray(remainderHash);
        writer.writeI64(signature.getRemainderLength());
        writer.writeString(signature.getFileType());
        writer.writeI64(signature.getOffsetStart());
        writer.writeI64(signature.getOffsetEnd());
    }
}