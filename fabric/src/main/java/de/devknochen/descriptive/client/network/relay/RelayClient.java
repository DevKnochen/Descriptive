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

package de.devknochen.descriptive.client.network.relay;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.client.DescriptiveClient;
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RelayClient implements WebSocket.Listener {

    private static final String RELAY_SERVER_URL       = "wss://relay-descriptive.knochenn.de/sync";
    private static final String RELAY_PROTOCOL_VERSION = "A3.1";
    private static final Gson   GSON                   = new Gson();

    private WebSocket webSocket;
    private String    currentServer;
    private UUID      localPlayerUuid;
    private boolean   connected = false;

    private static boolean isRelayDisabled() {
        return !DescriptiveClient.getInstance().getConfig().isRelayEnabled();
    }

    public CompletableFuture<Boolean> connect(String serverAddress, UUID playerUuid) {
        if (isRelayDisabled()) return CompletableFuture.completedFuture(false);
        if (connected) disconnect();
        this.currentServer   = serverAddress;
        this.localPlayerUuid = playerUuid;

        @SuppressWarnings("resource")
        HttpClient client = HttpClient.newHttpClient();
        return client.newWebSocketBuilder()
                .buildAsync(URI.create(RELAY_SERVER_URL), this)
                .thenApply(ws -> { this.webSocket = ws; this.connected = true; sendJoinMessage(); return true; })
                .exceptionally(t -> { Descriptive.LOGGER.error("[RELAY] Failed to connect", t); return false; });
    }

    public void disconnect() {
        if (webSocket != null && connected) {
            sendLeaveMessage();
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
            connected = false;
        }
    }

    public void broadcastCustomName(CustomNameData data) {
        if (isRelayDisabled() || !connected || webSocket == null) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type",             "custom_name");
        msg.addProperty("server",           currentServer);
        msg.addProperty("playerUuid",       data.playerUuid().toString());
        msg.addProperty("color",            data.color());
        msg.addProperty("bold",             data.bold());
        msg.addProperty("italic",           data.italic());
        msg.addProperty("underlined",       data.underlined());
        msg.addProperty("strikethrough",    data.strikethrough());
        msg.addProperty("animationEnabled", data.animationEnabled());
        JsonArray animArray = new JsonArray();
        for (String a : data.animationTypes()) animArray.add(a);
        msg.add("animationTypes", animArray);
        msg.addProperty("animationSpeed", data.animationSpeed());
        JsonArray gradArray = new JsonArray();
        for (int c : data.gradientColors()) gradArray.add(c);
        msg.add("gradientColors", gradArray);
        webSocket.sendText(GSON.toJson(msg), true);
    }

    private void sendJoinMessage() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "join"); msg.addProperty("server", currentServer);
        msg.addProperty("playerUuid", localPlayerUuid.toString()); msg.addProperty("version", RELAY_PROTOCOL_VERSION);
        webSocket.sendText(GSON.toJson(msg), true);
    }

    private void sendLeaveMessage() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "leave"); msg.addProperty("server", currentServer);
        msg.addProperty("playerUuid", localPlayerUuid.toString());
        webSocket.sendText(GSON.toJson(msg), true);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            JsonObject msg = GSON.fromJson(data.toString(), JsonObject.class);
            String type = msg.has("type") ? msg.get("type").getAsString() : "";
            switch (type) {
                case "custom_name"      -> handleCustomName(msg);
                case "kicked"           -> handleKicked(msg);
                case "banned"           -> handleBanned(msg);
                case "server_blocked"   -> handleServerBlocked(msg);
                case "version_rejected" -> handleVersionRejected();
                case "blocked"          -> handleLegacyBlocked(msg);
            }
        } catch (Exception e) { Descriptive.LOGGER.error("[RELAY] Error processing message", e); }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        connected = false;
        Descriptive.LOGGER.info("[RELAY] Connection closed (code={}, reason={})", statusCode, reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Descriptive.LOGGER.error("[RELAY] WebSocket error", error);
        connected = false;
    }

    private void handleKicked(JsonObject msg) {
        connected = false;
        String reason = msg.has("reason") ? msg.get("reason").getAsString() : "You were kicked by an admin.";
        Descriptive.LOGGER.warn("[RELAY] Kicked: {}", reason);
        sendChatNotice(Component.literal("⚠ You were kicked from the Descriptive relay.")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("\n  Reason: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(reason).withStyle(ChatFormatting.WHITE)));
    }

    private void handleBanned(JsonObject msg) {
        connected = false;
        String reason = msg.has("reason") ? msg.get("reason").getAsString() : "Your IP or account has been banned.";
        Descriptive.LOGGER.warn("[RELAY] Banned: {}", reason);
        sendChatNotice(Component.literal("✖ You are banned from the Descriptive relay.")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal("\n  Reason: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(reason).withStyle(ChatFormatting.WHITE)));
    }

    private void handleServerBlocked(JsonObject msg) {
        String reason = msg.has("reason") ? msg.get("reason").getAsString() : "This server is not permitted on the relay.";
        Descriptive.LOGGER.warn("[RELAY] Server blocked: {}", reason);
        sendChatNotice(Component.literal("⚠ Descriptive relay is unavailable on this server.")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("\n  " + reason).withStyle(ChatFormatting.GRAY)));
    }

    private void handleVersionRejected() {
        connected = false;
        Descriptive.LOGGER.warn("[RELAY] Version rejected by relay.");
        sendChatNotice(Component.literal("✖ Your Descriptive mod is outdated.")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal("\n  Please update the mod to continue using the relay.").withStyle(ChatFormatting.GRAY)));
    }

    private void handleLegacyBlocked(JsonObject msg) {
        String reason = msg.has("reason") ? msg.get("reason").getAsString() : "unknown";
        switch (reason) {
            case "client_ip" -> handleBanned(msg);
            case "server_ip" -> handleServerBlocked(msg);
            default -> { connected = false; sendChatNotice(Component.literal("✖ Blocked by Descriptive relay: " + reason).withStyle(ChatFormatting.RED)); }
        }
    }

    private void handleCustomName(JsonObject msg) {
        try {
            UUID playerUuid = UUID.fromString(msg.get("playerUuid").getAsString());
            if (playerUuid.equals(localPlayerUuid)) return;
            List<String> animTypes = new ArrayList<>();
            float animSpeed = 1.0f; boolean animEnabled = false;
            List<Integer> gradColors = new ArrayList<>(List.of(0xFF0000, 0x0000FF));
            if (msg.has("animationEnabled")) {
                animEnabled = msg.get("animationEnabled").getAsBoolean();
                if (msg.has("animationTypes"))
                    for (JsonElement e : msg.getAsJsonArray("animationTypes")) animTypes.add(e.getAsString());
                if (msg.has("animationSpeed")) animSpeed = msg.get("animationSpeed").getAsFloat();
            }
            if (msg.has("gradientColors")) {
                gradColors = new ArrayList<>();
                for (JsonElement e : msg.getAsJsonArray("gradientColors")) gradColors.add(e.getAsInt());
            }
            CustomNameCache.put(CustomNameData.create(
                    playerUuid, msg.get("color").getAsInt(),
                    msg.get("bold").getAsBoolean(), msg.get("italic").getAsBoolean(),
                    msg.get("underlined").getAsBoolean(), msg.get("strikethrough").getAsBoolean(),
                    animTypes, animSpeed, animEnabled, gradColors));
        } catch (Exception e) { Descriptive.LOGGER.error("[RELAY] Error handling custom_name", e); }
    }

    private static void sendChatNotice(MutableComponent content) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.literal("[Descriptive] ").withStyle(ChatFormatting.DARK_GRAY).append(content));
            }
        });
    }
}