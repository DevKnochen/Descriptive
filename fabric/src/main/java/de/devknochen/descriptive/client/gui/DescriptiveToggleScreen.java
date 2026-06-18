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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.*;

public class DescriptiveToggleScreen extends Screen {
    private static final int HEAD_SIZE=16, ROW_H=26, BTN_H=20;
    private static final int MIN_CONTENT_WIDTH=180, MAX_CONTENT_WIDTH=520, HORIZONTAL_MARGIN=20, CONTENT_TOP=36;
    private final @Nullable Screen parent;
    private final DescriptiveClientConfig config;
    private final List<UUID> descriptivePlayers = new ArrayList<>();
    private final Set<UUID> pendingDisabled;
    private final Set<UUID> originalDisabled;
    private int scrollOffset=0;
    private int contentHeight=0;

    public DescriptiveToggleScreen(@Nullable Screen parent) {
        super(Component.literal("Rendering Toggles"));
        this.parent = parent;
        this.config = DescriptiveClient.getInstance().getConfig();
        this.pendingDisabled = new HashSet<>(config.getDisabledPlayers());
        this.originalDisabled = new HashSet<>(config.getDisabledPlayers());
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        int cx=width/2, contentWidth=getContentWidth(), left=cx-contentWidth/2, right=left+contentWidth, btnW=getButtonWidth(contentWidth), btnX=right-btnW;
        descriptivePlayers.clear();
        if (mc.player != null) {
            UUID my = mc.player.getUUID();
            for (UUID u : CustomNameCache.getAllEntries().keySet()) if (!u.equals(my)) descriptivePlayers.add(u);
        }
        int y = 12;
        if (mc.player != null) addToggleBtn(btnX, y, mc.player.getUUID());
        y += ROW_H+24+12;
        for (UUID u : descriptivePlayers) { addToggleBtn(btnX, y, u); y += ROW_H; }
        if(descriptivePlayers.isEmpty())y += ROW_H;
        contentHeight=y+8;
        if(clampScroll()){reinitialize();return;}
        int bY=height-30, bW=100;
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"),_->cancelAndClose()).bounds(cx-bW-4,bY,bW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Apply"),_->applyAndClose()).bounds(cx+4,bY,bW,20).build());
    }

    private void addToggleBtn(int x,int y,UUID uuid) {
        boolean enabled = !pendingDisabled.contains(uuid);
        int buttonY=contentY(y)+(HEAD_SIZE/2)-(BTN_H/2);
        Button btn = Button.builder(toggleLabel(enabled), button -> {
            if(pendingDisabled.contains(uuid))pendingDisabled.remove(uuid);else pendingDisabled.add(uuid);
            config.setDisabledPlayers(pendingDisabled);
            button.setMessage(toggleLabel(!pendingDisabled.contains(uuid)));
        }).bounds(x, buttonY, getButtonWidth(getContentWidth()), BTN_H).build();
        addContentWidget(btn,buttonY);
    }

    private Component toggleLabel(boolean on) {
        return Component.literal(on?"✔ Enabled":"✘ Disabled").withStyle(s->s.withBold(true).withColor(on?ChatFormatting.GREEN:ChatFormatting.RED));
    }
    private void applyAndClose(){config.setDisabledPlayers(pendingDisabled);config.save();minecraft.gui.setScreen(parent);}
    private void cancelAndClose(){config.setDisabledPlayers(originalDisabled);minecraft.gui.setScreen(parent);}
    @Override public void onClose(){cancelAndClose();}

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float dt) {
        super.extractRenderState(g, mx, my, dt);
        Minecraft mc = Minecraft.getInstance();
        int cx=width/2, contentWidth=getContentWidth(), left=cx-contentWidth/2, right=left+contentWidth, y=10;
        g.centeredText(font,Component.literal("Rendering Toggles").withStyle(s->s.withBold(true)),cx,y,0xFFFFFFFF); y+=12;
        g.fill(left,y,right,y+1,0x44FFFFFF);
        y=contentY(0);
        drawTextIfVisible(g,Component.literal("You").withStyle(s->s.withBold(true).withColor(ChatFormatting.YELLOW)),left,y); y+=12;
        if(mc.player!=null){
            drawHeadIfVisible(g,mc.player.getUUID(),left,y);
            drawTextIfVisible(g,NameBuilder.buildCustomName(mc.player.getName().getString()),left+HEAD_SIZE+6,y+(HEAD_SIZE/2)-4);
        }
        y+=ROW_H+12;
        fillIfVisible(g,left,y,right,y+1); y+=12;
        drawTextIfVisible(g,Component.literal("Other Players").withStyle(s->s.withBold(true).withColor(ChatFormatting.AQUA)),left,y); y+=12;
        if(descriptivePlayers.isEmpty()){
            if(isContentVisible(y,HEAD_SIZE))g.fill(left,y,left+HEAD_SIZE,y+HEAD_SIZE,0xFF555555);
            drawTextIfVisible(g,Component.literal("No other Descriptive players online").withStyle(s->s.withColor(ChatFormatting.GRAY)),left+HEAD_SIZE+6,y+(HEAD_SIZE/2)-4);
        } else {
            for(UUID u:descriptivePlayers){
                drawHeadIfVisible(g,u,left,y);
                String name=getPlayerName(mc,u);
                drawTextIfVisible(g,NameBuilder.buildCustomName(u,name),left+HEAD_SIZE+6,y+(HEAD_SIZE/2)-4);
                y+=ROW_H;
            }
        }
        drawScrollbar(g,right);
    }

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){
        int maxScroll=getMaxScroll();
        if(maxScroll<=0)return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
        int old=scrollOffset;
        scrollOffset=Math.clamp(scrollOffset-scrollStep(verticalAmount),0,maxScroll);
        if(scrollOffset!=old){reinitialize();return true;}
        return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
    }

    private int getContentWidth(){return Math.clamp(width-HORIZONTAL_MARGIN*2,MIN_CONTENT_WIDTH,MAX_CONTENT_WIDTH);}
    private int getButtonWidth(int contentWidth){return Math.clamp(contentWidth/4,76,100);}
    private int contentBottom(){return Math.max(CONTENT_TOP+32,height-58);}
    private int availableContentHeight(){return Math.max(32,contentBottom()-CONTENT_TOP);}
    private int getMaxScroll(){return Math.max(0,contentHeight-availableContentHeight());}
    private boolean clampScroll(){int old=scrollOffset;scrollOffset=Math.clamp(scrollOffset,0,getMaxScroll());return old!=scrollOffset;}
    private int contentY(int y){return CONTENT_TOP+y-scrollOffset;}
    private boolean isContentVisible(int y,int h){return y>=CONTENT_TOP&&y+h<=contentBottom();}
    private void addContentWidget(AbstractWidget widget,int y){if(isContentVisible(y,BTN_H))this.addRenderableWidget(widget);}
    private void drawTextIfVisible(GuiGraphicsExtractor g,Component text,int x,int y){if(isContentVisible(y,10))g.text(font,text,x,y,0xFFFFFFFF);}
    private void drawHeadIfVisible(GuiGraphicsExtractor g,UUID uuid,int x,int y){if(isContentVisible(y,HEAD_SIZE))drawHead(g,uuid,x,y);}
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
    private void reinitialize(){clearWidgets();init();}
    private int scrollStep(double verticalAmount){return verticalAmount>0?18:verticalAmount<0?-18:0;}

    private void drawHead(GuiGraphicsExtractor g,UUID uuid,int x,int y){
        if(minecraft.getConnection()==null)return;
        PlayerInfo entry=minecraft.getConnection().getPlayerInfo(uuid);
        if(entry==null){g.fill(x,y,x+HEAD_SIZE,y+HEAD_SIZE,0xFF555555);return;}
        // PlayerSkin.body() returns ClientAsset.Texture - get identifier from it
        Identifier skin = entry.getSkin().body().id();
        g.blit(RenderPipelines.GUI_TEXTURED,skin,x,y,8.0f,8.0f,HEAD_SIZE,HEAD_SIZE,64,64);
        g.blit(RenderPipelines.GUI_TEXTURED,skin,x,y,40.0f,8.0f,HEAD_SIZE,HEAD_SIZE,64,64);
    }
    private String getPlayerName(Minecraft mc,UUID uuid){
        if(mc.getConnection()==null)return uuid.toString().substring(0,8);
        PlayerInfo e=mc.getConnection().getPlayerInfo(uuid);
        return e!=null?e.getProfile().name():uuid.toString().substring(0,8);
    }
}
