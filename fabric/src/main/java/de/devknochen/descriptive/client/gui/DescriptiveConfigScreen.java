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
import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.common.util.NameBuilder;
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import java.util.*;

@SuppressWarnings("FieldCanBeLocal")
public class DescriptiveConfigScreen extends Screen {

    private static final int CONTENT_WIDTH = 220;
    private static final int SECTION_GAP   = 8;
    private enum ColorMode { SOLID, RAINBOW, GRADIENT }

    private final @Nullable Screen parent;
    private final DescriptiveClientConfig config;
    private int previewColor;
    private boolean previewBold, previewItalic, previewUnderlined, previewStrikethrough;
    private int gradientColor1 = 0xFF0000, gradientColor2 = 0x0000FF;
    private ColorMode colorMode;
    private final List<String> originalAnimationTypes;
    private final float originalAnimationSpeed;
    private final int originalGradientColor1, originalGradientColor2;
    private ColorSlider redSlider, greenSlider, blueSlider;
    private EditBox hexInput, gradientHex1, gradientHex2;
    private boolean updatingFromSliders = false, updatingFromHex = false;

    public DescriptiveConfigScreen(@Nullable Screen parent) {
        super(Component.literal("Descriptive Settings"));
        this.parent = parent;
        this.config = DescriptiveClient.getInstance().getConfig();
        this.previewColor         = config.getColor();
        this.previewBold          = config.isBold();
        this.previewItalic        = config.isItalic();
        this.previewUnderlined    = config.isUnderlined();
        this.previewStrikethrough = config.isStrikethrough();
        List<Integer> sg = config.getGradientColors();
        if (sg.size() >= 2) { gradientColor1 = sg.get(0); gradientColor2 = sg.get(1); }
        List<String> active = config.getAnimationTypes();
        if (active.contains("rainbow"))       colorMode = ColorMode.RAINBOW;
        else if (active.contains("gradient")) colorMode = ColorMode.GRADIENT;
        else                                  colorMode = ColorMode.SOLID;
        this.originalAnimationTypes = new ArrayList<>(config.getAnimationTypes());
        this.originalAnimationSpeed = config.getAnimationSpeed();
        this.originalGradientColor1 = this.gradientColor1;
        this.originalGradientColor2 = this.gradientColor2;
    }

    public @Nullable Screen getParent() { return parent; }

