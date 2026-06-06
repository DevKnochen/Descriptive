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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.*;

public class DescriptiveResetScreen extends Screen {
    private static final int MARGIN=30, LINE_H=13, MIN_CONTENT_WIDTH=180, MAX_CONTENT_WIDTH=420;
    private record ResetLine(String icon, int iconColor, String text){}
    private static final ResetLine[] CONSEQUENCES = {
            new ResetLine("✘",0xFFFF5555,"Custom color will be reset to white."),
            new ResetLine("✘",0xFFFF5555,"Bold, italic, underline and strikethrough will be disabled."),
            new ResetLine("✘",0xFFFF5555,"All animations will be disabled."),
            new ResetLine("✘",0xFFFF5555,"Gradient colors will be reset to defaults."),
            new ResetLine("✘",0xFFFF5555,"Per-player rendering toggles will be reset."),
    };
    private final @Nullable Screen parent;

    public DescriptiveResetScreen(@Nullable Screen parent) {
        super(Component.literal("Reset Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx=width/2, bY=height-30, bW=100;
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"),_->minecraft.setScreen(parent)).bounds(cx-bW-4,bY,bW,20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset").withStyle(s->s.withBold(true).withColor(ChatFormatting.RED)),_->{
            doReset();
            if(parent instanceof DescriptiveConfigScreen cs)minecraft.setScreen(new DescriptiveConfigScreen(cs.getParent()));
            else minecraft.setScreen(parent);
        }).bounds(cx+4,bY,bW,20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float dt) {
        super.extractRenderState(g, mx, my, dt);
        int cx=width/2, cw=Math.clamp(width-MARGIN*2,MIN_CONTENT_WIDTH,MAX_CONTENT_WIDTH), left=cx-cw/2, y=12;
        g.centeredText(font,Component.literal("Reset Settings").withStyle(s->s.withBold(true)),cx,y,0xFFFFFFFF); y+=14;
        g.fill(left,y,left+cw,y+1,0x44FFFFFF); y+=10;
        g.centeredText(font,Component.literal("The following settings will be permanently reset to their defaults."),cx,y,0xFFCCCCCC); y+=LINE_H+8;
        g.fill(left,y,left+cw,y+1,0x33FFFFFF); y+=10;
        g.text(font,Component.literal("Will be reset").withStyle(s->s.withBold(true).withColor(ChatFormatting.RED)),left,y,0xFFFFFFFF); y+=LINE_H+4;
        for(ResetLine l:CONSEQUENCES){g.text(font,Component.literal(l.icon()),left,y,l.iconColor());g.text(font,Component.literal(l.text()),left+14,y,0xFFCCCCCC);y+=LINE_H;}
        y+=10; g.fill(left,y,left+cw,y+1,0x33FFFFFF);
    }

    private void doReset(){
        DescriptiveClientConfig c=DescriptiveClient.getInstance().getConfig();
        c.setColorInternal(0xFFFFFF); c.setBoldInternal(false); c.setItalicInternal(false);
        c.setUnderlinedInternal(false); c.setStrikethroughInternal(false);
        c.setAnimationTypesInternal(new ArrayList<>()); c.setAnimationSpeedInternal(1.0f);
        c.setGradientColors(List.of(0xFF0000,0x0000FF)); c.setDisabledPlayers(Collections.emptySet()); c.save();
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
