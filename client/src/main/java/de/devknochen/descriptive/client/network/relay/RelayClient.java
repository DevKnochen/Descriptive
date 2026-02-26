/*
 * Copyright 2026 DevKnochen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
// AT THIS STAGE, THIS CODE IS NOT FUNCTIONAL AND ONLY A PLACEHOLDER FOR A FUTURE FUNCTION
//

package de.devknochen.descriptive.client.network.relay;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.common.network.packet.CustomNameData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RelayClient implements WebSocket.Listener {

    private static final String RELAY_SERVER_URL = "wss://relay-descriptive.knochenn.de/sync";
    private static final Gson GSON = new Gson();

    private WebSocket webSocket;
    private String currentServer;
    private UUID localPlayerUuid;
    private boolean connected = false;

    public CompletableFuture<Boolean> connect(String serverAddress, UUID playerUuid) {
        if (connected) disconnect();

        this.currentServer = serverAddress;
        this.localPlayerUuid = playerUuid;

        Descriptive.LOGGER.info("[RELAY] Connecting to relay server...");

        @SuppressWarnings("resource")
        HttpClient client = HttpClient.newHttpClient();

        return client.newWebSocketBuilder()
                .buildAsync(URI.create(RELAY_SERVER_URL), this)
                .thenApply(ws -> {
                    this.webSocket = ws;
                    this.connected = true;
                    sendJoinMessage();
                    return true;
                })
                .exceptionally(throwable -> {
                    Descriptive.LOGGER.error("[RELAY] Failed to connect", throwable);
                    return false;
                });
    }

    public void disconnect() {
        if (webSocket != null && connected) {
            sendLeaveMessage();
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
            connected = false;
        }
    }

    public void broadcastCustomName(CustomNameData data) {
        if (!connected || webSocket == null) {
            return;
        }

        JsonObject message = new JsonObject();
        message.addProperty("type", "custom_name");
        message.addProperty("server", currentServer);
        message.addProperty("playerUuid", data.playerUuid().toString());
        message.addProperty("color", data.color());
        message.addProperty("bold", data.bold());
        message.addProperty("italic", data.italic());
        message.addProperty("underlined", data.underlined());
        message.addProperty("strikethrough", data.strikethrough());
        message.addProperty("animationEnabled", data.animationEnabled());

        JsonArray animArray = new JsonArray();
        for (String anim : data.animationTypes()) animArray.add(anim);
        message.add("animationTypes", animArray);
        message.addProperty("animationSpeed", data.animationSpeed());

        JsonArray gradArray = new JsonArray();
        for (int c : data.gradientColors()) gradArray.add(c);
        message.add("gradientColors", gradArray);

        webSocket.sendText(GSON.toJson(message), true);
    }

    private void sendJoinMessage() {
        JsonObject message = new JsonObject();
        message.addProperty("type", "join");
        message.addProperty("server", currentServer);
        message.addProperty("playerUuid", localPlayerUuid.toString());
        webSocket.sendText(GSON.toJson(message), true);
    }

    private void sendLeaveMessage() {
        JsonObject message = new JsonObject();
        message.addProperty("type", "leave");
        message.addProperty("server", currentServer);
        message.addProperty("playerUuid", localPlayerUuid.toString());
        webSocket.sendText(GSON.toJson(message), true);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            JsonObject message = GSON.fromJson(data.toString(), JsonObject.class);
            if ("custom_name".equals(message.get("type").getAsString())) {
                handleCustomNameMessage(message);
            }
        } catch (Exception e) {
            Descriptive.LOGGER.error("[RELAY] Error processing message", e);
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        connected = false;
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Descriptive.LOGGER.error("[RELAY] WebSocket error", error);
        connected = false;
    }

    private void handleCustomNameMessage(JsonObject message) {
        try {
            UUID playerUuid = UUID.fromString(message.get("playerUuid").getAsString());
            if (playerUuid.equals(localPlayerUuid)) return;

            List<String> animationTypes = new ArrayList<>();
            float animationSpeed = 1.0f;
            boolean animationEnabled = false;
            List<Integer> gradientColors = new ArrayList<>(List.of(0xFF0000, 0x0000FF));

            if (message.has("animationEnabled")) {
                animationEnabled = message.get("animationEnabled").getAsBoolean();
                if (message.has("animationTypes")) {
                    for (JsonElement e : message.getAsJsonArray("animationTypes"))
                        animationTypes.add(e.getAsString());
                }
                if (message.has("animationSpeed"))
                    animationSpeed = message.get("animationSpeed").getAsFloat();
            }

            if (message.has("gradientColors")) {
                gradientColors = new ArrayList<>();
                for (JsonElement e : message.getAsJsonArray("gradientColors"))
                    gradientColors.add(e.getAsInt());
            }

            CustomNameCache.put(CustomNameData.create(
                    playerUuid,
                    message.get("color").getAsInt(),
                    message.get("bold").getAsBoolean(),
                    message.get("italic").getAsBoolean(),
                    message.get("underlined").getAsBoolean(),
                    message.get("strikethrough").getAsBoolean(),
                    animationTypes,
                    animationSpeed,
                    animationEnabled,
                    gradientColors
            ));
        } catch (Exception e) {
            Descriptive.LOGGER.error("[RELAY] Error handling custom name", e);
        }
    }
}