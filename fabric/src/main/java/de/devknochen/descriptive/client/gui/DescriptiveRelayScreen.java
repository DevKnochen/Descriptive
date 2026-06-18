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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class DescriptiveRelayScreen extends Screen {
    private static final int MIN_CONTENT_WIDTH=180, MAX_CONTENT_WIDTH=420, HORIZONTAL_MARGIN=20, CONTENT_TOP=36;
    private static final int LINE_H=13, TEXT_INDENT=14;
    private static final String RELAY_URL="wss://relay-descriptive.knochenn.de/sync";
    private final @Nullable Screen parent;
    private final DescriptiveClientConfig config;
    private boolean accepted, relayEnabled;
    @SuppressWarnings("FieldCanBeLocal") private Checkbox acceptCheckbox;
    private Button toggleButton, checkStatusButton;
    private @Nullable Boolean relayOnline=null;
    private boolean checkingRelay=false;
    private long pingMs=-1;
    private int scrollOffset=0;
    private int contentHeight=0;

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
        int cx=width/2, cw=getContentWidth(), left=cx-cw/2;
        int checkboxY=contentY(getCheckboxOffset(cw));
        acceptCheckbox=Checkbox.builder(Component.literal("I have read and accept the above information."),font)
                .pos(left,checkboxY).selected(accepted)
                .onValueChange((_,v)->{
                    accepted=v;
                    if(!accepted&&relayEnabled){relayEnabled=false;config.setRelayEnabled(false);config.save();}
                    updateToggleButton();
                }).build();
        contentHeight=getCheckboxOffset(cw)+24;
        if(clampScroll()){reinitialize();return;}
        addContentWidget(acceptCheckbox,checkboxY);
        int bY=height-30, bW=100;
        this.addRenderableWidget(Button.builder(Component.literal("Go Back"),_->minecraft.gui.setScreen(parent)).bounds(cx-bW-4,bY,bW,20).build());
        toggleButton=Button.builder(toggleLabel(relayEnabled),button->{
            relayEnabled=!relayEnabled; config.setRelayEnabled(relayEnabled); config.save(); button.setMessage(toggleLabel(relayEnabled));
        }).bounds(cx+4,bY,bW,20).build();
        this.addRenderableWidget(toggleButton); updateToggleButton();
        checkStatusButton=Button.builder(Component.literal("Check Status"),_->pingRelay()).bounds(cx-50,bY-26,100,20).build();
        this.addRenderableWidget(checkStatusButton);
    }

    private void pingRelay(){
        if(checkingRelay)return; checkingRelay=true; relayOnline=null; pingMs=-1;
        checkStatusButton.active=false; checkStatusButton.setMessage(Component.literal("Checking…"));
        long start=System.currentTimeMillis();
        CompletableFuture.runAsync(()->{
            try(HttpClient hc=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()){
                hc.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).buildAsync(URI.create(RELAY_URL),new WebSocket.Listener(){
                    @Override public void onOpen(@NonNull WebSocket ws){pingMs=System.currentTimeMillis()-start;relayOnline=true;ws.sendClose(WebSocket.NORMAL_CLOSURE,"ping").thenRun(()->resetBtn());}
                    @Override public CompletionStage<?> onClose(@NonNull WebSocket ws,int statusCode,@NonNull String reason){resetBtn();return WebSocket.Listener.super.onClose(ws,statusCode,reason);}
                    @Override public void onError(@NonNull WebSocket ignoredWebSocket,@NonNull Throwable ignored){relayOnline=false;pingMs=-1;resetBtn();}
                }).exceptionally(ignored->{relayOnline=false;pingMs=-1;resetBtn();return null;}).join();
            }catch(Exception ignored){relayOnline=false;resetBtn();}
        });
    }
    private void resetBtn(){checkingRelay=false;minecraft.execute(()->{checkStatusButton.active=true;checkStatusButton.setMessage(Component.literal("Check Status"));});}
    private void updateToggleButton(){toggleButton.active=accepted;toggleButton.setMessage(toggleLabel(relayEnabled));}
    private Component toggleLabel(boolean on){return Component.literal("Relay: ").append(Component.literal(on?"Enabled":"Disabled").withStyle(s->s.withBold(true).withColor(on?ChatFormatting.GREEN:ChatFormatting.RED)));}

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor g, int mx, int my, float dt){
        super.extractRenderState(g,mx,my,dt);
        int cx=width/2, cw=getContentWidth(), left=cx-cw/2, right=left+cw, y=12;
        g.centeredText(font,Component.literal("Relay Settings").withStyle(s->s.withBold(true)),cx,y,0xFFFFFFFF); y+=14;
        g.fill(left,y,right,y+1,0x44FFFFFF);
        y=contentY(0);
        y=drawIntro(g,cx,y,cw)+8;
        drawTextIfVisible(g,Component.literal("Benefits").withStyle(s->s.withBold(true).withColor(ChatFormatting.GREEN)),left,y,0xFFFFFFFF); y+=LINE_H+2;
        y=drawLines(g,left,y,cw,BENEFITS); y+=8;
        drawTextIfVisible(g,Component.literal("Important to know").withStyle(s->s.withBold(true).withColor(ChatFormatting.YELLOW)),left,y,0xFFFFFFFF); y+=LINE_H+2;
        y=drawLines(g,left,y,cw,WARNINGS); y+=10;
        fillIfVisible(g,left,y,right,y+1);
        drawScrollbar(g,right);
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

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){
        int maxScroll=getMaxScroll();
        if(maxScroll<=0)return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
        int old=scrollOffset;
        scrollOffset=Math.clamp(scrollOffset-scrollStep(verticalAmount),0,maxScroll);
        if(scrollOffset!=old){reinitialize();return true;}
        return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
    }

    private int drawLines(GuiGraphicsExtractor g,int left,int y,int cw,ContentLine[] lines){
        for(ContentLine l:lines){
            List<String> wrapped=wrapText(l.text(),cw-TEXT_INDENT);
            for(int i=0;i<wrapped.size();i++){
                if(i==0&&l.icon()!=null&&isContentVisible(y,10))g.text(font,Component.literal(l.icon()),left,y,l.iconColor());
                drawTextIfVisible(g,Component.literal(wrapped.get(i)),left+TEXT_INDENT,y,0xFFCCCCCC);
                y+=LINE_H;
            }
        }
        return y;
    }
    private int drawIntro(GuiGraphicsExtractor g,int cx,int y,int width){
        for(String line:wrapText("Descriptive would like to connect to an external server to sync custom names.",width)){
            if(isContentVisible(y,10))g.centeredText(font,Component.literal(line),cx,y,0xFFCCCCCC);
            y+=LINE_H;
        }
        return y;
    }
    private List<String> wrapText(String text,int maxWidth){
        List<String> lines=new ArrayList<>();
        StringBuilder current=new StringBuilder();
        for(String word:text.split(" ")){
            String next=current.isEmpty()?word:current+" "+word;
            if(font.width(next)<=maxWidth||current.isEmpty())current=new StringBuilder(next);
            else{lines.add(current.toString());current=new StringBuilder(word);}
        }
        if(!current.isEmpty())lines.add(current.toString());
        return lines;
    }
    private int getCheckboxOffset(int cw){
        int y=0;
        y+=wrapText("Descriptive would like to connect to an external server to sync custom names.",cw).size()*LINE_H+8;
        y+=LINE_H+2;
        for(ContentLine l:BENEFITS)y+=wrapText(l.text(),cw-TEXT_INDENT).size()*LINE_H;
        y+=8+LINE_H+2;
        for(ContentLine l:WARNINGS)y+=wrapText(l.text(),cw-TEXT_INDENT).size()*LINE_H;
        return y+10+1+8;
    }
    private int getContentWidth(){return Math.clamp(width-HORIZONTAL_MARGIN*2,MIN_CONTENT_WIDTH,MAX_CONTENT_WIDTH);}
    private int contentBottom(){return Math.max(CONTENT_TOP+32,height-84);}
    private int availableContentHeight(){return Math.max(32,contentBottom()-CONTENT_TOP);}
    private int getMaxScroll(){return Math.max(0,contentHeight-availableContentHeight());}
    private boolean clampScroll(){int old=scrollOffset;scrollOffset=Math.clamp(scrollOffset,0,getMaxScroll());return old!=scrollOffset;}
    private int contentY(int y){return CONTENT_TOP+y-scrollOffset;}
    private boolean isContentVisible(int y,int h){return y>=CONTENT_TOP&&y+h<=contentBottom();}
    private void addContentWidget(AbstractWidget widget,int y){if(isContentVisible(y,20))this.addRenderableWidget(widget);}
    private void drawTextIfVisible(GuiGraphicsExtractor g,Component text,int x,int y,int color){if(isContentVisible(y,10))g.text(font,text,x,y,color);}
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
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
