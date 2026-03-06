package de.devknochen.descriptive.server.network;

import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import de.devknochen.descriptive.common.network.packet.ServerStatusPayload;
import de.devknochen.descriptive.config.DescriptiveServerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

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
                FabricLoader.getInstance().getConfigDir().resolve("descriptive-server.toml")
        );
        config.load();

        ServerPlayNetworking.registerGlobalReceiver(CustomNameData.ID, (payload, context) -> {
            if (!config.isEnabled()) return;
            MinecraftServer server = context.server();
            server.execute(() -> {
                serverCache.put(payload.playerUuid(), payload);
                for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
                    if (serverPlayer.getUuid().equals(payload.playerUuid())) continue;
                    try {
                        ServerPlayNetworking.send(serverPlayer, payload);
                    } catch (Exception e) {
                        Descriptive.LOGGER.error("[SERVER] Failed to relay packet to player", e);
                    }
                }
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID joiningUuid = handler.player.getUuid();
            server.execute(() -> {
                try {
                    ServerPlayNetworking.send(handler.player, new ServerStatusPayload(config.isEnabled()));
                } catch (Exception e) {
                    Descriptive.LOGGER.error("[SERVER] Failed to send status payload on join", e);
                }

                if (!config.isEnabled()) return;

                for (Map.Entry<UUID, CustomNameData> entry : serverCache.entrySet()) {
                    if (entry.getKey().equals(joiningUuid)) continue;
                    try {
                        ServerPlayNetworking.send(handler.player, entry.getValue());
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