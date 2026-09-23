package org.example.thingsboard.websocketdemo;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryCommandBuilderTest {
    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void buildsThingsBoardLatestTelemetryCommand() {
        var command = JsonParser.parseString(
                TelemetryCommandBuilder.latestTelemetry(DEVICE_ID, "temperature,humidity", 7))
                .getAsJsonObject();
        var subscription = command.getAsJsonArray("tsSubCmds").get(0).getAsJsonObject();
        assertEquals("DEVICE", subscription.get("entityType").getAsString());
        assertEquals(DEVICE_ID.toString(), subscription.get("entityId").getAsString());
        assertEquals("LATEST_TELEMETRY", subscription.get("scope").getAsString());
        assertEquals("temperature,humidity", subscription.get("keys").getAsString());
        assertEquals(7, subscription.get("cmdId").getAsInt());
    }

    @Test
    void buildsAttributeAndUnsubscribeCommands() {
        var attributes = JsonParser.parseString(
                TelemetryCommandBuilder.attributes(DEVICE_ID, "firmware", 2)).getAsJsonObject();
        assertEquals("CLIENT_SCOPE", attributes.getAsJsonArray("attrSubCmds")
                .get(0).getAsJsonObject().get("scope").getAsString());

        var unsubscribe = JsonParser.parseString(TelemetryCommandBuilder.unsubscribe(2)).getAsJsonObject();
        assertTrue(unsubscribe.getAsJsonArray("tsSubCmds").get(0).getAsJsonObject()
                .get("unsubscribe").getAsBoolean());
    }
}
