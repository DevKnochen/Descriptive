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

package de.devknochen.descriptive.client.command;

import com.mojang.brigadier.context.CommandContext;
import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.client.DescriptiveClient;
import de.devknochen.descriptive.client.animation.AnimatedStyleMarker;
import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.client.gui.DescriptiveConfigScreen;
import de.devknochen.descriptive.client.network.ClientNetworkHandler;
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.client.network.ServerStatusCache;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import de.devknochen.descriptive.common.util.NameBuilder;
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("unused")
public class DescriptiveCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("descriptive")
                        .then(ClientCommands.literal("config").executes(DescriptiveCommand::openConfig))
                        .then(ClientCommands.literal("debug").executes(DescriptiveCommand::debug))
                        .then(ClientCommands.literal("rebroadcast").executes(DescriptiveCommand::rebroadcast))
                        .then(ClientCommands.literal("test_broadcast").executes(DescriptiveCommand::testBroadcast))
                        .then(ClientCommands.literal("test_render").executes(DescriptiveCommand::testRender))
                        .then(ClientCommands.literal("help").executes(DescriptiveCommand::help))
                        .executes(DescriptiveCommand::help)
                )
        );
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        @Nullable Screen current = client.gui.screen();
        client.execute(() -> client.gui.setScreen(new DescriptiveConfigScreen(current)));
        return 1;
    }

    private static int debug(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = context.getSource().getClient();
        sendMessage(context, "§6=== Descriptive Debug ===");

        if (client.player != null) {
            DescriptiveClientConfig config = DescriptiveClient.getInstance().getConfig();
            sendMessage(context, "§aLocal Player:");
            sendMessage(context, "  Name: §f" + client.player.getName().getString());
            sendMessage(context, "  UUID: §f" + client.player.getUUID());
            sendMessage(context, "  Color: §f#" + String.format("%06X", config.getColor()));
            sendMessage(context, "  Bold: §f" + config.isBold() + " §7| Italic: §f" + config.isItalic() +
                    " §7| Underline: §f" + config.isUnderlined() + " §7| Strike: §f" + config.isStrikethrough());
            sendMessage(context, "");
            sendMessage(context, "§aAnimation:");
            sendMessage(context, "  Speed: §f" + config.getAnimationSpeed() + "x");
            sendMessage(context, "  Types: §f" + config.getAnimationTypes());
        }

        sendMessage(context, "");
        sendMessage(context, "§aNetwork:");
        if (client.getConnection() != null) {
            sendMessage(context, "  Connected: §aYES");
            sendMessage(context, "  Mode: " + getNetworkMode(client));
            sendMessage(context, "  Server allows Descriptive: §f" + ServerStatusCache.isServerAllowsDescriptive());
        } else {
            sendMessage(context, "  Connected: §cNO");
        }

        sendMessage(context, "");
        sendMessage(context, "§aCache: §f" + CustomNameCache.size() + " player(s)");

        if (CustomNameCache.size() > 0) {
            CustomNameCache.getAllEntries().forEach((uuid, data) -> {
                sendMessage(context, "  §f" + uuid);
                sendMessage(context, "    Color: §f#" + String.format("%06X", data.color()));
                sendMessage(context, "    Bold: §f" + data.bold() + " §7| Italic: §f" + data.italic() +
                        " §7| Underline: §f" + data.underlined() + " §7| Strike: §f" + data.strikethrough());
                sendMessage(context, "    Anim Speed: §f" + data.animationSpeed() + "x §7| Types: §f" + data.animationTypes());
            });
        }

        sendMessage(context, "§6========================");
        return 1;
    }

    private static String getNetworkMode(Minecraft client) {
        if (!ServerStatusCache.isServerAllowsDescriptive()) return "§cREJECTED";
        if (client.isLocalServer() || client.getSingleplayerServer() != null) return "§aDIRECT §7(local)";
        if (ClientNetworkHandler.isUsingRelay()) return "§eRELAY";
        return "§aDIRECT";
    }

    private static int rebroadcast(CommandContext<FabricClientCommandSource> context) {
        if (!ServerStatusCache.isServerAllowsDescriptive()) {
            sendMessage(context, "§c✗ Server rejects Descriptive packets. Rebroadcast is not effective.");
            return 0;
        }
        ClientNetworkHandler.updateCustomName();
        sendMessage(context, "§a✓ Re-broadcasted custom name.");
        return 1;
    }

    private static int testBroadcast(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = context.getSource().getClient();
        if (client.player == null) {
            sendMessage(context, "§cNot in game!");
            return 0;
        }

        if (!ServerStatusCache.isServerAllowsDescriptive()) {
            sendMessage(context, "§c✗ Server rejects Descriptive packets. Broadcast is not effective.");
            return 0;
        }

        DescriptiveClientConfig config = DescriptiveClient.getInstance().getConfig();
        CustomNameData data = CustomNameData.create(
                client.player.getUUID(), config.getColor(), config.isBold(), config.isItalic(),
                config.isUnderlined(), config.isStrikethrough(), config.getAnimationTypes(),
                config.getAnimationSpeed(), config.isAnimationEnabled(), config.getGradientColors()
        );

        try {
            if (ClientPlayNetworking.canSend(CustomNameData.TYPE)) {
                ClientPlayNetworking.send(data);
                sendMessage(context, "§a✓ Packet sent successfully!");
            } else {
                sendMessage(context, "§c✗ Cannot send packet (server doesn't support it)");
            }
        } catch (Exception e) {
            sendMessage(context, "§c✗ Error: " + e.getMessage());
            Descriptive.LOGGER.error("Failed to send test packet", e);
        }

        return 1;
    }

    private static int testRender(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = context.getSource().getClient();
        if (client.player == null || client.level == null) {
            sendMessage(context, "§cNot in game!");
            return 0;
        }

        sendMessage(context, "§6=== Render Test ===");

        for (var player : client.level.players()) {
            String playerName = player.getName().getString();
            var playerUuid = player.getUUID();

            boolean inCache = CustomNameCache.has(playerUuid);
            sendMessage(context, "§e" + playerName + " §7- Cache: " + (inCache ? "§aYES" : "§cNO"));

            PlayerAnimationContext.setCurrentPlayer(playerUuid);
            Component customName = (Component) NameBuilder.buildCustomName(playerUuid, playerName);
            boolean isMarked = AnimatedStyleMarker.shouldAnimate(customName.getStyle());
            sendMessage(context, "  Marked: " + (isMarked ? "§aYES" : "§cNO"));

            if (isMarked) {
                var markedUuid = AnimatedStyleMarker.getPlayerForStyle(customName.getStyle());
                sendMessage(context, "  UUID: " + (markedUuid != null && markedUuid.equals(playerUuid) ? "§aCORRECT" : "§cWRONG"));
            }

            PlayerAnimationContext.clear();
        }

        return 1;
    }

    private static int help(CommandContext<FabricClientCommandSource> context) {
        sendMessage(context, "§6=== Descriptive Commands ===");
        sendMessage(context, "§e/descriptive config §7- Open settings");
        sendMessage(context, "§e/descriptive debug §7- Show debug info");
        sendMessage(context, "§e/descriptive rebroadcast §7- Re-broadcast your name");
        sendMessage(context, "§e/descriptive test_broadcast §7- Test packet sending");
        sendMessage(context, "§e/descriptive test_render §7- Test rendering");
        sendMessage(context, "§e/descriptive help §7- Show this help");
        return 1;
    }

    private static void sendMessage(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Component.literal(message));
    }
}