    @Override
    protected void init() {
        int cx = this.width / 2, left = cx - CONTENT_WIDTH / 2, y = 36 + 12;
        ColorMode initialMode = colorMode;
        // CycleButton.builder(valueStringifier, defaultValueSupplier) - 26.1 signature
        this.addRenderableWidget(CycleButton.<ColorMode>builder(
                        mode -> Component.literal(switch (mode) {
                            case SOLID -> "Solid Color"; case RAINBOW -> "Rainbow"; case GRADIENT -> "Gradient";
                        }), () -> initialMode)
                .withValues(ColorMode.SOLID, ColorMode.RAINBOW, ColorMode.GRADIENT)
                .displayOnlyValue()
                .create(left, y, CONTENT_WIDTH, 20, Component.literal("Color Mode"), (_b, mode) -> {
                    colorMode = mode;
                    List<String> cur = new ArrayList<>(config.getAnimationTypes());
                    cur.remove("rainbow"); cur.remove("gradient");
                    if (mode == ColorMode.RAINBOW) cur.add("rainbow");
                    if (mode == ColorMode.GRADIENT) cur.add("gradient");
                    config.setAnimationTypesInternal(cur); reinitialize();
                }));
        y += 24;

        if (colorMode == ColorMode.SOLID) {
            this.redSlider = new ColorSlider(left, y, CONTENT_WIDTH, 20,
                    Component.literal("Red"), getRed(previewColor)/255.0, ColorChannel.RED);
            this.addRenderableWidget(redSlider); y += 22;
            this.greenSlider = new ColorSlider(left, y, CONTENT_WIDTH, 20,
                    Component.literal("Green"), getGreen(previewColor)/255.0, ColorChannel.GREEN);
            this.addRenderableWidget(greenSlider); y += 22;
            this.blueSlider = new ColorSlider(left, y, CONTENT_WIDTH, 20,
                    Component.literal("Blue"), getBlue(previewColor)/255.0, ColorChannel.BLUE);
            this.addRenderableWidget(blueSlider); y += 24;
            int hexW = 80, swatchSize = 20, gap = 6;
            this.hexInput = new EditBox(this.font, cx-(hexW+gap+swatchSize)/2+swatchSize+gap, y, hexW, 20, Component.literal("Hex"));
            this.hexInput.setMaxLength(7);
            this.hexInput.setValue(String.format("#%06X", previewColor & 0xFFFFFF));
            this.hexInput.setResponder(this::onHexInputChanged);
            this.addRenderableWidget(hexInput); y += 24;
        } else if (colorMode == ColorMode.GRADIENT) {
            int fw = 80, sw = 18, pg = 10, pw = sw+6+fw;
            int gl = cx - (pw*2+pg)/2;
            this.gradientHex1 = makeHexField(gl+sw+6, y, gradientColor1, h -> { int p=parseHex(h); if(p>=0){gradientColor1=p;saveGradient();} });
            this.gradientHex2 = makeHexField(gl+pw+pg+sw+6, y, gradientColor2, h -> { int p=parseHex(h); if(p>=0){gradientColor2=p;saveGradient();} });
            this.addRenderableWidget(gradientHex1); this.addRenderableWidget(gradientHex2); y += 26;
        }

        y += SECTION_GAP+1+SECTION_GAP+12;
        int colW = CONTENT_WIDTH/2;
        this.addRenderableWidget(Checkbox.builder(Component.literal("Bold"),this.font).pos(left,y).selected(previewBold).onValueChange((_c,v)->previewBold=v).build());
        this.addRenderableWidget(Checkbox.builder(Component.literal("Italic"),this.font).pos(left+colW,y).selected(previewItalic).onValueChange((_c,v)->previewItalic=v).build());
        y+=22;
        this.addRenderableWidget(Checkbox.builder(Component.literal("Underline"),this.font).pos(left,y).selected(previewUnderlined).onValueChange((_c,v)->previewUnderlined=v).build());
        this.addRenderableWidget(Checkbox.builder(Component.literal("Strikethrough"),this.font).pos(left+colW,y).selected(previewStrikethrough).onValueChange((_c,v)->previewStrikethrough=v).build());
        y+=22+SECTION_GAP+1+SECTION_GAP+12;

        List<String> animTypes = new ArrayList<>(AnimationRegistry.getAll().keySet());
        animTypes.removeAll(List.of("none","rainbow","gradient")); Collections.sort(animTypes);
        List<String> activeAnims = config.getAnimationTypes();
        int cbPerRow=2, animCbW=(CONTENT_WIDTH-8)/cbPerRow, row=0, col=0;
        for (String animType : animTypes) {
            Animation anim = AnimationRegistry.get(animType);
            this.addRenderableWidget(Checkbox.builder(Component.literal(anim.getName()),this.font)
                    .pos(left+col*(animCbW+8), y+row*22).selected(activeAnims.contains(animType))
                    .onValueChange((_c,v)->{
                        List<String> cur=new ArrayList<>(config.getAnimationTypes());
                        if(v){if(!cur.contains(animType))cur.add(animType);}else cur.remove(animType);
                        config.setAnimationTypesInternal(cur);
                    }).build());
            col++; if(col>=cbPerRow){col=0;row++;}
        }
        y+=(row+1)*22+4;
        this.addRenderableWidget(new AnimationSpeedSlider(left,y,CONTENT_WIDTH,20,
                Component.literal("Speed"),(config.getAnimationSpeed()-0.1f)/4.9f));

        int bY=this.height-30, bW=100, bL=cx-(bW*3+8)/2;
        this.addRenderableWidget(Button.builder(Component.literal("Toggle Relay..."),_b->{if(minecraft!=null)minecraft.setScreen(new DescriptiveRelayScreen(this));}).bounds(cx-154,bY-24,150,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Rendering Toggles..."),_b->{if(minecraft!=null)minecraft.setScreen(new DescriptiveToggleScreen(this));}).bounds(cx+4,bY-24,150,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§cReset..."),_b->{if(minecraft!=null)minecraft.setScreen(new DescriptiveResetScreen(this));}).bounds(bL,bY,bW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"),_b->cancelAndClose()).bounds(bL+bW+4,bY,bW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Apply"),_b->{
            config.setColorInternal(previewColor); config.setBoldInternal(previewBold);
            config.setItalicInternal(previewItalic); config.setUnderlinedInternal(previewUnderlined);
            config.setStrikethroughInternal(previewStrikethrough);
            config.setGradientColorsInternal(List.of(gradientColor1,gradientColor2));
            config.save(); if(minecraft!=null)minecraft.setScreen(parent);
        }).bounds(bL+(bW+4)*2,bY,bW,20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        super.extractRenderState(g, mx, my, dt);
        int cx=width/2, left=cx-CONTENT_WIDTH/2, right=cx+CONTENT_WIDTH/2, y=12;
        g.centeredText(font,Component.literal("Descriptive Settings").withStyle(s->s.withBold(true)),cx,y,0xFFFFFFFF); y+=14;
        g.fill(left,y,right,y+1,0x44FFFFFF); y+=10;
        g.text(font,Component.literal("Color").withStyle(s->s.withBold(true)),left,y,0xFFFFFFFF); y+=12+24;
        if(colorMode==ColorMode.SOLID){
            y+=22+22+24;
            int hexW=80,sw=20,gap=6,sx=cx-(hexW+gap+sw)/2;
            g.fill(sx,y,sx+sw,y+sw,0xFF000000); g.fill(sx+1,y+1,sx+sw-1,y+sw-1,previewColor|0xFF000000); y+=24;
        } else if(colorMode==ColorMode.GRADIENT){
            int sw=18,pg=10,pw=sw+6+80,gl=cx-(pw*2+pg)/2;
            drawSwatch(g,gl,y,gradientColor1,sw); drawSwatch(g,gl+pw+pg,y,gradientColor2,sw); y+=26;
        }
        g.fill(left,y,right,y+1,0x33FFFFFF); y+=1+SECTION_GAP;
        g.text(font,Component.literal("Formatting").withStyle(s->s.withBold(true)),left,y,0xFFFFFFFF); y+=12+22+22+SECTION_GAP;
        g.fill(left,y,right,y+1,0x33FFFFFF); y+=1+SECTION_GAP;
        g.text(font,Component.literal("Animations").withStyle(s->s.withBold(true)),left,y,0xFFFFFFFF);
        int py=height-108;
        g.fill(left,py,right,py+1,0x33FFFFFF);
        g.text(font,Component.literal("Preview").withStyle(s->s.withBold(true)),left,py+8,0xFFFFFFFF);
        String pn=(minecraft!=null&&minecraft.player!=null)?minecraft.player.getName().getString():"Preview";
        UUID pu=(minecraft!=null&&minecraft.player!=null)?minecraft.player.getUUID():UUID.fromString("00000000-0000-0000-0000-000000000000");
        PlayerAnimationContext.setCurrentPlayer(pu); config.setColorInternal(previewColor);
        g.centeredText(font,NameBuilder.buildPreview(pn,pu,previewColor,previewBold,previewItalic,previewUnderlined,previewStrikethrough),cx,py+23,0xFFFFFFFF);
        g.fill(left,py+46,right,py+47,0x33FFFFFF);
    }

