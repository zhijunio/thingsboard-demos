package org.example.thingsboard;

import org.example.thingsboard.proto.KeyValueProto;
import org.example.thingsboard.proto.KeyValueType;
import org.example.thingsboard.proto.PostTelemetryMsg;
import org.example.thingsboard.proto.TbMsgProto;
import org.example.thingsboard.proto.TsKvListProto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtobufDemoTest {

    @Test
    void messageRoundTripPreservesTheThingsBoardFields() throws Exception {
        PostTelemetryMsg telemetry = PostTelemetryMsg.newBuilder()
                .addTsKvList(TsKvListProto.newBuilder()
                        .setTs(1000)
                        .addKv(KeyValueProto.newBuilder()
                                .setKey("temperature")
                                .setType(KeyValueType.DOUBLE_V)
                                .setDoubleV(23.5)))
                .build();

        PostTelemetryMsg decodedTelemetry = PostTelemetryMsg.parseFrom(telemetry.toByteArray());

        TbMsgProto message = TbMsgProto.newBuilder()
                .setId("id-1")
                .setType("POST_TELEMETRY")
                .setEntityType("DEVICE")
                .setData("{\"temperature\":23.5}")
                .build();

        TbMsgProto decoded = TbMsgProto.parseFrom(message.toByteArray());

        assertEquals("id-1", decoded.getId());
        assertEquals("POST_TELEMETRY", decoded.getType());
        assertEquals("DEVICE", decoded.getEntityType());
        assertEquals("{\"temperature\":23.5}", decoded.getData());
        assertEquals(1, decodedTelemetry.getTsKvListCount());
        assertEquals(23.5, decodedTelemetry.getTsKvList(0).getKv(0).getDoubleV());
    }
}
