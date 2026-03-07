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
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DescriptiveRelayScreen extends Screen {

    private static final int    MARGIN      = 30;
    private static final int    LINE_H      = 13;
    private static final int    ICON_COL    = 10;
    private static final int    TEXT_INDENT = ICON_COL + 4;
    private static final String RELAY_URL   = "wss://relay-descriptive.knochenn.de/sync";

    private final Screen                parent;
    private final DescriptiveClientConfig config;

    private boolean accepted;
    private boolean relayEnabled;

    @SuppressWarnings("FieldCanBeLocal")
    private CheckboxWidget acceptCheckbox;
    private ButtonWidget   toggleButton;
    private ButtonWidget   checkStatusButton;

    private Boolean relayOnline   = null;
    private boolean checkingRelay = false;
    private long    pingMs        = -1;

    private record ContentLine(String icon, int iconColor, String text) {}

    private static final ContentLine[] BENEFITS = {
            new ContentLine("✔", 0xFF55FF55, "Custom names will work on any server, vanilla or modded."),
            new ContentLine("✔", 0xFF55FF55, "Connection opens on server join and closes when you"),
            new ContentLine(null, 0,         "leave. No persistent background process."),
            new ContentLine("✔", 0xFF55FF55, "You can enable or disable this function at any time."),
            new ContentLine("✔", 0xFF55FF55, "The relay connection code is open source."),
    };

    private static final ContentLine[] WARNINGS = {
            new ContentLine("⚠", 0xFFFFFF55, "Only your Minecraft UUID, styling data and current server"),
            new ContentLine(null, 0,         "address are transmitted to an external websocket server"),
            new ContentLine(null, 0,         "called relay."),
            new ContentLine("⚠", 0xFFFFFF55, "Transmitted data is never stored. It exists only"),
            new ContentLine(null, 0,         "in the relay's RAM and is cleared on disconnect."),
            new ContentLine("⚠", 0xFFFFFF55, "The relay is community-run on a best-effort basis."),
            new ContentLine(null, 0,         "There is no guaranteed uptime."),
    };

    public DescriptiveRelayScreen(Screen parent) {
        super(Text.literal("Relay Settings"));
        this.parent = parent;
        this.config = DescriptiveClient.getInstance().getConfig();
        this.relayEnabled = config.isRelayEnabled();
        this.accepted     = config.isRelayEnabled();
    }

    @Override
    protected void init() {
        int cx       = this.width / 2;
        int contentW = Math.min(420, this.width - MARGIN * 2);
        int left     = cx - contentW / 2;

        acceptCheckbox = CheckboxWidget.builder(
                        Text.literal("I have read and accept the above information."),
                        this.textRenderer)
                .pos(left, getCheckboxY())
                .checked(accepted)
                .callback((cb, v) -> {
                    accepted = v;
                    if (!accepted && relayEnabled) {
                        relayEnabled = false;
                        config.setRelayEnabled(false);
                        config.save();
                    }
                    updateToggleButton();
                })
                .build();
        this.addDrawableChild(acceptCheckbox);

        int btnY = this.height - 30;
        int bW   = 100;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Go Back"),
                        button -> { if (this.client != null) this.client.setScreen(parent); })
                .dimensions(cx - bW - 4, btnY, bW, 20).build());

        toggleButton = ButtonWidget.builder(buildToggleLabel(relayEnabled), button -> {
            relayEnabled = !relayEnabled;
            config.setRelayEnabled(relayEnabled);
            config.save();
            button.setMessage(buildToggleLabel(relayEnabled));
        }).dimensions(cx + 4, btnY, bW, 20).build();
        this.addDrawableChild(toggleButton);
        updateToggleButton();

        checkStatusButton = ButtonWidget.builder(
                Text.literal("Check Status"),
                button -> pingRelay()
        ).dimensions(cx - 50, btnY - 26, 100, 20).build();
        this.addDrawableChild(checkStatusButton);
    }

    private void pingRelay() {
        if (checkingRelay) return;
        checkingRelay = true;
        relayOnline   = null;
        pingMs        = -1;
        checkStatusButton.active = false;
        checkStatusButton.setMessage(Text.literal("Checking…"));

        long startMs = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            try (HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()) {

                WebSocket.Listener listener = new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket ws) {
                        pingMs      = System.currentTimeMillis() - startMs;
                        relayOnline = true;
                        ws.sendClose(WebSocket.NORMAL_CLOSURE, "ping")
                                .thenRun(() -> resetCheckButton());
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                        resetCheckButton();
                        return WebSocket.Listener.super.onClose(ws, code, reason);
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        relayOnline = false;
                        pingMs      = -1;
                        resetCheckButton();
                    }
                };

                httpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .buildAsync(URI.create(RELAY_URL), listener)
                        .exceptionally(e -> {
                            relayOnline = false;
                            pingMs      = -1;
                            resetCheckButton();
                            return null;
                        })
                        .join();

            } catch (Exception e) {
                relayOnline = false;
                resetCheckButton();
            }
        });
    }

    private void resetCheckButton() {
        checkingRelay = false;
        if (client != null) {
            client.execute(() -> {
                checkStatusButton.active = true;
                checkStatusButton.setMessage(Text.literal("Check Status"));
            });
        }
    }

    private void updateToggleButton() {
        toggleButton.active = accepted;
        toggleButton.setMessage(buildToggleLabel(relayEnabled));
    }

    private Text buildToggleLabel(boolean on) {
        return Text.literal("Relay: ")
                .append(Text.literal(on ? "Enabled" : "Disabled")
                        .styled(s -> s.withBold(true)
                                .withFormatting(on ? Formatting.GREEN : Formatting.RED)));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx       = this.width / 2;
        int contentW = Math.min(420, this.width - MARGIN * 2);
        int left     = cx - contentW / 2;
        int y        = 12;

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Relay Settings").styled(s -> s.withBold(true)),
                cx, y, 0xFFFFFFFF);
        y += 14;

        context.fill(left, y, left + contentW, y + 1, 0x44FFFFFF);
        y += 10;

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Descriptive would like to connect to a external Server to sync custom names."),
                cx, y, 0xFFCCCCCC);
        y += LINE_H + 8;

        context.drawTextWithShadow(textRenderer,
                Text.literal("Benefits").styled(s -> s.withBold(true).withFormatting(Formatting.GREEN)),
                left, y, 0xFFFFFFFF);
        y += LINE_H + 2;
        y = drawContentLines(context, left, y, BENEFITS);
        y += 8;

        context.drawTextWithShadow(textRenderer,
                Text.literal("Important to know").styled(s -> s.withBold(true).withFormatting(Formatting.YELLOW)),
                left, y, 0xFFFFFFFF);
        y += LINE_H + 2;
        y = drawContentLines(context, left, y, WARNINGS);
        y += 10;

        context.fill(left, y, left + contentW, y + 1, 0x33FFFFFF);

        int statusY = this.height - 30 - 26 - 18;

        if (checkingRelay) {
            int dots = (int) ((System.currentTimeMillis() / 400) % 4);
            String anim = "●".repeat(dots) + "○".repeat(3 - dots);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Pinging relay  " + anim)
                            .styled(s -> s.withFormatting(Formatting.GRAY)),
                    cx, statusY, 0xFFFFFFFF);

        } else if (relayOnline == null) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Check the relay's status. This will attempt to connect to a external websocket server.")
                            .styled(s -> s.withFormatting(Formatting.GRAY).withItalic(true)),
                    cx, statusY, 0xFFFFFFFF);

        } else if (relayOnline) {
            String pingLabel = pingMs >= 0 ? "  (" + pingMs + " ms)" : "";
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("● Relay Online" + pingLabel)
                            .styled(s -> s.withBold(true).withFormatting(Formatting.GREEN)),
                    cx, statusY, 0xFFFFFFFF);

        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("● Relay Offline or unreachable")
                            .styled(s -> s.withBold(true).withFormatting(Formatting.RED)),
                    cx, statusY, 0xFFFFFFFF);
        }
    }

    private int drawContentLines(DrawContext context, int left, int y, ContentLine[] lines) {
        for (ContentLine line : lines) {
            if (line.icon() != null) {
                context.drawTextWithShadow(textRenderer,
                        Text.literal(line.icon()), left, y, line.iconColor());
            }
            context.drawTextWithShadow(textRenderer,
                    Text.literal(line.text()), left + TEXT_INDENT, y, 0xFFCCCCCC);
            y += LINE_H;
        }
        return y;
    }

    private int getCheckboxY() {
        int y = 12 + 14 + 10;
        y += LINE_H + 8;
        y += LINE_H + 2 + BENEFITS.length * LINE_H + 8;
        y += LINE_H + 2 + WARNINGS.length * LINE_H + 10;
        y += 1 + 8;
        return y;
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}