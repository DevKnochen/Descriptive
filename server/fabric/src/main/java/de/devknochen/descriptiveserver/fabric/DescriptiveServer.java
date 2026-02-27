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

package de.devknochen.descriptiveserver.fabric;

import de.devknochen.descriptiveserver.common.DescriptivePayload;
import de.devknochen.descriptiveserver.common.ServerConfig;
import de.devknochen.descriptiveserver.common.ServerStatusPacket;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("resource")
public class DescriptiveServer implements DedicatedServerModInitializer {

    private static final Map<UUID, FabricPayload> cache = new ConcurrentHashMap<>();
    private static ServerConfig config;

    @Override
    public void onInitializeServer() {
        config = new ServerConfig(
                FabricLoader.getInstance().getConfigDir().resolve("descriptive-server.json")
        );
        config.load();

        PayloadTypeRegistry.playC2S().register(FabricPayload.ID, FabricPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricPayload.ID, FabricPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatusPayload.ID, StatusPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(FabricPayload.ID, (payload, context) -> {
            if (!config.isEnabled()) return;
            UUID senderUuid = context.player().getUuid();
            MinecraftServer server = context.server();
            server.execute(() -> {
                cache.put(senderUuid, payload);
                for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
                    if (target.getUuid().equals(senderUuid)) continue;
                    ServerPlayNetworking.send(target, payload);
                }
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID joiningUuid = handler.player.getUuid();
            server.execute(() -> {
                ServerPlayNetworking.send(handler.player, new StatusPayload(config.isEnabled()));
                if (!config.isEnabled()) return;
                for (Map.Entry<UUID, FabricPayload> entry : cache.entrySet()) {
                    if (entry.getKey().equals(joiningUuid)) continue;
                    ServerPlayNetworking.send(handler.player, entry.getValue());
                }
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                cache.remove(handler.player.getUuid()));
    }

    public record FabricPayload(
            UUID playerUuid, int color, boolean bold, boolean italic,
            boolean underlined, boolean strikethrough,
            List<String> animationTypes, float animationSpeed, boolean animationEnabled,
            List<Integer> gradientColors
    ) implements CustomPayload {

        public static final Identifier RAW_ID = Identifier.of("descriptive", "sync");
        public static final Id<FabricPayload> ID = new Id<>(RAW_ID);

        public static final PacketCodec<PacketByteBuf, FabricPayload> CODEC = new PacketCodec<>() {
            @Override
            public FabricPayload decode(PacketByteBuf buf) {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                DescriptivePayload p = DescriptivePayload.decode(bytes);
                if (p == null) return null;
                return new FabricPayload(p.playerUuid(), p.color(), p.bold(), p.italic(),
                        p.underlined(), p.strikethrough(), p.animationTypes(), p.animationSpeed(),
                        p.animationEnabled(), p.gradientColors());
            }

            @Override
            public void encode(PacketByteBuf buf, FabricPayload p) {
                buf.writeBytes(new DescriptivePayload(
                        p.playerUuid(), p.color(), p.bold(), p.italic(),
                        p.underlined(), p.strikethrough(), p.animationTypes(), p.animationSpeed(),
                        p.animationEnabled(), p.gradientColors()).encode());
            }
        };

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record StatusPayload(boolean enabled) implements CustomPayload {

        public static final Identifier RAW_ID = Identifier.of("descriptive", "server_status");
        public static final Id<StatusPayload> ID = new Id<>(RAW_ID);

        public static final PacketCodec<PacketByteBuf, StatusPayload> CODEC = new PacketCodec<>() {
            @Override
            public StatusPayload decode(PacketByteBuf buf) {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new StatusPayload(ServerStatusPacket.decode(bytes));
            }

            @Override
            public void encode(PacketByteBuf buf, StatusPayload p) {
                buf.writeBytes(ServerStatusPacket.encode(p.enabled));
            }
        };

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}