package org.example.thingsboard;

import com.google.protobuf.ByteString;
import org.example.thingsboard.proto.KeyValueProto;
import org.example.thingsboard.proto.KeyValueType;
import org.example.thingsboard.proto.PostTelemetryMsg;
import org.example.thingsboard.proto.TbMsgMetaDataProto;
import org.example.thingsboard.proto.TbMsgProto;
import org.example.thingsboard.proto.TsKvListProto;

import java.util.HexFormat;

public final class ProtobufDemo {

    private ProtobufDemo() {
    }

    public static void main(String[] args) throws Exception {
        PostTelemetryMsg telemetry = PostTelemetryMsg.newBuilder()
                .addTsKvList(TsKvListProto.newBuilder()
                        .setTs(1_725_000_000_000L)
                        .addKv(KeyValueProto.newBuilder()
                                .setKey("temperature")
                                .setType(KeyValueType.DOUBLE_V)
                                .setDoubleV(23.5))
                        .addKv(KeyValueProto.newBuilder()
                                .setKey("humidity")
                                .setType(KeyValueType.LONG_V)
                                .setLongV(61)))
                .build();

        byte[] encodedTelemetry = telemetry.toByteArray();
        PostTelemetryMsg decodedTelemetry = PostTelemetryMsg.parseFrom(encodedTelemetry);

        // TbMsgProto.data is a string in the ThingsBoard model. In the queue,
        // application code commonly puts the JSON payload in this field.
        String telemetryJson = "{\"temperature\":23.5,\"humidity\":61}";

        TbMsgProto message = TbMsgProto.newBuilder()
                .setId("demo-message-001")
                .setType("POST_TELEMETRY")
                .setEntityType("DEVICE")
                .setEntityIdMSB(0x1234)
                .setEntityIdLSB(0x5678)
                .setMetaData(TbMsgMetaDataProto.newBuilder()
                        .putData("deviceName", "demo-device")
                        .putData("source", "protobuf-demo"))
                .setDataType(1)
                .setData(telemetryJson)
                .setTs(1_725_000_000_000L)
                .build();

        byte[] encodedMessage = message.toByteArray();
        TbMsgProto decodedMessage = TbMsgProto.parseFrom(encodedMessage);
        System.out.println("message type: " + decodedMessage.getType());
        System.out.println("message id: " + decodedMessage.getId());
        System.out.println("device: " + decodedMessage.getMetaData().getDataOrThrow("deviceName"));
        System.out.println("telemetry points: " + decodedTelemetry.getTsKvList(0).getKvCount());
        System.out.println("telemetry bytes: " + encodedTelemetry.length);
        System.out.println("serialized bytes: " + encodedMessage.length);
        System.out.println("wire prefix: " + HexFormat.of().formatHex(
                ByteString.copyFrom(encodedMessage).substring(0, Math.min(24, encodedMessage.length)).toByteArray()));
    }
}
