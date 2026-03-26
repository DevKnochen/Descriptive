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

package de.devknochen.descriptive.client.animation;

import de.devknochen.descriptive.client.DescriptiveClient;
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AnimationRenderer {

    private static final ThreadLocal<Integer> CHAR_INDEX = ThreadLocal.withInitial(() -> 0);

    public static void resetCharIndex() { CHAR_INDEX.set(0); }

    @SuppressWarnings("unused")
    public static int getCurrentCharIndex() { return CHAR_INDEX.get(); }

    public static int getAndIncrementCharIndex() {
        int current = CHAR_INDEX.get();
        CHAR_INDEX.set(current + 1);
        return current;
    }

    public static void applyEffects(EffectSettings settings, UUID playerUuid) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        List<String> activeAnimations;
        float speed;
        int baseColor;
        List<Integer> gradientColors;

        if (client.player.getUUID().equals(playerUuid)) {
            DescriptiveClientConfig config = DescriptiveClient.getInstance().getConfig();
            activeAnimations = config.getAnimationTypes();
            speed            = config.getAnimationSpeed();
            baseColor        = config.getColor();
            gradientColors   = config.getGradientColors();
        } else {
            CustomNameData data = CustomNameCache.get(playerUuid);
            if (data == null || !data.animationEnabled()) return;
            activeAnimations = data.animationTypes();
            speed            = data.animationSpeed();
            baseColor        = data.color();
            gradientColors   = data.gradientColors();
        }

        if (activeAnimations.isEmpty()) return;

        int effectiveBase = baseColor;
        if (activeAnimations.contains("gradient") && !gradientColors.isEmpty())
            effectiveBase = gradientColors.getFirst();

        settings.r = ((effectiveBase >> 16) & 0xFF) / 255.0f;
        settings.g = ((effectiveBase >>  8) & 0xFF) / 255.0f;
        settings.b = ( effectiveBase        & 0xFF) / 255.0f;

        long rawMillis = client.level.getGameTime() * 50L
                + (long)(client.getDeltaTracker().getGameTimeDeltaPartialTick(false) * 50f);
        long millis = rawMillis % 100000L;
        float time  = millis * 0.001f * speed;

        for (String animType : activeAnimations)
            applyAnimation(settings, animType, time, millis, gradientColors);
    }

    private static void applyAnimation(EffectSettings settings, String animType,
                                       float time, long millis, List<Integer> gradientColors) {
        switch (animType) {
            case "shake"    -> applyShake(settings, time);
            case "wave"     -> applyWave(settings, time);
            case "rainbow"  -> applyRainbow(settings, millis);
            case "wiggle"   -> applyWiggle(settings, time);
            case "pulse"    -> applyPulse(settings, time);
            case "bounce"   -> applyBounce(settings, time);
            case "swing"    -> applySwing(settings, time);
            case "fade"     -> applyFade(settings, millis);
            case "pend"     -> applyPendulum(settings, time);
            case "turb"     -> applyTurbulence(settings, time);
            case "glitch"   -> applyGlitch(settings, millis);
            case "gradient" -> applyGradient(settings, gradientColors);
        }
    }

    private static void applyShake(EffectSettings s, float time) {
        int tick = Mth.floor(time * 22.0f + s.index * 0.37f);
        Random random = new Random(0x9E3779B97F4A7C15L + (long) s.index * 31L + tick * 17L);
        float amplitude = 1.6f;
        s.offsetX += (random.nextFloat() - 0.5f) * amplitude;
        s.offsetY += (random.nextFloat() - 0.5f) * amplitude;
    }
    private static void applyWave(EffectSettings s, float t) { s.offsetY += Mth.sin((t*2f)+(s.index*0.5f))*2f; }
    private static void applyRainbow(EffectSettings s, long millis) {
        float hue = ((millis*0.02f)+(s.index*1f))%30f;
        int c = Mth.hsvToRgb(hue/30f,0.8f,0.8f);
        s.r=((c>>16)&0xFF)/255f; s.g=((c>>8)&0xFF)/255f; s.b=(c&0xFF)/255f;
    }
    private static void applyWiggle(EffectSettings s, float t) {
        float phase = s.index * 0.55f;
        float direction = (float) Math.toRadians((s.index * 137.5f) % 360.0f);
        float distance = Mth.sin(t * 6.0f + phase) * 1.35f;
        s.offsetX += Mth.cos(direction) * distance;
        s.offsetY += Mth.sin(direction) * distance;
    }
    private static void applyPulse(EffectSettings s, float t) { float k=0.6f+0.4f*(0.5f+0.5f*Mth.sin(t*2f)); s.r*=k; s.g*=k; s.b*=k; }
    private static void applyBounce(EffectSettings s, float time) {
        float t=((time-s.index*0.2f)%1f); if(t<0)t+=1f;
        float off=0;
        if(t<0.2f){off=Mth.sin((t/0.2f)*(float)(Math.PI/2));}
        else if(t<0.8f){float bt=(t-0.2f)/0.6f;if(bt<1f/2.75f){off=7.5625f*bt*bt;}else if(bt<2f/2.75f){bt-=1.5f/2.75f;off=7.5625f*bt*bt+0.75f;}else if(bt<2.5f/2.75f){bt-=2.25f/2.75f;off=7.5625f*bt*bt+0.9375f;}else{bt-=2.625f/2.75f;off=7.5625f*bt*bt+0.984375f;}off=1-off;}
        s.offsetY-=off*4f;
    }
    private static void applySwing(EffectSettings s, float time) {
        float angle = Mth.sin(time * ((float) Math.PI * 2.0f) * 0.2f) * 0.85f;
        s.rotationRadians += angle;
        s.pivotXFactor = 0.5f;
        s.pivotYFactor = 0.5f;
    }
    private static void applyFade(EffectSettings s, long millis) { float k=0.1f+(0.9f*(0.5f+0.5f*Mth.sin(millis*0.002f+s.index*0.4f))); s.r*=k; s.g*=k; s.b*=k; }
    private static void applyPendulum(EffectSettings s, float time) {
        float angle = Mth.sin(time * ((float) Math.PI * 2.0f) * 0.3f) * ((float) Math.toRadians(30.0));
        s.rotationRadians += angle;
        s.pivotXFactor = 0.0f;
        s.pivotYFactor = 0.0f;
    }
    private static void applyTurbulence(EffectSettings s, float time) { float t=time*2f; s.offsetX+=Mth.sin(t*1.7f+s.index*0.31f)*1.5f; s.offsetY+=Mth.sin(t*2.3f+s.index*0.27f)*1.5f; }
    private static void applyGlitch(EffectSettings s, long millis) {
        double time=millis*0.025; int pulse=(int)time%3;
        Random r=new Random(s.index+(long)(time*1000)); r.nextFloat();
        if(pulse==1&&r.nextFloat()<0.015f){s.offsetX+=(r.nextFloat()-0.5f)*8;s.offsetY+=(r.nextFloat()-0.5f)*4;}
        if(r.nextFloat()<0.003f){float b=r.nextFloat()<0.3f?0f:0.3f;s.r*=b;s.g*=b;s.b*=b;}
        Random r2=new Random((long)(time*2)*1000L*12345L);
        if(r2.nextFloat()<0.08f){float off=0.75f+r2.nextFloat()*0.75f;if(r2.nextBoolean())off=-off;s.offsetX+=off;if(r2.nextBoolean())s.r=Math.min(1f,s.r+0.5f);else s.b=Math.min(1f,s.b+0.5f);}
    }
    private static void applyGradient(EffectSettings s, List<Integer> gc) {
        if(gc==null||gc.size()<2)return;
        float t=Math.min(s.index/16f,1f); int c1=gc.get(0),c2=gc.get(1);
        int r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF,r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
        s.r=(r1+(r2-r1)*t)/255f; s.g=(g1+(g2-g1)*t)/255f; s.b=(b1+(b2-b1)*t)/255f;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean shouldAnimate(UUID playerUuid) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        if (client.player.getUUID().equals(playerUuid))
            return DescriptiveClient.getInstance().getConfig().isAnimationEnabled();
        CustomNameData data = CustomNameCache.get(playerUuid);
        return data != null && data.animationEnabled() && !data.animationTypes().isEmpty();
    }
}
