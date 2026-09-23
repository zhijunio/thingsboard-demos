package org.example.thingsboard.websocketdemo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.UUID;

/** 对应 ThingsBoard telemetry WebSocket 的 v1 命令结构。 */
public final class TelemetryCommandBuilder {
    private TelemetryCommandBuilder() {
    }

    public static String latestTelemetry(UUID deviceId, String keys, int cmdId) {
        JsonObject command = new JsonObject();
        command.addProperty("entityType", "DEVICE");
        command.addProperty("entityId", deviceId.toString());
        command.addProperty("scope", "LATEST_TELEMETRY");
        command.addProperty("keys", keys);
        command.addProperty("cmdId", cmdId);
        return wrap("tsSubCmds", command);
    }

    public static String attributes(UUID deviceId, String keys, int cmdId) {
        JsonObject command = new JsonObject();
        command.addProperty("entityType", "DEVICE");
        command.addProperty("entityId", deviceId.toString());
        command.addProperty("scope", "CLIENT_SCOPE");
        command.addProperty("keys", keys);
        command.addProperty("cmdId", cmdId);
        return wrap("attrSubCmds", command);
    }

    public static String unsubscribe(int cmdId) {
        JsonObject command = new JsonObject();
        command.addProperty("cmdId", cmdId);
        command.addProperty("unsubscribe", true);
        return wrap("tsSubCmds", command);
    }

    private static String wrap(String field, JsonObject command) {
        JsonArray commands = new JsonArray();
        commands.add(command);
        JsonObject wrapper = new JsonObject();
        wrapper.add(field, commands);
        return wrapper.toString();
    }
}
