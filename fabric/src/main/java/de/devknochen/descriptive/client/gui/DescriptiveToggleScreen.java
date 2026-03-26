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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import java.util.*;

public class DescriptiveToggleScreen extends Screen {
    private static final int HEAD_SIZE=16, ROW_H=26, BTN_W=100, BTN_H=20, MARGIN=30;
    private final @Nullable Screen parent;
    private final DescriptiveClientConfig config;
    private final List<UUID> descriptivePlayers = new ArrayList<>();
    private Set<UUID> pendingDisabled, originalDisabled;

    public DescriptiveToggleScreen(@Nullable Screen parent) {
        super(Component.literal("Rendering Toggles"));
        this.parent = parent;
        this.config = DescriptiveClient.getInstance().getConfig();
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        int cx=width/2, btnX=width-MARGIN-BTN_W;
        pendingDisabled = new HashSet<>(config.getDisabledPlayers());
        originalDisabled = new HashSet<>(config.getDisabledPlayers());
        descriptivePlayers.clear();
        if (mc.player != null) {
            UUID my = mc.player.getUUID();
            for (UUID u : CustomNameCache.getAllEntries().keySet()) if (!u.equals(my)) descriptivePlayers.add(u);
        }
        int y = 30+12;
        if (mc.player != null) addToggleBtn(btnX, y, mc.player.getUUID());
        y += ROW_H+MARGIN+12;
        for (UUID u : descriptivePlayers) { addToggleBtn(btnX, y, u); y += ROW_H; }
        int bY=height-30, bW=100;
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"),_b->cancelAndClose()).bounds(cx-bW-4,bY,bW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Apply"),_b->applyAndClose()).bounds(cx+4,bY,bW,20).build());
    }

    private void addToggleBtn(int x,int y,UUID uuid) {
        boolean enabled = !pendingDisabled.contains(uuid);
        Button btn = Button.builder(toggleLabel(enabled), _b -> {
            if(pendingDisabled.contains(uuid))pendingDisabled.remove(uuid);else pendingDisabled.add(uuid);
            config.setDisabledPlayers(pendingDisabled);
            _b.setMessage(toggleLabel(!pendingDisabled.contains(uuid)));
        }).bounds(x, y+(HEAD_SIZE/2)-(BTN_H/2), BTN_W, BTN_H).build();
        this.addRenderableWidget(btn);
    }

    private Component toggleLabel(boolean on) {
        return Component.literal(on?"✔ Enabled":"✘ Disabled").withStyle(s->s.withBold(true).withColor(on?ChatFormatting.GREEN:ChatFormatting.RED));
    }
    private void applyAndClose(){config.setDisabledPlayers(pendingDisabled);config.save();if(minecraft!=null)minecraft.setScreen(parent);}
    private void cancelAndClose(){config.setDisabledPlayers(originalDisabled);if(minecraft!=null)minecraft.setScreen(parent);}
    @Override public void onClose(){cancelAndClose();}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        super.extractRenderState(g, mx, my, dt);
        Minecraft mc = Minecraft.getInstance();
        int cx=width/2, left=MARGIN, y=10;
        g.centeredText(font,Component.literal("Rendering Toggles").withStyle(s->s.withBold(true)),cx,y,0xFFFFFFFF); y+=12;
        g.fill(left,y,width-MARGIN,y+1,0x44FFFFFF); y+=8;
        g.text(font,Component.literal("You").withStyle(s->s.withBold(true).withColor(ChatFormatting.YELLOW)),left,y,0xFFFFFFFF); y+=12;
        if(mc.player!=null){
            drawHead(g,mc.player.getUUID(),left,y);
            g.text(font,NameBuilder.buildCustomName(mc.player.getName().getString()),left+HEAD_SIZE+6,y+(HEAD_SIZE/2)-4,0xFFFFFFFF);
        }
        y+=ROW_H+MARGIN/2;
        g.fill(left,y,width-MARGIN,y+1,0x33FFFFFF); y+=MARGIN/2;
        g.text(font,Component.literal("Other Players").withStyle(s->s.withBold(true).withColor(ChatFormatting.AQUA)),left,y,0xFFFFFFFF); y+=12;
        if(descriptivePlayers.isEmpty()){
            g.fill(left,y,left+HEAD_SIZE,y+HEAD_SIZE,0xFF555555);
            g.text(font,Component.literal("No other Descriptive players online").withStyle(s->s.withColor(ChatFormatting.GRAY)),left+HEAD_SIZE+6,y+(HEAD_SIZE/2)-4,0xFFFFFFFF);
        } else {
            for(UUID u:descriptivePlayers){
                drawHead(g,u,left,y);
                String name=getPlayerName(mc,u);
                g.text(font,NameBuilder.buildCustomName(u,name),left+HEAD_SIZE+6,y+(HEAD_SIZE/2)-4,0xFFFFFFFF);
                y+=ROW_H;
            }
        }
    }

    private void drawHead(GuiGraphicsExtractor g,UUID uuid,int x,int y){
        if(minecraft==null||minecraft.getConnection()==null)return;
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