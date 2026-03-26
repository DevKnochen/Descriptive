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
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;

public class DescriptiveRelayScreen extends Screen {
    private static final int MARGIN=30, LINE_H=13, TEXT_INDENT=14;
    private static final String RELAY_URL="wss://relay-descriptive.knochenn.de/sync";
    private final @Nullable Screen parent;
    private final DescriptiveClientConfig config;
    private boolean accepted, relayEnabled;
    @SuppressWarnings("FieldCanBeLocal") private Checkbox acceptCheckbox;
    private Button toggleButton, checkStatusButton;
    private Boolean relayOnline=null;
    private boolean checkingRelay=false;
    private long pingMs=-1;

    private record ContentLine(@Nullable String icon, int iconColor, String text){}
    private static final ContentLine[] BENEFITS={
            new ContentLine("✔",0xFF55FF55,"Custom names will work on any server, vanilla or modded."),
            new ContentLine("✔",0xFF55FF55,"Connection opens on server join and closes when you"),
            new ContentLine(null,0,"leave. No persistent background process."),
            new ContentLine("✔",0xFF55FF55,"You can enable or disable this function at any time."),
            new ContentLine("✔",0xFF55FF55,"The relay connection code is open source."),
    };
    private static final ContentLine[] WARNINGS={
            new ContentLine("⚠",0xFFFFFF55,"Only your Minecraft UUID, styling data and current server"),
            new ContentLine(null,0,"address are transmitted to an external websocket server"),
            new ContentLine(null,0,"called relay."),
            new ContentLine("⚠",0xFFFFFF55,"Transmitted data is never stored. It exists only"),
            new ContentLine(null,0,"in the relay's RAM and is cleared on disconnect."),
            new ContentLine("⚠",0xFFFFFF55,"The relay is community-run on a best-effort basis."),
            new ContentLine(null,0,"There is no guaranteed uptime."),
    };

    public DescriptiveRelayScreen(@Nullable Screen parent){
        super(Component.literal("Relay Settings"));
        this.parent=parent; this.config=DescriptiveClient.getInstance().getConfig();
        this.relayEnabled=config.isRelayEnabled(); this.accepted=config.isRelayEnabled();
    }

    @Override
    protected void init(){
        int cx=width/2, cw=Math.min(420,width-MARGIN*2), left=cx-cw/2;
        acceptCheckbox=Checkbox.builder(Component.literal("I have read and accept the above information."),font)
                .pos(left,getCheckboxY()).selected(accepted)
                .onValueChange((_c,v)->{
                    accepted=v;
                    if(!accepted&&relayEnabled){relayEnabled=false;config.setRelayEnabled(false);config.save();}
                    updateToggleButton();
                }).build();
        this.addRenderableWidget(acceptCheckbox);
        int bY=height-30, bW=100;
        this.addRenderableWidget(Button.builder(Component.literal("Go Back"),_b->{if(minecraft!=null)minecraft.setScreen(parent);}).bounds(cx-bW-4,bY,bW,20).build());
        toggleButton=Button.builder(toggleLabel(relayEnabled),_b->{
            relayEnabled=!relayEnabled; config.setRelayEnabled(relayEnabled); config.save(); _b.setMessage(toggleLabel(relayEnabled));
        }).bounds(cx+4,bY,bW,20).build();
        this.addRenderableWidget(toggleButton); updateToggleButton();
        checkStatusButton=Button.builder(Component.literal("Check Status"),_b->pingRelay()).bounds(cx-50,bY-26,100,20).build();
        this.addRenderableWidget(checkStatusButton);
    }

