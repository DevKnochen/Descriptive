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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.*;

@SuppressWarnings("FieldCanBeLocal")
public class DescriptiveConfigScreen extends Screen {

    private static final int MIN_CONTENT_WIDTH = 180;
    private static final int MAX_CONTENT_WIDTH = 300;
    private static final int HORIZONTAL_MARGIN = 20;
    private static final int CONTENT_TOP = 36;
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
    private int scrollOffset = 0;
    private int contentHeight = 0;

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
        this.redSlider = null;
        this.greenSlider = null;
        this.blueSlider = null;
        this.hexInput = null;
        this.gradientHex1 = null;
        this.gradientHex2 = null;

        int cx = this.width / 2, contentWidth = getContentWidth(), left = cx - contentWidth / 2, y = 12;
        ColorMode initialMode = colorMode;
        int widgetY = contentY(y);
        addContentWidget(CycleButton.<ColorMode>builder(
                        mode -> Component.literal(switch (mode) {
                            case SOLID -> "Solid Color"; case RAINBOW -> "Rainbow"; case GRADIENT -> "Gradient";
                        }), () -> initialMode)
                .withValues(ColorMode.SOLID, ColorMode.RAINBOW, ColorMode.GRADIENT)
                .displayOnlyValue()
                .create(left, widgetY, contentWidth, 20, Component.literal("Color Mode"), (_, mode) -> {
                    colorMode = mode;
                    List<String> cur = new ArrayList<>(config.getAnimationTypes());
                    cur.remove("rainbow"); cur.remove("gradient");
                    if (mode == ColorMode.RAINBOW) cur.add("rainbow");
                    if (mode == ColorMode.GRADIENT) cur.add("gradient");
                    config.setAnimationTypesInternal(cur); reinitialize();
                }), widgetY, 20);
        y += 24;

        if (colorMode == ColorMode.SOLID) {
            widgetY = contentY(y);
            this.redSlider = new ColorSlider(left, widgetY, contentWidth, 20,
                    Component.literal("Red"), getRed(previewColor)/255.0, ColorChannel.RED);
            addContentWidget(redSlider, widgetY, 20); y += 22;
            widgetY = contentY(y);
            this.greenSlider = new ColorSlider(left, widgetY, contentWidth, 20,
                    Component.literal("Green"), getGreen(previewColor)/255.0, ColorChannel.GREEN);
            addContentWidget(greenSlider, widgetY, 20); y += 22;
            widgetY = contentY(y);
            this.blueSlider = new ColorSlider(left, widgetY, contentWidth, 20,
                    Component.literal("Blue"), getBlue(previewColor)/255.0, ColorChannel.BLUE);
            addContentWidget(blueSlider, widgetY, 20); y += 24;
            int hexW = 80, swatchSize = 20, gap = 6;
            widgetY = contentY(y);
            this.hexInput = new EditBox(this.font, cx-(hexW+gap+swatchSize)/2+swatchSize+gap, widgetY, hexW, 20, Component.literal("Hex"));
            this.hexInput.setMaxLength(7);
            this.hexInput.setValue(String.format("#%06X", previewColor & 0xFFFFFF));
            this.hexInput.setResponder(this::onHexInputChanged);
            addContentWidget(hexInput, widgetY, 20); y += 24;
        } else if (colorMode == ColorMode.GRADIENT) {
            int fw = 80, sw = 18, pg = 10, pw = sw+6+fw;
            int gl = cx - (pw*2+pg)/2;
            widgetY = contentY(y);
            this.gradientHex1 = makeHexField(gl+sw+6, widgetY, gradientColor1, h -> { int p=parseHex(h); if(p>=0){gradientColor1=p;saveGradient();} });
            this.gradientHex2 = makeHexField(gl+pw+pg+sw+6, widgetY, gradientColor2, h -> { int p=parseHex(h); if(p>=0){gradientColor2=p;saveGradient();} });
            addContentWidget(gradientHex1, widgetY, 18); addContentWidget(gradientHex2, widgetY, 18); y += 26;
        }

