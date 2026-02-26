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
import de.devknochen.descriptive.client.animation.Animation;
import de.devknochen.descriptive.client.animation.AnimationRegistry;
import de.devknochen.descriptive.config.DescriptiveConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.*;

@SuppressWarnings({"FieldCanBeLocal"})
public class DescriptiveConfigScreen extends Screen {

    private static final int CONTENT_WIDTH = 220;
    private static final int SECTION_GAP   = 8;
    private enum ColorMode { SOLID, RAINBOW, GRADIENT }

    private final Screen parent;
    private final DescriptiveConfig config;

    private int previewColor;
    private boolean previewBold;
    private boolean previewItalic;
    private boolean previewUnderlined;
    private boolean previewStrikethrough;

    private int gradientColor1 = 0xFF0000;
    private int gradientColor2 = 0x0000FF;
    private ColorMode colorMode;

    private final List<String> originalAnimationTypes;
    private final float originalAnimationSpeed;
    private final int originalGradientColor1;
    private final int originalGradientColor2;

    private ColorSlider redSlider;
    private ColorSlider greenSlider;
    private ColorSlider blueSlider;
    private TextFieldWidget hexInput;
    private CheckboxWidget boldCheckbox;
    private CheckboxWidget italicCheckbox;
    private CheckboxWidget underlinedCheckbox;
    private CheckboxWidget strikethroughCheckbox;
    private SliderWidget animationSpeedSlider;
    private TextFieldWidget gradientHex1;
    private TextFieldWidget gradientHex2;

    private boolean updatingFromSliders = false;
    private boolean updatingFromHex     = false;

    public DescriptiveConfigScreen(Screen parent) {
        super(Text.literal("Descriptive Settings"));
        this.parent = parent;
        this.config = DescriptiveClient.getInstance().getConfig();

        this.previewColor         = config.getColor();
        this.previewBold          = config.isBold();
        this.previewItalic        = config.isItalic();
        this.previewUnderlined    = config.isUnderlined();
        this.previewStrikethrough = config.isStrikethrough();

        List<Integer> savedGradient = config.getGradientColors();
        if (savedGradient.size() >= 2) {
            gradientColor1 = savedGradient.get(0);
            gradientColor2 = savedGradient.get(1);
        }

        List<String> active = config.getAnimationTypes();
        if (active.contains("rainbow"))       colorMode = ColorMode.RAINBOW;
        else if (active.contains("gradient")) colorMode = ColorMode.GRADIENT;
        else                                  colorMode = ColorMode.SOLID;

        this.originalAnimationTypes = new ArrayList<>(config.getAnimationTypes());
        this.originalAnimationSpeed = config.getAnimationSpeed();
        this.originalGradientColor1 = this.gradientColor1;
        this.originalGradientColor2 = this.gradientColor2;
    }

    public Screen getParent() { return parent; }

