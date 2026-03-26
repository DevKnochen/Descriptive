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

package de.devknochen.descriptive.common.util;

import de.devknochen.descriptive.client.DescriptiveClient;
import de.devknochen.descriptive.client.animation.AnimatedStyleMarker;
import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.client.network.ServerStatusCache;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.UUID;

public class NameBuilder {

    public static Component buildCustomName(String originalName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return Component.literal(originalName);

        UUID playerUuid = client.player.getUUID();
        DescriptiveClientConfig config = DescriptiveClient.getInstance().getConfig();

        if (!config.isPlayerEnabled(playerUuid)) return Component.literal(originalName);

        PlayerAnimationContext.setCurrentPlayer(playerUuid);
        return buildStyledName(originalName, playerUuid, config.getColor(), config.isBold(),
                config.isItalic(), config.isUnderlined(), config.isStrikethrough(), true);
    }

    public static Component buildCustomName(UUID playerUuid, String originalName) {
        Minecraft client = Minecraft.getInstance();
        PlayerAnimationContext.setCurrentPlayer(playerUuid);

        if (client.player != null && client.player.getUUID().equals(playerUuid))
            return buildCustomName(originalName);

        if (!ServerStatusCache.isServerAllowsDescriptive()) return Component.literal(originalName);
        if (!DescriptiveClient.getInstance().getConfig().isPlayerEnabled(playerUuid))
            return Component.literal(originalName);

        if (CustomNameCache.has(playerUuid)) {
            CustomNameData data = CustomNameCache.get(playerUuid);
            if (data != null) {
                boolean shouldAnimate = data.animationEnabled() && !data.animationTypes().isEmpty();
                return buildStyledName(originalName, playerUuid, data.color(), data.bold(),
                        data.italic(), data.underlined(), data.strikethrough(), shouldAnimate);
            }
        }
        return Component.literal(originalName);
    }

    public static Component buildPreview(String name, UUID playerUuid, int color, boolean bold,
                                         boolean italic, boolean underlined, boolean strikethrough) {
        return buildStyledName(name, playerUuid, color, bold, italic, underlined, strikethrough, true);
    }

    private static Component buildStyledName(String name, UUID playerUuid, int color, boolean bold,
                                             boolean italic, boolean underlined, boolean strikethrough,
                                             boolean animate) {
        MutableComponent text = Component.literal(name);
        Style style = Style.EMPTY
                .withColor(TextColor.fromRgb(color))
                .withBold(bold)
                .withItalic(italic)
                .withUnderlined(underlined)
                .withStrikethrough(strikethrough);

        if (animate && playerUuid != null)
            style = AnimatedStyleMarker.mark(style, playerUuid);

        text.setStyle(style);
        return text;
    }
}