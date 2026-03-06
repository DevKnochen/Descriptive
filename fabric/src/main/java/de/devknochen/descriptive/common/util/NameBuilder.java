package de.devknochen.descriptive.common.util;

import de.devknochen.descriptive.client.DescriptiveClient;
import de.devknochen.descriptive.client.animation.AnimatedStyleMarker;
import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.client.network.ServerStatusCache;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.UUID;

public class NameBuilder {

    public static Text buildCustomName(String originalName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return Text.literal(originalName);
        }

        UUID playerUuid = client.player.getUuid();
        DescriptiveClientConfig config = DescriptiveClient.getInstance().getConfig();

        // Player has disabled their own Descriptive rendering
        if (!config.isPlayerEnabled(playerUuid)) {
            return Text.literal(originalName);
        }

        PlayerAnimationContext.setCurrentPlayer(playerUuid);

        return buildStyledName(originalName, playerUuid, config.getColor(), config.isBold(),
                config.isItalic(), config.isUnderlined(), config.isStrikethrough(), true);
    }

    public static Text buildCustomName(UUID playerUuid, String originalName) {
        MinecraftClient client = MinecraftClient.getInstance();

        PlayerAnimationContext.setCurrentPlayer(playerUuid);

        // Always apply local player's own style regardless of server setting
        if (client.player != null && client.player.getUuid().equals(playerUuid)) {
            return buildCustomName(originalName);
        }

        // Server has disabled descriptive - return plain name for other players
        if (!ServerStatusCache.isServerAllowsDescriptive()) {
            return Text.literal(originalName);
        }

        // User has disabled this player's Descriptive rendering
        if (!DescriptiveClient.getInstance().getConfig().isPlayerEnabled(playerUuid)) {
            return Text.literal(originalName);
        }

        if (CustomNameCache.has(playerUuid)) {
            CustomNameData data = CustomNameCache.get(playerUuid);
            if (data != null) {
                boolean shouldAnimate = data.animationEnabled() && !data.animationTypes().isEmpty();

                return buildStyledName(originalName, playerUuid, data.color(), data.bold(),
                        data.italic(), data.underlined(), data.strikethrough(), shouldAnimate);
            }
        }

        return Text.literal(originalName);
    }

    public static Text buildPreview(String name, UUID playerUuid, int color, boolean bold,
                                    boolean italic, boolean underlined,
                                    boolean strikethrough) {
        return buildStyledName(name, playerUuid, color, bold, italic, underlined, strikethrough, true);
    }

    private static Text buildStyledName(String name, UUID playerUuid, int color, boolean bold,
                                        boolean italic, boolean underlined,
                                        boolean strikethrough, boolean animate) {
        MutableText nameText = Text.literal(name);

        Style style = Style.EMPTY
                .withColor(net.minecraft.text.TextColor.fromRgb(color))
                .withBold(bold)
                .withItalic(italic)
                .withUnderline(underlined)
                .withStrikethrough(strikethrough);

        if (animate && playerUuid != null) {
            style = AnimatedStyleMarker.mark(style, playerUuid);
        }

        nameText.setStyle(style);
        return nameText;
    }
}