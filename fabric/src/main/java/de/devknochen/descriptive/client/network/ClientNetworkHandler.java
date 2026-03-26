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

package de.devknochen.descriptive.client.network;

import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.client.DescriptiveClient;
import de.devknochen.descriptive.client.network.relay.RelayClient;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import de.devknochen.descriptive.common.network.packet.ServerStatusPayload;
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientNetworkHandler {

    private static RelayClient relayClient;
    private static boolean usingRelay = false;

    @SuppressWarnings("resource")
    public static void initialize() {
        Descriptive.LOGGER.info("Initializing ClientNetworkHandler");
        relayClient = new RelayClient();

        ClientPlayNetworking.registerGlobalReceiver(CustomNameData.TYPE, (payload, context) -> {
            if (payload.protocolVersion() != CustomNameData.CURRENT_PROTOCOL_VERSION) {
                Descriptive.LOGGER.warn("Incompatible protocol version: {} (expected {})",
                        payload.protocolVersion(), CustomNameData.CURRENT_PROTOCOL_VERSION);
                return;
            }
            Minecraft client = context.client();
            if (client.player != null && client.player.getUUID().equals(payload.playerUuid())) return;
            client.execute(() -> CustomNameCache.put(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(ServerStatusPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    ServerStatusCache.setServerAllowsDescriptive(payload.enabled());
                    if (!payload.enabled()) {
                        // gui.getChat().addMessage() is private in 26.1 — use sendSystemMessage instead
                        if (context.client().player != null) {
                            context.client().player.sendSystemMessage(
                                    Component.literal("§7[Descriptive] §cThis server has disabled custom name display. Only your own name is animated.")
                            );
                        }
                    }
                }));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Descriptive.LOGGER.info("Client joined server");
            String serverAddress = handler.getConnection().getRemoteAddress().toString();
            client.execute(() -> { if (client.player != null) detectModeAndBroadcast(client, serverAddress); });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Descriptive.LOGGER.info("Disconnected from server");
            if (usingRelay) { relayClient.disconnect(); usingRelay = false; }
            CustomNameCache.clear();
            ServerStatusCache.reset();
        });

        Descriptive.LOGGER.info("ClientNetworkHandler initialized");
    }

    private static void detectModeAndBroadcast(Minecraft client, String serverAddress) {
        if (client.isSingleplayer() || client.getSingleplayerServer() != null
                || ClientPlayNetworking.canSend(CustomNameData.TYPE)) {
            Descriptive.LOGGER.info("Using DIRECT mode");
            usingRelay = false;
            broadcastDirect(client);
        } else {
            Descriptive.LOGGER.info("Using RELAY mode");
            usingRelay = true;
            connectToRelay(client, serverAddress);
        }
    }

    private static CustomNameData buildCustomNameData(Minecraft client) {
        assert client.player != null;
        DescriptiveClientConfig config = DescriptiveClient.getInstance().getConfig();
        return CustomNameData.create(
                client.player.getUUID(),
                config.getColor(), config.isBold(), config.isItalic(),
                config.isUnderlined(), config.isStrikethrough(),
                config.getAnimationTypes(), config.getAnimationSpeed(),
                config.isAnimationEnabled(), config.getGradientColors()
        );
    }

    private static void broadcastDirect(Minecraft client) {
        if (client.player == null) return;
        try { ClientPlayNetworking.send(buildCustomNameData(client)); }
        catch (Exception e) { Descriptive.LOGGER.error("Failed to send packet", e); }
    }

    private static void connectToRelay(Minecraft client, String serverAddress) {
        if (client.player == null) return;
        var playerUuid = client.player.getUUID();
        relayClient.connect(serverAddress, playerUuid).thenAccept(success -> {
            if (success && client.player != null) broadcastViaRelay(client);
            else if (!success) Descriptive.LOGGER.warn("Failed to connect to relay");
        });
    }

    private static void broadcastViaRelay(Minecraft client) {
        if (client.player == null) return;
        relayClient.broadcastCustomName(buildCustomNameData(client));
    }

    public static void updateCustomName() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) return;
        if (usingRelay) broadcastViaRelay(client);
        else broadcastDirect(client);
    }

    public static boolean isUsingRelay() { return usingRelay; }
}