        y += SECTION_GAP+1+SECTION_GAP+12;
        int colW = contentWidth/2;
        widgetY = contentY(y);
        addContentWidget(Checkbox.builder(Component.literal("Bold"),this.font).pos(left,widgetY).selected(previewBold).onValueChange((_,v)->previewBold=v).build(), widgetY, 20);
        addContentWidget(Checkbox.builder(Component.literal("Italic"),this.font).pos(left+colW,widgetY).selected(previewItalic).onValueChange((_,v)->previewItalic=v).build(), widgetY, 20);
        y+=22;
        widgetY = contentY(y);
        addContentWidget(Checkbox.builder(Component.literal("Underline"),this.font).pos(left,widgetY).selected(previewUnderlined).onValueChange((_,v)->previewUnderlined=v).build(), widgetY, 20);
        addContentWidget(Checkbox.builder(Component.literal("Strikethrough"),this.font).pos(left+colW,widgetY).selected(previewStrikethrough).onValueChange((_,v)->previewStrikethrough=v).build(), widgetY, 20);
        y+=22+SECTION_GAP+1+SECTION_GAP+12;

        List<String> animTypes = new ArrayList<>(AnimationRegistry.getAll().keySet());
        animTypes.removeAll(List.of("none","rainbow","gradient")); Collections.sort(animTypes);
        List<String> activeAnims = config.getAnimationTypes();
        int cbPerRow=contentWidth >= 240 ? 2 : 1, animCbW=(contentWidth-8)/cbPerRow, row=0, col=0;
        for (String animType : animTypes) {
            Animation anim = AnimationRegistry.get(animType);
            widgetY = contentY(y+row*22);
            addContentWidget(Checkbox.builder(Component.literal(anim.getName()),this.font)
                    .pos(left+col*(animCbW+8), widgetY).selected(activeAnims.contains(animType))
                    .onValueChange((_,v)->{
                        List<String> cur=new ArrayList<>(config.getAnimationTypes());
                        if(v){if(!cur.contains(animType))cur.add(animType);}else cur.remove(animType);
                        config.setAnimationTypesInternal(cur);
                    }).build(), widgetY, 20);
            col++; if(col>=cbPerRow){col=0;row++;}
        }
        y+=(row+(col>0?1:0))*22+4;
        widgetY = contentY(y);
        addContentWidget(new AnimationSpeedSlider(left,widgetY,contentWidth,20,
                Component.literal("Speed"),(config.getAnimationSpeed()-0.1f)/4.9f), widgetY, 20);
        y += 24;
        contentHeight = y;
        if(clampScroll()){reinitialize();return;}

