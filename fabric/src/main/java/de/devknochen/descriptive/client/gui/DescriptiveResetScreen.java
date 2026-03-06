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
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DescriptiveResetScreen extends Screen {

    private static final int MARGIN = 30;
    private static final int LINE_H = 13;

    private record ResetLine(String icon, int iconColor, String text) {}

    private static final ResetLine[] CONSEQUENCES = {
            new ResetLine("✘", 0xFFFF5555, "Custom color will be reset to white."),
            new ResetLine("✘", 0xFFFF5555, "Bold, italic, underline and strikethrough will be disabled."),
            new ResetLine("✘", 0xFFFF5555, "All animations will be disabled."),
            new ResetLine("✘", 0xFFFF5555, "Gradient colors will be reset to defaults."),
            new ResetLine("✘", 0xFFFF5555, "Per-player rendering toggles will be reset."),
    };

    private final Screen parent;

    public DescriptiveResetScreen(Screen parent) {
        super(Text.literal("Reset Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx     = this.width / 2;
        int btnY   = this.height - 30;
        int btnW   = 100;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                        button -> { if (this.client != null) this.client.setScreen(parent); })
                .dimensions(cx - btnW - 4, btnY, btnW, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset").styled(s -> s.withBold(true).withFormatting(Formatting.RED)),
                button -> {
                    doReset();
                    if (this.client != null) {
                        if (parent instanceof DescriptiveConfigScreen configScreen) {
                            this.client.setScreen(new DescriptiveConfigScreen(configScreen.getParent()));
                        } else {
                            this.client.setScreen(parent);
                        }
                    }
                }).dimensions(cx + 4, btnY, btnW, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx       = this.width / 2;
        int contentW = Math.min(420, this.width - MARGIN * 2);
        int left     = cx - contentW / 2;
        int y        = 12;

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Reset Settings").styled(s -> s.withBold(true)),
                cx, y, 0xFFFFFFFF);
        y += 14;

        context.fill(left, y, left + contentW, y + 1, 0x44FFFFFF);
        y += 10;

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("The following settings will be permanently reset to their defaults."),
                cx, y, 0xFFCCCCCC);
        y += LINE_H + 8;

        context.fill(left, y, left + contentW, y + 1, 0x33FFFFFF);
        y += 10;

        context.drawTextWithShadow(textRenderer,
                Text.literal("Will be reset").styled(s -> s.withBold(true).withFormatting(Formatting.RED)),
                left, y, 0xFFFFFFFF);
        y += LINE_H + 4;

        int iconIndent = 10 + 4;
        for (ResetLine line : CONSEQUENCES) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal(line.icon()), left, y, line.iconColor());
            context.drawTextWithShadow(textRenderer,
                    Text.literal(line.text()), left + iconIndent, y, 0xFFCCCCCC);
            y += LINE_H;
        }
        y += 10;

        context.fill(left, y, left + contentW, y + 1, 0x33FFFFFF);
    }

    private void doReset() {
        DescriptiveClientConfig config = DescriptiveClient.getInstance().getConfig();
        config.setColorInternal(0xFFFFFF);
        config.setBoldInternal(false);
        config.setItalicInternal(false);
        config.setUnderlinedInternal(false);
        config.setStrikethroughInternal(false);
        config.setAnimationTypesInternal(new ArrayList<>());
        config.setAnimationSpeedInternal(1.0f);
        config.setGradientColors(List.of(0xFF0000, 0x0000FF));
        config.setDisabledPlayers(Collections.emptySet());
        config.save();
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}