    private void pingRelay(){
        if(checkingRelay)return; checkingRelay=true; relayOnline=null; pingMs=-1;
        checkStatusButton.active=false; checkStatusButton.setMessage(Component.literal("Checking…"));
        long start=System.currentTimeMillis();
        CompletableFuture.runAsync(()->{
            try(HttpClient hc=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()){
                hc.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).buildAsync(URI.create(RELAY_URL),new WebSocket.Listener(){
                    @Override public void onOpen(WebSocket ws){pingMs=System.currentTimeMillis()-start;relayOnline=true;ws.sendClose(WebSocket.NORMAL_CLOSURE,"ping").thenRun(()->resetBtn());}
                    @Override public CompletionStage<?> onClose(WebSocket ws,int c,String r){resetBtn();return WebSocket.Listener.super.onClose(ws,c,r);}
                    @Override public void onError(WebSocket ws,Throwable e){relayOnline=false;pingMs=-1;resetBtn();}
                }).exceptionally(e->{relayOnline=false;pingMs=-1;resetBtn();return null;}).join();
            }catch(Exception e){relayOnline=false;resetBtn();}
        });
    }
    private void resetBtn(){checkingRelay=false;if(minecraft!=null)minecraft.execute(()->{checkStatusButton.active=true;checkStatusButton.setMessage(Component.literal("Check Status"));});}
    private void updateToggleButton(){toggleButton.active=accepted;toggleButton.setMessage(toggleLabel(relayEnabled));}
    private Component toggleLabel(boolean on){return Component.literal("Relay: ").append(Component.literal(on?"Enabled":"Disabled").withStyle(s->s.withBold(true).withColor(on?ChatFormatting.GREEN:ChatFormatting.RED)));}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt){
        super.extractRenderState(g,mx,my,dt);
        int cx=width/2, cw=Math.min(420,width-MARGIN*2), left=cx-cw/2, y=12;
        g.centeredText(font,Component.literal("Relay Settings").withStyle(s->s.withBold(true)),cx,y,0xFFFFFFFF); y+=14;
        g.fill(left,y,left+cw,y+1,0x44FFFFFF); y+=10;
        g.centeredText(font,Component.literal("Descriptive would like to connect to a external Server to sync custom names."),cx,y,0xFFCCCCCC); y+=LINE_H+8;
        g.text(font,Component.literal("Benefits").withStyle(s->s.withBold(true).withColor(ChatFormatting.GREEN)),left,y,0xFFFFFFFF); y+=LINE_H+2;
        y=drawLines(g,left,y,BENEFITS); y+=8;
        g.text(font,Component.literal("Important to know").withStyle(s->s.withBold(true).withColor(ChatFormatting.YELLOW)),left,y,0xFFFFFFFF); y+=LINE_H+2;
        y=drawLines(g,left,y,WARNINGS); y+=10;
        g.fill(left,y,left+cw,y+1,0x33FFFFFF);
        int sY=height-30-26-18;
        if(checkingRelay){
            int dots=(int)((System.currentTimeMillis()/400)%4);
            g.centeredText(font,Component.literal("Pinging relay  "+"●".repeat(dots)+"○".repeat(3-dots)).withStyle(s->s.withColor(ChatFormatting.GRAY)),cx,sY,0xFFFFFFFF);
        } else if(relayOnline==null){
            g.centeredText(font,Component.literal("Check the relay's status.").withStyle(s->s.withColor(ChatFormatting.GRAY).withItalic(true)),cx,sY,0xFFFFFFFF);
        } else if(relayOnline){
            g.centeredText(font,Component.literal("● Relay Online"+(pingMs>=0?"  ("+pingMs+" ms)":"")).withStyle(s->s.withBold(true).withColor(ChatFormatting.GREEN)),cx,sY,0xFFFFFFFF);
        } else {
            g.centeredText(font,Component.literal("● Relay Offline or unreachable").withStyle(s->s.withBold(true).withColor(ChatFormatting.RED)),cx,sY,0xFFFFFFFF);
        }
    }

    private int drawLines(GuiGraphicsExtractor g,int left,int y,ContentLine[] lines){
        for(ContentLine l:lines){if(l.icon()!=null)g.text(font,Component.literal(l.icon()),left,y,l.iconColor());g.text(font,Component.literal(l.text()),left+TEXT_INDENT,y,0xFFCCCCCC);y+=LINE_H;}return y;
    }
    private int getCheckboxY(){int y=12+14+10+LINE_H+8;y+=LINE_H+2+BENEFITS.length*LINE_H+8;y+=LINE_H+2+WARNINGS.length*LINE_H+10;y+=1+8;return y;}
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
}