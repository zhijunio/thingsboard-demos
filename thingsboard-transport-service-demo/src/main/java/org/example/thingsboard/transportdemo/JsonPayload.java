package org.example.thingsboard.transportdemo;

import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;

/** 对应 DefaultTransportService 中 JsonUtils + Gson 的最小 JSON 转换。 */
final class JsonPayload {
    private JsonPayload() {
    }

    static String toJson(List<TransportProtos.KeyValueProto> values) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            TransportProtos.KeyValueProto value = values.get(i);
            json.append(quote(value.getKey())).append(':').append(value(value));
        }
        return json.append('}').toString();
    }

    private static String value(TransportProtos.KeyValueProto value) {
        return switch (value.getType()) {
            case BOOLEAN_V -> Boolean.toString(value.getBoolV());
            case LONG_V -> Long.toString(value.getLongV());
            case DOUBLE_V -> Double.toString(value.getDoubleV());
            case JSON_V -> value.getJsonV();
            case STRING_V, UNRECOGNIZED -> quote(value.getStringV());
        };
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