    private void drawSwatch(GuiGraphicsExtractor g,int x,int y,int color,int size){
        g.fill(x,y,x+size,y+size,0xFF000000); g.fill(x+1,y+1,x+size-1,y+size-1,color|0xFF000000);
    }
    private EditBox makeHexField(int x,int y,int color,java.util.function.Consumer<String> cb){
        EditBox f=new EditBox(font,x,y,80,18,Component.literal(""));
        f.setMaxLength(7); f.setValue(String.format("#%06X",color&0xFFFFFF)); f.setResponder(cb); return f;
    }
    private int parseHex(String h){if(h.startsWith("#"))h=h.substring(1);if(h.length()!=6)return -1;try{return Integer.parseInt(h,16);}catch(NumberFormatException e){return -1;}}
    private void saveGradient(){config.setGradientColorsInternal(List.of(gradientColor1,gradientColor2));}
    private void updateSlidersFromColor(){
        if(redSlider!=null)redSlider.updateValue(getRed(previewColor)/255.0);
        if(greenSlider!=null)greenSlider.updateValue(getGreen(previewColor)/255.0);
        if(blueSlider!=null)blueSlider.updateValue(getBlue(previewColor)/255.0);
    }
    private void reinitialize(){clearWidgets();init();}
    private void cancelAndClose(){
        config.setAnimationTypesInternal(originalAnimationTypes);
        config.setAnimationSpeedInternal(originalAnimationSpeed);
        config.setGradientColorsInternal(List.of(originalGradientColor1,originalGradientColor2));
        if(minecraft!=null)minecraft.setScreen(parent);
    }
    @Override public void onClose(){cancelAndClose();}
    private void onHexInputChanged(String hex){
        if(updatingFromSliders)return; updatingFromHex=true;
        String raw=hex.startsWith("#")?hex.substring(1):hex;
        if(raw.length()==6){try{previewColor=Integer.parseInt(raw,16);updateSlidersFromColor();}catch(NumberFormatException ignored){}}
        updatingFromHex=false;
    }
    private static int getRed(int c){return(c>>16)&0xFF;}
    private static int getGreen(int c){return(c>>8)&0xFF;}
    private static int getBlue(int c){return c&0xFF;}
    private enum ColorChannel{RED,GREEN,BLUE}

