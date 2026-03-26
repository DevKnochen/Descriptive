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
import de.devknochen.descriptive.common.network.packet.ServerStatusPayload;
import de.devknochen.descriptive.config.DescriptiveServerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("resource")
public class ServerNetworkHandler {

    private static final Map<UUID, CustomNameData> serverCache = new ConcurrentHashMap<>();
    private static DescriptiveServerConfig config;
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        config = new DescriptiveServerConfig(
                FabricLoader.getInstance().getConfigDir().resolve("descriptive-server.toml"));
        config.load();

        ServerPlayNetworking.registerGlobalReceiver(CustomNameData.TYPE, (payload, context) -> {
            if (!config.isEnabled()) return;
            MinecraftServer server = context.server();
            server.execute(() -> {
                serverCache.put(payload.playerUuid(), payload);
                for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
                    if (serverPlayer.getUUID().equals(payload.playerUuid())) continue;
                    try { ServerPlayNetworking.send(serverPlayer, payload); }
                    catch (Exception e) { Descriptive.LOGGER.error("[SERVER] Failed to relay packet to player", e); }
                }
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID joiningUuid = handler.player.getUUID();
            server.execute(() -> {
                try { ServerPlayNetworking.send(handler.player, new ServerStatusPayload(config.isEnabled())); }
                catch (Exception e) { Descriptive.LOGGER.error("[SERVER] Failed to send status payload on join", e); }

                if (!config.isEnabled()) return;

                for (Map.Entry<UUID, CustomNameData> entry : serverCache.entrySet()) {
                    if (entry.getKey().equals(joiningUuid)) continue;
                    try { ServerPlayNetworking.send(handler.player, entry.getValue()); }
                    catch (Exception e) { Descriptive.LOGGER.error("[SERVER] Failed to send cached name on join", e); }
                }
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                serverCache.remove(handler.player.getUUID()));
    }
}