        int bY=this.height-30, availableButtonWidth=Math.max(0,this.width-HORIZONTAL_MARGIN*2);
        int navW=Math.clamp((availableButtonWidth-8)/2,70,150);
        int navL=cx-(navW*2+8)/2;
        int bW=Math.clamp((availableButtonWidth-8)/3,58,100), bL=cx-(bW*3+8)/2;
        this.addRenderableWidget(Button.builder(Component.literal("Toggle Relay..."),_->minecraft.gui.setScreen(new DescriptiveRelayScreen(this))).bounds(navL,bY-24,navW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Rendering Toggles..."),_->minecraft.gui.setScreen(new DescriptiveToggleScreen(this))).bounds(navL+navW+8,bY-24,navW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§cReset..."),_->minecraft.gui.setScreen(new DescriptiveResetScreen(this))).bounds(bL,bY,bW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"),_->cancelAndClose()).bounds(bL+bW+4,bY,bW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Apply"),_->{
            config.setColorInternal(previewColor); config.setBoldInternal(previewBold);
            config.setItalicInternal(previewItalic); config.setUnderlinedInternal(previewUnderlined);
            config.setStrikethroughInternal(previewStrikethrough);
            config.setGradientColorsInternal(List.of(gradientColor1,gradientColor2));
            config.save(); minecraft.gui.setScreen(parent);
        }).bounds(bL+(bW+4)*2,bY,bW,20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float dt) {
        super.extractRenderState(g, mx, my, dt);
        int cx=width/2, contentWidth=getContentWidth(), left=cx-contentWidth/2, right=cx+contentWidth/2, y=12;
        g.centeredText(font,Component.literal("Descriptive Settings").withStyle(s->s.withBold(true)),cx,y,0xFFFFFFFF); y+=14;
        g.fill(left,y,right,y+1,0x44FFFFFF);
        y = contentY(0);
        drawTextIfVisible(g,Component.literal("Color").withStyle(s->s.withBold(true)),left,y); y+=12+24;
        if(colorMode==ColorMode.SOLID){
            y+=22+22+24;
            int hexW=80,sw=20,gap=6,sx=cx-(hexW+gap+sw)/2;
            if(isContentVisible(y,sw)){g.fill(sx,y,sx+sw,y+sw,0xFF000000); g.fill(sx+1,y+1,sx+sw-1,y+sw-1,previewColor|0xFF000000);} y+=24;
        } else if(colorMode==ColorMode.GRADIENT){
            int sw=18,pg=10,pw=sw+6+80,gl=cx-(pw*2+pg)/2;
            if(isContentVisible(y,sw)){drawSwatch(g,gl,y,gradientColor1,sw); drawSwatch(g,gl+pw+pg,y,gradientColor2,sw);} y+=26;
        }
        fillIfVisible(g,left,y,right,y+1); y+=1+SECTION_GAP;
        drawTextIfVisible(g,Component.literal("Formatting").withStyle(s->s.withBold(true)),left,y); y+=12+22+22+SECTION_GAP;
        fillIfVisible(g,left,y,right,y+1); y+=1+SECTION_GAP;
        drawTextIfVisible(g,Component.literal("Animations").withStyle(s->s.withBold(true)),left,y);
        drawScrollbar(g,right);
        if(showPreview()){
            int py=height-108;
            g.fill(left,py,right,py+1,0x33FFFFFF);
            g.text(font,Component.literal("Preview").withStyle(s->s.withBold(true)),left,py+8,0xFFFFFFFF);
            String pn=minecraft.player!=null?minecraft.player.getName().getString():"Preview";
            UUID pu=minecraft.player!=null?minecraft.player.getUUID():UUID.fromString("00000000-0000-0000-0000-000000000000");
            PlayerAnimationContext.setCurrentPlayer(pu); config.setColorInternal(previewColor);
            g.centeredText(font,NameBuilder.buildPreview(pn,pu,previewColor,previewBold,previewItalic,previewUnderlined,previewStrikethrough),cx,py+23,0xFFFFFFFF);
            g.fill(left,py+46,right,py+47,0x33FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){
        int maxScroll = getMaxScroll();
        if(maxScroll <= 0)return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
        int old = scrollOffset;
        scrollOffset = Math.clamp(scrollOffset-scrollStep(verticalAmount),0,maxScroll);
        if(scrollOffset != old){reinitialize();return true;}
        return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
    }

    private int getContentWidth(){return Math.clamp(width-HORIZONTAL_MARGIN*2,MIN_CONTENT_WIDTH,MAX_CONTENT_WIDTH);}
    private boolean showPreview(){return height>=220;}
    private int contentBottom(){return Math.max(CONTENT_TOP+32,showPreview()?height-114:height-58);}
    private int availableContentHeight(){return Math.max(32,contentBottom()-CONTENT_TOP);}
    private int getMaxScroll(){return Math.max(0,contentHeight-availableContentHeight());}
    private boolean clampScroll(){int old=scrollOffset;scrollOffset=Math.clamp(scrollOffset,0,getMaxScroll());return old!=scrollOffset;}
    private int contentY(int y){return CONTENT_TOP+y-scrollOffset;}
    private boolean isContentVisible(int y,int h){return y>=CONTENT_TOP&&y+h<=contentBottom();}
    private void addContentWidget(AbstractWidget widget,int y,int h){if(isContentVisible(y,h))this.addRenderableWidget(widget);}
    private void drawTextIfVisible(GuiGraphicsExtractor g,Component text,int x,int y){if(isContentVisible(y,10))g.text(font,text,x,y,0xFFFFFFFF);}
    private void fillIfVisible(GuiGraphicsExtractor g,int x1,int y1,int x2,int y2){if(isContentVisible(y1,y2-y1))g.fill(x1,y1,x2,y2,0x33FFFFFF);}
    private void drawScrollbar(GuiGraphicsExtractor g,int right){
        int maxScroll=getMaxScroll();
        if(maxScroll<=0)return;
        int top=CONTENT_TOP,bottom=contentBottom(),trackH=bottom-top;
        int thumbH=Math.max(16,trackH*trackH/Math.max(trackH,contentHeight));
        int thumbY=top+scrollOffset*(trackH-thumbH)/maxScroll;
        int x=right+5;
        g.fill(x,top,x+2,bottom,0x33000000);
        g.fill(x,thumbY,x+2,thumbY+thumbH,0x99FFFFFF);
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
    private int scrollStep(double verticalAmount){return verticalAmount>0?18:verticalAmount<0?-18:0;}
    private void cancelAndClose(){
        config.setAnimationTypesInternal(originalAnimationTypes);
        config.setAnimationSpeedInternal(originalAnimationSpeed);
        config.setGradientColorsInternal(List.of(originalGradientColor1,originalGradientColor2));
        minecraft.gui.setScreen(parent);
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
        void updateValue(double v){this.value=Math.clamp(v,0.0,1.0);applyValue();updateMessage();}
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