    private class ColorSlider extends AbstractSliderButton {
        private final ColorChannel channel;
        ColorSlider(int x,int y,int w,int h,Component t,double v,ColorChannel ch){super(x,y,w,h,t,v);this.channel=ch;updateMessage();}
        void updateValue(double v){this.value=Math.max(0,Math.min(1,v));applyValue();updateMessage();}
        @Override protected void updateMessage(){int v=(int)(this.value*255);setMessage(Component.literal(switch(channel){case RED->"Red: "+v;case GREEN->"Green: "+v;case BLUE->"Blue: "+v;}));}
        @Override protected void applyValue(){
            if(updatingFromSliders||updatingFromHex)return; updatingFromSliders=true;
            int v=(int)(this.value*255),r=getRed(previewColor),g=getGreen(previewColor),b=getBlue(previewColor);
            switch(channel){case RED->r=v;case GREEN->g=v;case BLUE->b=v;}
            previewColor=(r<<16)|(g<<8)|b;
            if(hexInput!=null&&!hexInput.isFocused())hexInput.setValue(String.format("#%06X",previewColor&0xFFFFFF));
            updatingFromSliders=false;
        }
    }
    private class AnimationSpeedSlider extends AbstractSliderButton {
        AnimationSpeedSlider(int x,int y,int w,int h,Component t,double v){super(x,y,w,h,t,v);updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(String.format("Speed: %.1fx",0.1f+(float)(this.value*4.9f))));}
        @Override protected void applyValue(){config.setAnimationSpeedInternal(0.1f+(float)(this.value*4.9f));}
    }
}