    @Override
    protected void init() {
        int cx   = this.width / 2;
        int left = cx - CONTENT_WIDTH / 2;
        int y    = 36;

        y += 12;

        this.addDrawableChild(CyclingButtonWidget.<ColorMode>builder(
                        mode -> Text.literal(switch (mode) {
                            case SOLID    -> "Solid Color";
                            case RAINBOW  -> "Rainbow";
                            case GRADIENT -> "Gradient";
                        }),
                        () -> colorMode
                ).values(ColorMode.SOLID, ColorMode.RAINBOW, ColorMode.GRADIENT)
                .omitKeyText()
                .build(left, y, CONTENT_WIDTH, 20, Text.literal("Color Mode"), (btn, mode) -> {
                    colorMode = mode;
                    List<String> current = new ArrayList<>(config.getAnimationTypes());
                    current.remove("rainbow");
                    current.remove("gradient");
                    if (mode == ColorMode.RAINBOW)  current.add("rainbow");
                    if (mode == ColorMode.GRADIENT) current.add("gradient");
                    config.setAnimationTypesInternal(current);
                    reinitialize();
                }));
        y += 24;

        if (colorMode == ColorMode.SOLID) {
            this.redSlider = new ColorSlider(left, y, CONTENT_WIDTH, 20,
                    Text.literal("Red"), getRed(previewColor) / 255.0, ColorChannel.RED);
            this.addDrawableChild(redSlider); y += 22;

            this.greenSlider = new ColorSlider(left, y, CONTENT_WIDTH, 20,
                    Text.literal("Green"), getGreen(previewColor) / 255.0, ColorChannel.GREEN);
            this.addDrawableChild(greenSlider); y += 22;

            this.blueSlider = new ColorSlider(left, y, CONTENT_WIDTH, 20,
                    Text.literal("Blue"), getBlue(previewColor) / 255.0, ColorChannel.BLUE);
            this.addDrawableChild(blueSlider); y += 24;

            int hexW = 80; int swatchSize = 20; int gap = 6;
            int hexX = cx - (hexW + gap + swatchSize) / 2;
            this.hexInput = new TextFieldWidget(this.textRenderer, hexX + swatchSize + gap, y, hexW, 20, Text.literal("Hex"));
            this.hexInput.setMaxLength(7);
            this.hexInput.setText(String.format("#%06X", previewColor & 0xFFFFFF));
            this.hexInput.setChangedListener(this::onHexInputChanged);
            this.addDrawableChild(hexInput);
            y += 24;
        } else if (colorMode == ColorMode.GRADIENT) {
            int fieldW  = 80; int swatchW = 18; int pairGap = 10;
            int pairW   = swatchW + 6 + fieldW;
            int totalGW = pairW * 2 + pairGap;
            int gLeft   = cx - totalGW / 2;

            this.gradientHex1 = makeGradientHexField(gLeft + swatchW + 6, y, gradientColor1, hex -> {
                int p = parseHex(hex); if (p >= 0) { gradientColor1 = p; saveGradientColors(); }
            });
            this.gradientHex2 = makeGradientHexField(gLeft + pairW + pairGap + swatchW + 6, y, gradientColor2, hex -> {
                int p = parseHex(hex); if (p >= 0) { gradientColor2 = p; saveGradientColors(); }
            });
            this.addDrawableChild(gradientHex1);
            this.addDrawableChild(gradientHex2);
            y += 26;
        } else {
            this.gradientHex1 = null;
            this.gradientHex2 = null;
        }

        y += SECTION_GAP;

        y += 1 + SECTION_GAP;

        y += 12;

        int colW = CONTENT_WIDTH / 2;

        this.boldCheckbox = CheckboxWidget.builder(Text.literal("Bold"), this.textRenderer)
                .pos(left, y).checked(previewBold)
                .callback((cb, v) -> previewBold = v).build();
        this.addDrawableChild(boldCheckbox);

        this.italicCheckbox = CheckboxWidget.builder(Text.literal("Italic"), this.textRenderer)
                .pos(left + colW, y).checked(previewItalic)
                .callback((cb, v) -> previewItalic = v).build();
        this.addDrawableChild(italicCheckbox);
        y += 22;

        this.underlinedCheckbox = CheckboxWidget.builder(Text.literal("Underline"), this.textRenderer)
                .pos(left, y).checked(previewUnderlined)
                .callback((cb, v) -> previewUnderlined = v).build();
        this.addDrawableChild(underlinedCheckbox);

        this.strikethroughCheckbox = CheckboxWidget.builder(Text.literal("Strikethrough"), this.textRenderer)
                .pos(left + colW, y).checked(previewStrikethrough)
                .callback((cb, v) -> previewStrikethrough = v).build();
        this.addDrawableChild(strikethroughCheckbox);
        y += 22 + SECTION_GAP;

        y += 1 + SECTION_GAP;

        y += 12;

        List<String> animationTypes = new ArrayList<>(AnimationRegistry.getAll().keySet());
        animationTypes.removeAll(List.of("none", "rainbow", "gradient"));
        Collections.sort(animationTypes);
        List<String> activeAnimations = config.getAnimationTypes();

        int cbPerRow = 2;
        int animCbW  = (CONTENT_WIDTH - 8) / cbPerRow;
        int row = 0, col = 0;
        for (String animType : animationTypes) {
            Animation anim = AnimationRegistry.get(animType);
            int ax = left + col * (animCbW + 8);
            int ay = y + row * 22;
            CheckboxWidget cb = CheckboxWidget.builder(Text.literal(anim.getName()), this.textRenderer)
                    .pos(ax, ay).checked(activeAnimations.contains(animType))
                    .callback((c, v) -> {
                        List<String> current = new ArrayList<>(config.getAnimationTypes());
                        if (v) { if (!current.contains(animType)) current.add(animType); }
                        else current.remove(animType);
                        config.setAnimationTypesInternal(current);
                    })
                    .build();
            this.addDrawableChild(cb);
            col++;
            if (col >= cbPerRow) { col = 0; row++; }
        }
        y += (row + 1) * 22 + 4;

        this.animationSpeedSlider = new AnimationSpeedSlider(left, y, CONTENT_WIDTH, 20,
                Text.literal("Speed"), (config.getAnimationSpeed() - 0.1f) / 4.9f);
        this.addDrawableChild(animationSpeedSlider);

        int buttonY   = this.height - 30;
        int smallBtnW = 100;
        int btnLeft   = cx - (smallBtnW * 3 + 8) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Toggle Relay..."),
                button -> {
                    if (this.client != null) this.client.setScreen(new DescriptiveRelayScreen(this));
                }).dimensions(cx - 154, buttonY - 24, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Rendering Toggles..."),
                button -> {
                    if (this.client != null) this.client.setScreen(new DescriptiveToggleScreen(this));
                }).dimensions(cx + 4, buttonY - 24, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§cReset..."),
                button -> {
                    if (this.client != null) this.client.setScreen(new DescriptiveResetScreen(this));
                }).dimensions(btnLeft, buttonY, smallBtnW, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                        button -> cancelAndClose())
                .dimensions(btnLeft + smallBtnW + 4, buttonY, smallBtnW, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), button -> {
            config.setColorInternal(previewColor);
            config.setBoldInternal(previewBold);
            config.setItalicInternal(previewItalic);
            config.setUnderlinedInternal(previewUnderlined);
            config.setStrikethroughInternal(previewStrikethrough);
            config.setGradientColorsInternal(List.of(gradientColor1, gradientColor2));
            config.save();
            if (this.client != null) this.client.setScreen(parent);
        }).dimensions(btnLeft + (smallBtnW + 4) * 2, buttonY, smallBtnW, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx   = this.width / 2;
        int left = cx - CONTENT_WIDTH / 2;
        int contentRight = cx + CONTENT_WIDTH / 2;
        int y    = 12;

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Descriptive Settings").styled(s -> s.withBold(true)),
                cx, y, 0xFFFFFFFF);
        y += 14;

        context.fill(left, y, contentRight, y + 1, 0x44FFFFFF);
        y += 10;

        context.drawTextWithShadow(textRenderer, Text.literal("Color").styled(s -> s.withBold(true)), left, y, 0xFFFFFFFF);
        y += 12 + 24;

        if (colorMode == ColorMode.SOLID) {
            y += 22 + 22 + 24;
            int hexW = 80; int swatchSize = 20; int gap = 6;
            int hexX    = cx - (hexW + gap + swatchSize) / 2;
            int swatchX = hexX;
            context.fill(swatchX,     y,     swatchX + swatchSize,     y + swatchSize, 0xFF000000);
            context.fill(swatchX + 1, y + 1, swatchX + swatchSize - 1, y + swatchSize - 1, previewColor | 0xFF000000);
            y += 24;
        } else if (colorMode == ColorMode.GRADIENT) {
            int swatchW = 18; int pairGap = 10;
            int pairW   = swatchW + 6 + 80;
            int gLeft   = cx - (pairW * 2 + pairGap) / 2;
            drawSwatch(context, gLeft,                    y, gradientColor1, swatchW);
            drawSwatch(context, gLeft + pairW + pairGap, y, gradientColor2, swatchW);
            y += 26;
        }

        context.fill(left, y, contentRight, y + 1, 0x33FFFFFF);
        y += 1 + SECTION_GAP;

        context.drawTextWithShadow(textRenderer, Text.literal("Formatting").styled(s -> s.withBold(true)), left, y, 0xFFFFFFFF);
        y += 12 + 22 + 22 + SECTION_GAP;

        context.fill(left, y, contentRight, y + 1, 0x33FFFFFF);
        y += 1 + SECTION_GAP;

        context.drawTextWithShadow(textRenderer, Text.literal("Animations").styled(s -> s.withBold(true)), left, y, 0xFFFFFFFF);

        // Preview strip — top separator 10px below the speed slider, bottom separator above buttons
        int previewY = this.height - 108;
        context.fill(left, previewY, contentRight, previewY + 1, 0x33FFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Preview").styled(s -> s.withBold(true)), left, previewY + 8, 0xFFFFFFFF);

        String playerName = (client != null && client.player != null) ? client.player.getName().getString() : "Preview";
        java.util.UUID previewUuid = (client != null && client.player != null) ? client.player.getUuid() : java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
        de.devknochen.descriptive.client.animation.PlayerAnimationContext.setCurrentPlayer(previewUuid);
        config.setColorInternal(previewColor);
        net.minecraft.text.Text previewText = de.devknochen.descriptive.common.util.NameBuilder.buildPreview(
                playerName, previewUuid,
                previewColor, previewBold, previewItalic, previewUnderlined, previewStrikethrough);

        context.drawCenteredTextWithShadow(textRenderer, previewText, cx, previewY + 23, 0xFFFFFFFF);
        context.fill(left, previewY + 46, contentRight, previewY + 47, 0x33FFFFFF);
    }

    private void drawSwatch(DrawContext context, int x, int y, int color, int size) {
        context.fill(x,     y,     x + size,     y + size, 0xFF000000);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, color | 0xFF000000);
    }

    private TextFieldWidget makeGradientHexField(int x, int y, int color, java.util.function.Consumer<String> onChange) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, 80, 18, Text.literal(""));
        field.setMaxLength(7);
        field.setText(String.format("#%06X", color & 0xFFFFFF));
        field.setChangedListener(onChange);
        return field;
    }

    private int parseHex(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6) return -1;
        try { return Integer.parseInt(hex, 16); }
        catch (NumberFormatException e) { return -1; }
    }

    private void saveGradientColors() {
        config.setGradientColorsInternal(List.of(gradientColor1, gradientColor2));
    }

    private void updateSlidersFromColor() {
        if (redSlider   != null) redSlider.setValue(getRed(previewColor)     / 255.0);
        if (greenSlider != null) greenSlider.setValue(getGreen(previewColor) / 255.0);
        if (blueSlider  != null) blueSlider.setValue(getBlue(previewColor)   / 255.0);
    }

    private void reinitialize() {
        this.clearChildren();
        this.init();
    }

    private void cancelAndClose() {
        config.setAnimationTypesInternal(originalAnimationTypes);
        config.setAnimationSpeedInternal(originalAnimationSpeed);
        config.setGradientColorsInternal(List.of(originalGradientColor1, originalGradientColor2));
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void close() {
        cancelAndClose();
    }

    private void onHexInputChanged(String hex) {
        if (updatingFromSliders) return;
        updatingFromHex = true;
        String raw = hex.startsWith("#") ? hex.substring(1) : hex;
        if (raw.length() == 6) {
            try {
                previewColor = Integer.parseInt(raw, 16);
                updateSlidersFromColor();
            } catch (NumberFormatException ignored) {}
        }
        updatingFromHex = false;
    }

    private static int getRed(int color)   { return (color >> 16) & 0xFF; }
    private static int getGreen(int color) { return (color >> 8)  & 0xFF; }
    private static int getBlue(int color)  { return color & 0xFF; }

    private enum ColorChannel { RED, GREEN, BLUE }

    private class ColorSlider extends SliderWidget {
        private final ColorChannel channel;

        ColorSlider(int x, int y, int width, int height, Text text, double value, ColorChannel channel) {
            super(x, y, width, height, text, value);
            this.channel = channel;
            updateMessage();
        }

        protected void setValue(double value) {
            this.value = Math.max(0.0, Math.min(1.0, value));
            applyValue();
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int v = (int)(this.value * 255);
            this.setMessage(Text.literal(switch (channel) {
                case RED   -> "Red: "   + v;
                case GREEN -> "Green: " + v;
                case BLUE  -> "Blue: "  + v;
            }));
        }

        @Override
        protected void applyValue() {
            if (updatingFromSliders || updatingFromHex) return;
            updatingFromSliders = true;

            int v     = (int)(this.value * 255);
            int red   = getRed(previewColor);
            int green = getGreen(previewColor);
            int blue  = getBlue(previewColor);

            switch (channel) {
                case RED   -> red   = v;
                case GREEN -> green = v;
                case BLUE  -> blue  = v;
            }

            previewColor = (red << 16) | (green << 8) | blue;

            if (hexInput != null && !hexInput.isFocused())
                hexInput.setText(String.format("#%06X", previewColor & 0xFFFFFF));

            updatingFromSliders = false;
        }
    }

    private class AnimationSpeedSlider extends SliderWidget {
        AnimationSpeedSlider(int x, int y, int width, int height, Text text, double value) {
            super(x, y, width, height, text, value);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(String.format("Speed: %.1fx", 0.1f + (float)(this.value * 4.9f))));
        }

        @Override
        protected void applyValue() {
            config.setAnimationSpeedInternal(0.1f + (float)(this.value * 4.9f));
        }
    }
}