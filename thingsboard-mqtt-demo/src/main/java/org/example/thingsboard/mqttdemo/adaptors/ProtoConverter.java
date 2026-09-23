package org.example.thingsboard.mqttdemo.adaptors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.List;
import java.util.Map;

/** 精简自 ThingsBoard ProtoConverter，保留 descriptor -> DynamicMessage -> JSON 的路径。 */
public final class ProtoConverter {

    private ProtoConverter() {
    }

    public static JsonElement dynamicMsgToJson(byte[] bytes, Descriptors.Descriptor descriptor)
            throws InvalidProtocolBufferException {
        if (descriptor == null) {
            throw new IllegalArgumentException("protobuf message descriptor is missing");
        }
        return toJson(DynamicMessage.parseFrom(descriptor, bytes));
    }

    private static JsonObject toJson(DynamicMessage message) {
        JsonObject result = new JsonObject();
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor field = entry.getKey();
            Object value = entry.getValue();
            if (field.isRepeated()) {
                JsonArray array = new JsonArray();
                for (Object item : (List<?>) value) {
                    array.add(toJsonValue(field, item));
                }
                result.add(field.getJsonName(), array);
            } else {
                result.add(field.getJsonName(), toJsonValue(field, value));
            }
        }
        return result;
    }

    private static JsonElement toJsonValue(Descriptors.FieldDescriptor field, Object value) {
        return switch (field.getJavaType()) {
            case BOOLEAN -> new JsonPrimitive((Boolean) value);
            case INT, LONG -> new JsonPrimitive((Number) value);
            case FLOAT, DOUBLE -> new JsonPrimitive((Number) value);
            case STRING -> new JsonPrimitive((String) value);
            case ENUM -> new JsonPrimitive(((Descriptors.EnumValueDescriptor) value).getName());
            case BYTE_STRING -> new JsonPrimitive(value.toString());
            case MESSAGE -> toJson((DynamicMessage) value);
        };
    }
}
