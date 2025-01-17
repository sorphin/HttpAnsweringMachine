package org.kendar.mongo.model;

import org.kendar.mongo.handlers.OpCodes;
import org.kendar.mongo.model.payloads.BaseMsgPayload;
import org.kendar.typed.serializer.TypedSerializer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MsgPacket extends MongoPacket<MsgPacket> implements MongoReqResPacket {
    private List<BaseMsgPayload> payloads = new ArrayList<>();
    private int flagBits;
    private int checksum;
    private int requestId;
    private int responseTo;
    public MsgPacket() {
        setOpCode(OpCodes.OP_MSG);
    }

    public List<BaseMsgPayload> getPayloads() {
        return payloads;
    }

    public void setPayloads(List<BaseMsgPayload> payloads) {
        this.payloads = payloads;
    }

    public int getFlagBits() {
        return flagBits;
    }

    public void setFlagBits(int flagBits) {
        this.flagBits = flagBits;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    @Override
    public void serialize(TypedSerializer typedSerializer) {
        super.serialize(typedSerializer);
        typedSerializer.write("flagBits", flagBits);
        typedSerializer.write("checksum", checksum);
        typedSerializer.write("payloads", payloads);
        typedSerializer.write("requestId", requestId);
        typedSerializer.write("responseTo", responseTo);
    }

    @Override
    public MsgPacket deserialize(TypedSerializer typedSerializer) {
        super.deserialize(typedSerializer);
        flagBits = typedSerializer.read("flagBits");
        checksum = typedSerializer.read("checksum");
        payloads = typedSerializer.read("payloads");
        requestId = typedSerializer.read("requestId");
        responseTo = typedSerializer.read("responseTo");
        return this;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public int getResponseTo() {
        return responseTo;
    }

    public void setResponseTo(Integer responseTo) {
        this.responseTo = responseTo;
    }

    public byte[] serialize() {
        var msgLength = 16;
        ByteBuffer responseBuffer = ByteBuffer.allocate(64000);
        responseBuffer.order(ByteOrder.LITTLE_ENDIAN);
        responseBuffer.putInt(flagBits);
        for (var payload : this.payloads) {
            responseBuffer.put(payload.serialize());
        }
        msgLength += responseBuffer.position();

        responseBuffer.flip();
        var length = responseBuffer.position();
        responseBuffer.position(0);
        var res = new byte[msgLength];
        for (var i = 16; i < msgLength; i++) {
            res[i] = responseBuffer.get();
        }

        var header = buildHeader(msgLength, requestId, responseTo, OpCodes.OP_MSG);
        System.arraycopy(header, 0, res, 0, 16);
        return res;
    }
}
