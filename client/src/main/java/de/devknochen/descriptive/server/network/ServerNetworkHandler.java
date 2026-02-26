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

package de.devknochen.descriptive.server.network;

import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("resource")
public class ServerNetworkHandler {

    private static final Map<UUID, CustomNameData> serverCache = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        ServerPlayNetworking.registerGlobalReceiver(CustomNameData.ID, (payload, context) -> {
            MinecraftServer server = context.server();
            server.execute(() -> {
                serverCache.put(payload.playerUuid(), payload);
                for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
                    try {
                        ServerPlayNetworking.send(serverPlayer, payload);
                    } catch (Exception e) {
                        Descriptive.LOGGER.error("[SERVER] Failed to relay packet to player", e);
                    }
                }
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                for (CustomNameData data : serverCache.values()) {
                    if (data.playerUuid().equals(handler.player.getUuid())) continue;
                    try {
                        ServerPlayNetworking.send(handler.player, data);
                    } catch (Exception e) {
                        Descriptive.LOGGER.error("[SERVER] Failed to send cached name on join", e);
                    }
                }
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                serverCache.remove(handler.player.getUuid()));
    }
}