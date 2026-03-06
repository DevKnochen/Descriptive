/*
 * Copyright 2026 DevKnochen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Portions of this file are copied from, derived from, or inspired from TextAnimator by Snownee.
 * Copyright 2023 Snownee. Licensed under the Apache License, Version 2.0
 */

package de.devknochen.descriptive.client.animation;

import de.devknochen.descriptive.client.DescriptiveClient;
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import de.devknochen.descriptive.config.DescriptiveClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AnimationRenderer {

    private static final ThreadLocal<Integer> CHAR_INDEX = ThreadLocal.withInitial(() -> 0);

    public static void resetCharIndex() {
        CHAR_INDEX.set(0);
    }

    @SuppressWarnings("unused")
    public static int getCurrentCharIndex() {
        return CHAR_INDEX.get();
    }

    public static int getAndIncrementCharIndex() {
        int current = CHAR_INDEX.get();
        CHAR_INDEX.set(current + 1);
        return current;
    }

    public static void applyEffects(EffectSettings settings, UUID playerUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        List<String> activeAnimations;
        float speed;
        int baseColor;
        List<Integer> gradientColors;

        if (client.player.getUuid().equals(playerUuid)) {
            DescriptiveClientConfig config = DescriptiveClient.getInstance().getConfig();
            activeAnimations = config.getAnimationTypes();
            speed = config.getAnimationSpeed();
            baseColor = config.getColor();
            gradientColors = config.getGradientColors();
        } else {
            CustomNameData data = CustomNameCache.get(playerUuid);
            if (data == null || !data.animationEnabled()) return;
            activeAnimations = data.animationTypes();
            speed = data.animationSpeed();
            baseColor = data.color();
            gradientColors = data.gradientColors();
        }

        if (activeAnimations.isEmpty()) return;

        int effectiveBase = baseColor;
        if (activeAnimations.contains("gradient") && !gradientColors.isEmpty()) {
            effectiveBase = gradientColors.getFirst();
        }
        settings.r = ((effectiveBase >> 16) & 0xFF) / 255.0f;
        settings.g = ((effectiveBase >> 8)  & 0xFF) / 255.0f;
        settings.b = (effectiveBase & 0xFF)          / 255.0f;

        long rawMillis = client.world.getTime() * 50L
                + (long)(client.getRenderTickCounter().getTickProgress(false) * 50f);
        long millis = rawMillis % 100000L;
        float time = millis * 0.001f * speed;

        for (String animType : activeAnimations) {
            applyAnimation(settings, animType, time, millis, gradientColors);
        }
    }

    private static void applyAnimation(EffectSettings settings, String animType,
                                       float time, long millis, List<Integer> gradientColors) {
        switch (animType) {
            case "shake"    -> applyShake(settings, millis);
            case "wave"     -> applyWave(settings, time);
            case "rainbow"  -> applyRainbow(settings, millis);
            case "wiggle"   -> applyWiggle(settings, time);
            case "pulse"    -> applyPulse(settings, time);
            case "bounce"   -> applyBounce(settings, time);
            case "swing"    -> applySwing(settings, millis);
            case "fade"     -> applyFade(settings, millis);
            case "pend"     -> applyPendulum(settings, millis);
            case "turb"     -> applyTurbulence(settings, time);
            case "glitch"   -> applyGlitch(settings, millis);
            case "gradient" -> applyGradient(settings, gradientColors);
        }
    }

    private static void applyShake(EffectSettings settings, long millis) {
        float t = millis * 0.01f;
        int seed = settings.index;
        Random dirRandom = new Random(seed);
        float dirX = dirRandom.nextFloat();
        float dirY = dirRandom.nextFloat();
        settings.offsetX += MathHelper.sin(t * 1.7f + seed) * 0.6f * dirX;
        settings.offsetY += MathHelper.sin(t * 2.3f + seed) * 0.6f * dirY;
    }

    private static void applyWave(EffectSettings settings, float time) {
        settings.offsetY += MathHelper.sin((time * 2.0f) + (settings.index * 0.5f)) * 2.0f;
    }

    private static void applyRainbow(EffectSettings settings, long millis) {
        float hue = ((millis * 0.02f) + (settings.index * 1.0f)) % 30f;
        int color = MathHelper.hsvToRgb(hue / 30f, 0.8f, 0.8f);
        settings.r = ((color >> 16) & 0xFF) / 255.0f;
        settings.g = ((color >> 8)  & 0xFF) / 255.0f;
        settings.b = (color & 0xFF)          / 255.0f;
    }

    private static void applyWiggle(EffectSettings settings, float time) {
        float phase = settings.index * 2.0f;
        float rad   = (float) Math.toRadians(settings.index * 137.5f);
        float dist  = MathHelper.sin((time * 2.0f) + phase) * 1.5f;
        settings.offsetX += MathHelper.cos(rad) * dist;
        settings.offsetY += MathHelper.sin(rad) * dist;
    }

    private static void applyPulse(EffectSettings settings, float time) {
        float k = 0.6f + 0.4f * (0.5f + 0.5f * MathHelper.sin(time * 2.0f));
        settings.r *= k;
        settings.g *= k;
        settings.b *= k;
    }

    private static void applyBounce(EffectSettings settings, float time) {
        float t = ((time - settings.index * 0.2f) % 1.0f);
        if (t < 0) t += 1.0f;

        float offset = 0;
        if (t < 0.2f) {
            offset = MathHelper.sin((t / 0.2f) * (float)(Math.PI / 2));
        } else if (t < 0.8f) {
            float bt = (t - 0.2f) / 0.6f;
            if (bt < 1f / 2.75f) {
                offset = 7.5625f * bt * bt;
            } else if (bt < 2f / 2.75f) {
                bt -= 1.5f / 2.75f;
                offset = 7.5625f * bt * bt + 0.75f;
            } else if (bt < 2.5f / 2.75f) {
                bt -= 2.25f / 2.75f;
                offset = 7.5625f * bt * bt + 0.9375f;
            } else {
                bt -= 2.625f / 2.75f;
                offset = 7.5625f * bt * bt + 0.984375f;
            }
            offset = 1 - offset;
        }
        settings.offsetY -= offset * 4.0f;
    }

    // Ported from Snownee's SwingEffect
    private static void applySwing(EffectSettings settings, long millis) {
        float t = millis * 0.003f + settings.index * 0.4f;
        settings.offsetX += MathHelper.sin(t) * 2.0f;
    }

    // Ported from Snownee's FadeEffect
    private static void applyFade(EffectSettings settings, long millis) {
        float t    = millis * 0.002f + settings.index * 0.4f;
        float minK = 0.1f;
        float k    = minK + (1f - minK) * (0.5f + 0.5f * MathHelper.sin(t));
        settings.r *= k;
        settings.g *= k;
        settings.b *= k;
    }

    private static void applyPendulum(EffectSettings settings, long millis) {
        double phase  = millis * 0.002 - settings.index * 0.1;
        float  swing  = (float) Math.sin(phase);
        settings.offsetX += swing * 3.0f;
        settings.offsetY += (float)(Math.sin(phase * 0.5) * 0.8f);
    }

    private static void applyTurbulence(EffectSettings settings, float time) {
        float t = time * 2.0f;
        settings.offsetX += MathHelper.sin(t * 1.7f + settings.index * 0.31f) * 1.5f;
        settings.offsetY += MathHelper.sin(t * 2.3f + settings.index * 0.27f) * 1.5f;
    }

    // Ported from Snownee's GlitchEffect (without siblings/mask which require pipeline changes)
    private static void applyGlitch(EffectSettings settings, long millis) {
        double time  = millis * 0.025;
        int    pulse = (int) time % 3;

        Random random = new Random(settings.index + (long)(time * 1000));
        random.nextFloat();

        if (pulse == 1 && random.nextFloat() < 0.015f) {
            settings.offsetX += (random.nextFloat() - 0.5f) * 8;
            settings.offsetY += (random.nextFloat() - 0.5f) * 4;
        }

        if (random.nextFloat() < 0.003f) {
            float blink = random.nextFloat() < 0.3f ? 0.0f : 0.3f;
            settings.r *= blink;
            settings.g *= blink;
            settings.b *= blink;
        }

        double time2   = time * 2;
        Random random2 = new Random((long)(time2) * 1000L * 12345L);
        if (random2.nextFloat() < 0.08f) {
            float offset = 0.75f + random2.nextFloat() * 0.75f;
            if (random2.nextBoolean()) offset = -offset;
            settings.offsetX += offset;
            if (random2.nextBoolean()) settings.r = Math.min(1f, settings.r + 0.5f);
            else                       settings.b = Math.min(1f, settings.b + 0.5f);
        }
    }

    private static void applyGradient(EffectSettings settings, List<Integer> gradientColors) {
        if (gradientColors == null || gradientColors.size() < 2) return;

        float t  = Math.min(settings.index / 16.0f, 1.0f);
        int   c1 = gradientColors.get(0);
        int   c2 = gradientColors.get(1);

        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;

        settings.r = (r1 + (r2 - r1) * t) / 255.0f;
        settings.g = (g1 + (g2 - g1) * t) / 255.0f;
        settings.b = (b1 + (b2 - b1) * t) / 255.0f;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean shouldAnimate(UUID playerUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;

        if (client.player.getUuid().equals(playerUuid)) {
            DescriptiveClientConfig cfg = DescriptiveClient.getInstance().getConfig();
            return cfg.isAnimationEnabled();
        } else {
            CustomNameData data = CustomNameCache.get(playerUuid);
            return data != null && data.animationEnabled() && !data.animationTypes().isEmpty();
        }
    }
}