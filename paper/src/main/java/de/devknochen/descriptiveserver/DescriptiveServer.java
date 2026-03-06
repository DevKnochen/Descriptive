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

package de.devknochen.descriptiveserver;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DescriptiveServer extends JavaPlugin implements Listener {

    private final Map<UUID, DescriptivePayload> cache = new ConcurrentHashMap<>();
    private ServerConfig config;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        config = new ServerConfig(getDataFolder().toPath().resolve("descriptive-server.json"));
        config.load();

        PacketEvents.getAPI().init();

        getServer().getMessenger().registerIncomingPluginChannel(this, DescriptivePayload.CHANNEL,
                (channel, player, message) -> {});
        getServer().getMessenger().registerOutgoingPluginChannel(this, DescriptivePayload.CHANNEL);
        getServer().getMessenger().registerOutgoingPluginChannel(this, ServerStatusPacket.CHANNEL);

        PacketEvents.getAPI().getEventManager().registerListener(new DescriptivePacketListener());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        getServer().getScheduler().runTaskLater(this, () -> {
            sendStatusPacket(joining, config.isEnabled());
            if (!config.isEnabled()) return;
            for (Map.Entry<UUID, DescriptivePayload> entry : cache.entrySet()) {
                if (entry.getKey().equals(joining.getUniqueId())) continue;
                sendPayload(joining, entry.getValue());
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    private void sendPayload(Player target, DescriptivePayload payload) {
        try {
            WrapperPlayServerPluginMessage packet =
                    new WrapperPlayServerPluginMessage(DescriptivePayload.CHANNEL, payload.encode());
            PacketEvents.getAPI().getPlayerManager().sendPacket(target, packet);
        } catch (Exception e) {
            getLogger().severe("Failed to send payload to " + target.getName() + ": " + e.getMessage());
        }
    }

    private void sendStatusPacket(Player target, boolean enabled) {
        try {
            WrapperPlayServerPluginMessage packet =
                    new WrapperPlayServerPluginMessage(ServerStatusPacket.CHANNEL,
                            ServerStatusPacket.encode(enabled));
            PacketEvents.getAPI().getPlayerManager().sendPacket(target, packet);
        } catch (Exception e) {
            getLogger().severe("Failed to send status packet to " + target.getName() + ": " + e.getMessage());
        }
    }

    private class DescriptivePacketListener extends PacketListenerAbstract {

        public DescriptivePacketListener() {
            super(PacketListenerPriority.NORMAL);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            if (!wrapper.getChannelName().equals(DescriptivePayload.CHANNEL)) return;
            if (!config.isEnabled()) return;

            DescriptivePayload payload = DescriptivePayload.decode(wrapper.getData());
            if (payload == null) {
                getLogger().warning("Received malformed packet, dropping");
                return;
            }

            UUID senderUuid = event.getUser().getUUID();
            cache.put(senderUuid, payload);

            getServer().getScheduler().runTask(DescriptiveServer.this, () -> {
                for (Player target : getServer().getOnlinePlayers()) {
                    if (target.getUniqueId().equals(senderUuid)) continue;
                    sendPayload(target, payload);
                }
            });
        }
    }
}
