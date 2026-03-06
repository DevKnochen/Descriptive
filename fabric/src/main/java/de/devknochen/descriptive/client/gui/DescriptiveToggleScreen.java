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

package de.devknochen.descriptive.client.gui;

import de.devknochen.descriptive.client.DescriptiveClient;
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.common.util.NameBuilder;
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DescriptiveToggleScreen extends Screen {

    private static final int HEAD_SIZE   = 16;
    private static final int ROW_H       = 26;
    private static final int BTN_W       = 100;
    private static final int BTN_H       = 20;
    private static final int MARGIN      = 30;

    private final Screen parent;
    private final DescriptiveClientConfig config;

    private final List<UUID> descriptivePlayers = new ArrayList<>();
    private Set<UUID> pendingDisabled;
    private Set<UUID> originalDisabled;

    public DescriptiveToggleScreen(Screen parent) {
        super(Text.literal("Rendering Toggles"));
        this.parent = parent;
        this.config = DescriptiveClient.getInstance().getConfig();
    }

    @Override
    protected void init() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int cx = this.width / 2;
        int btnX = this.width - MARGIN - BTN_W;

        pendingDisabled = new HashSet<>(config.getDisabledPlayers());
        originalDisabled = new HashSet<>(config.getDisabledPlayers());

        descriptivePlayers.clear();
        if (mc.player != null) {
            UUID myUuid = mc.player.getUuid();
            for (UUID uuid : CustomNameCache.getAllEntries().keySet()) {
                if (!uuid.equals(myUuid)) descriptivePlayers.add(uuid);
            }
        }

        int y = 30 + 12;

        if (mc.player != null) {
            UUID myUuid = mc.player.getUuid();
            addPendingToggleBtn(btnX, y, myUuid);
        }
        y += ROW_H + MARGIN;

        y += 12;

        for (UUID uuid : descriptivePlayers) {
            addPendingToggleBtn(btnX, y, uuid);
            y += ROW_H;
        }

        int btnY = this.height - 30;
        int bW = 100;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                        button -> cancelAndClose())
                .dimensions(cx - bW - 4, btnY, bW, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"),
                        button -> applyAndClose())
                .dimensions(cx + 4, btnY, bW, 20).build());
    }

    private void addPendingToggleBtn(int x, int y, UUID uuid) {
        boolean enabled = !pendingDisabled.contains(uuid);
        int btnY = y + (HEAD_SIZE / 2) - (BTN_H / 2);
        ButtonWidget btn = ButtonWidget.builder(buildToggleLabel(enabled), button -> {
            if (pendingDisabled.contains(uuid)) pendingDisabled.remove(uuid);
            else pendingDisabled.add(uuid);
            config.setDisabledPlayers(pendingDisabled);
            button.setMessage(buildToggleLabel(!pendingDisabled.contains(uuid)));
        }).dimensions(x, btnY, BTN_W, BTN_H).build();
        this.addDrawableChild(btn);
    }

    private Text buildToggleLabel(boolean on) {
        return Text.literal(on ? "✔ Enabled" : "✘ Disabled")
                .styled(s -> s.withBold(true)
                        .withFormatting(on ? Formatting.GREEN : Formatting.RED));
    }

    private void applyAndClose() {
        config.setDisabledPlayers(pendingDisabled);
        config.save();
        if (this.client != null) this.client.setScreen(parent);
    }

    private void cancelAndClose() {
        // Revert config to original state without saving
        config.setDisabledPlayers(originalDisabled);
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void close() {
        cancelAndClose();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        MinecraftClient mc = MinecraftClient.getInstance();
        int cx = this.width / 2;
        int left = MARGIN;
        int y = 10;

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Rendering Toggles").styled(s -> s.withBold(true)),
                cx, y, 0xFFFFFFFF);
        y += 12;

        context.fill(left, y, this.width - MARGIN, y + 1, 0x44FFFFFF);
        y += 8;

        context.drawTextWithShadow(textRenderer,
                Text.literal("You").styled(s -> s.withBold(true).withFormatting(Formatting.YELLOW)),
                left, y, 0xFFFFFFFF);
        y += 12;

        if (mc.player != null) {
            drawPlayerHead(context, mc.player.getUuid(), left, y);
            context.drawTextWithShadow(textRenderer,
                    NameBuilder.buildCustomName(mc.player.getName().getString()),
                    left + HEAD_SIZE + 6, y + (HEAD_SIZE / 2) - 4, 0xFFFFFFFF);
        }
        y += ROW_H;

        y += MARGIN / 2;
        context.fill(left, y, this.width - MARGIN, y + 1, 0x33FFFFFF);
        y += MARGIN / 2;

        context.drawTextWithShadow(textRenderer,
                Text.literal("Other Players").styled(s -> s.withBold(true).withFormatting(Formatting.AQUA)),
                left, y, 0xFFFFFFFF);
        y += 12;

        if (descriptivePlayers.isEmpty()) {
            context.fill(left, y, left + HEAD_SIZE, y + HEAD_SIZE, 0xFF555555);
            context.drawTextWithShadow(textRenderer,
                    Text.literal("No other Descriptive players online")
                            .styled(s -> s.withFormatting(Formatting.GRAY)),
                    left + HEAD_SIZE + 6, y + (HEAD_SIZE / 2) - 4, 0xFFFFFFFF);
        } else {
            for (UUID uuid : descriptivePlayers) {
                drawPlayerHead(context, uuid, left, y);
                String name = getPlayerName(mc, uuid);
                context.drawTextWithShadow(textRenderer,
                        NameBuilder.buildCustomName(uuid, name),
                        left + HEAD_SIZE + 6, y + (HEAD_SIZE / 2) - 4, 0xFFFFFFFF);
                y += ROW_H;
            }
        }
    }

    private void drawPlayerHead(DrawContext context, UUID uuid, int x, int y) {
        if (client == null || client.getNetworkHandler() == null) return;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry == null) {
            context.fill(x, y, x + HEAD_SIZE, y + HEAD_SIZE, 0xFF555555);
            return;
        }
        Identifier skin = entry.getSkinTextures().body().texturePath();
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                skin, x, y, 8.0f, 8.0f, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                skin, x, y, 40.0f, 8.0f, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);
    }

    private String getPlayerName(MinecraftClient mc, UUID uuid) {
        if (mc.getNetworkHandler() == null) return uuid.toString().substring(0, 8);
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
        return entry != null ? entry.getProfile().name() : uuid.toString().substring(0, 8);
    }
}