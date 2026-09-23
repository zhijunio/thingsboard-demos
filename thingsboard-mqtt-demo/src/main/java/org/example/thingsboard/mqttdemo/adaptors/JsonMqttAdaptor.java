package org.example.thingsboard.mqttdemo.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.example.thingsboard.mqttdemo.MqttDeviceAwareSessionContext;
import org.example.thingsboard.mqttdemo.proto.TransportProtos;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 对应源码中的 JsonMqttAdaptor。 */
public final class JsonMqttAdaptor implements MqttTransportAdaptor {

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx,
                                                                    MqttPublishMessage inbound)
            throws AdaptorException {
        return convertToPostTelemetry(parsePayload(inbound));
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx,
                                                                      MqttPublishMessage inbound)
            throws AdaptorException {
        return convertToPostAttributes(parsePayload(inbound));
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx,
                                                                               MqttPublishMessage inbound,
                                                                               String topicBase)
            throws AdaptorException {
        return TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                .setRequestId(requestId(inbound, topicBase))
                .setPayload(payloadText(inbound))
                .build();
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx,
                                                                            MqttPublishMessage inbound,
                                                                            String topicBase)
            throws AdaptorException {
        return convertToServerRpcRequest(parsePayload(inbound), requestId(inbound, topicBase));
    }

    /** 对应源码 JsonConverter.convertToTelemetryProto。 */
    public static TransportProtos.PostTelemetryMsg convertToPostTelemetry(JsonElement payload)
            throws AdaptorException {
        TransportProtos.PostTelemetryMsg.Builder result = TransportProtos.PostTelemetryMsg.newBuilder();
        if (payload.isJsonObject()) {
            addTelemetryObject(result, payload.getAsJsonObject());
        } else if (payload.isJsonArray()) {
            for (JsonElement item : payload.getAsJsonArray()) {
                if (!item.isJsonObject()) {
                    throw new AdaptorException("telemetry array item must be an object");
                }
                addTelemetryObject(result, item.getAsJsonObject());
            }
        } else {
            throw new AdaptorException("telemetry payload must be an object or array");
        }
        return result.build();
    }

    /** 对应源码 JsonConverter.convertToAttributesProto。 */
    public static TransportProtos.PostAttributeMsg convertToPostAttributes(JsonElement payload)
            throws AdaptorException {
        if (!payload.isJsonObject()) {
            throw new AdaptorException("attributes payload must be an object");
        }
        return TransportProtos.PostAttributeMsg.newBuilder()
                .addAllKv(toKeyValues(payload.getAsJsonObject()))
                .build();
    }

    /** 对应源码 JsonConverter.convertToServerRpcRequest。 */
    public static TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(JsonElement payload,
                                                                                    int requestId)
            throws AdaptorException {
        if (!payload.isJsonObject()) {
            throw new AdaptorException("RPC request payload must be an object");
        }
        JsonObject object = payload.getAsJsonObject();
        JsonElement method = object.get("method");
        JsonElement params = object.get("params");
        if (method == null || params == null) {
            throw new AdaptorException("RPC request requires method and params");
        }
        return TransportProtos.ToServerRpcRequestMsg.newBuilder()
                .setRequestId(requestId)
                .setMethodName(method.getAsString())
                .setParams(params.toString())
                .build();
    }

    private static void addTelemetryObject(TransportProtos.PostTelemetryMsg.Builder result,
                                           JsonObject object) throws AdaptorException {
        long timestamp = System.currentTimeMillis();
        JsonObject values = object;
        if (object.has("ts") && object.has("values")) {
            timestamp = object.get("ts").getAsLong();
            if (!object.get("values").isJsonObject()) {
                throw new AdaptorException("telemetry values must be an object");
            }
            values = object.getAsJsonObject("values");
        }
        result.addTsKvList(TransportProtos.TsKvListProto.newBuilder()
                .setTs(timestamp)
                .addAllKv(toKeyValues(values))
                .build());
    }

    private static List<TransportProtos.KeyValueProto> toKeyValues(JsonObject object) {
        List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        for (var entry : object.entrySet()) {
            if (entry.getValue().isJsonNull()) {
                continue;
            }
            JsonElement value = entry.getValue();
            TransportProtos.KeyValueProto.Builder item = TransportProtos.KeyValueProto.newBuilder()
                    .setKey(entry.getKey());
            if (value.isJsonObject() || value.isJsonArray()) {
                item.setType(TransportProtos.KeyValueType.JSON_V).setJsonV(value.toString());
            } else {
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    item.setType(TransportProtos.KeyValueType.BOOLEAN_V).setBoolV(primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    setNumeric(item, primitive.getAsString());
                } else {
                    setStringOrNumeric(item, primitive.getAsString());
                }
            }
            result.add(item.build());
        }
        return result;
    }

    private static void setStringOrNumeric(TransportProtos.KeyValueProto.Builder item, String value) {
        try {
            setNumeric(item, value);
        } catch (RuntimeException ignored) {
            item.setType(TransportProtos.KeyValueType.STRING_V).setStringV(value);
        }
    }

    private static void setNumeric(TransportProtos.KeyValueProto.Builder item, String value) {
        BigDecimal number = new BigDecimal(value);
        if (number.stripTrailingZeros().scale() <= 0 && !value.contains(".")) {
            item.setType(TransportProtos.KeyValueType.LONG_V).setLongV(number.longValueExact());
        } else {
            item.setType(TransportProtos.KeyValueType.DOUBLE_V).setDoubleV(number.doubleValue());
        }
    }

    private static JsonElement parsePayload(MqttPublishMessage inbound) throws AdaptorException {
        try {
            return JsonParser.parseString(payloadText(inbound));
        } catch (RuntimeException e) {
            throw new AdaptorException("invalid JSON payload", e);
        }
    }

    private static String payloadText(MqttPublishMessage inbound) {
        return inbound.payload().toString(StandardCharsets.UTF_8);
    }

    private static int requestId(MqttPublishMessage inbound, String topicBase) throws AdaptorException {
        String topic = inbound.variableHeader().topicName();
        if (!topic.startsWith(topicBase)) {
            throw new AdaptorException("topic does not start with " + topicBase);
        }
        try {
            return Integer.parseInt(topic.substring(topicBase.length()));
        } catch (NumberFormatException e) {
            throw new AdaptorException("invalid RPC request id", e);
        }
    }
}
