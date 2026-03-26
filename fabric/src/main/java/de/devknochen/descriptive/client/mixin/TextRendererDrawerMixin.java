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

package de.devknochen.descriptive.client.mixin;

import de.devknochen.descriptive.client.animation.AnimatedStyleMarker;
import de.devknochen.descriptive.client.animation.AnimationRenderer;
import de.devknochen.descriptive.client.animation.EffectSettings;
import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.client.animation.TransformedGlyphRenderable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public class TextRendererDrawerMixin {

    @Unique private Style descriptive$currentStyle = null;
    @Unique private EffectSettings descriptive$currentEffect = null;
    @Unique private UUID descriptive$currentPlayerUuid = null;

    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("HEAD"))
    private void descriptive$onTextStart(int index, Style style, int codePoint,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (index == 0) {
            AnimationRenderer.resetCharIndex();
            descriptive$currentStyle = style;

            if (AnimatedStyleMarker.shouldAnimate(style)) {
                UUID markerUuid = AnimatedStyleMarker.getPlayerForStyle(style);
                UUID contextUuid = PlayerAnimationContext.getCurrentPlayer();
                descriptive$currentPlayerUuid = markerUuid != null ? markerUuid : contextUuid;
            } else {
                descriptive$currentPlayerUuid = null;
            }
        }
    }

    @Inject(
            method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;createGlyph(FFIILnet/minecraft/network/chat/Style;FF)Lnet/minecraft/client/gui/font/TextRenderable$Styled;")
    )
    private void descriptive$beforeGlyphCreate(int index, Style style, BakedGlyph glyph,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (descriptive$isNotAnimated()) {
            descriptive$currentEffect = null;
            return;
        }
        int charIndex = AnimationRenderer.getCurrentCharIndex();
        descriptive$currentEffect = new EffectSettings(charIndex);
        AnimationRenderer.applyEffects(descriptive$currentEffect, descriptive$currentPlayerUuid);
    }

    @Redirect(
            method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;createGlyph(FFIILnet/minecraft/network/chat/Style;FF)Lnet/minecraft/client/gui/font/TextRenderable$Styled;")
    )
    private TextRenderable.Styled descriptive$redirectCreateGlyph(BakedGlyph glyph,
                                                                  float x, float y, int color, int shadowColor,
                                                                  Style style, float shadowOffset, float depth) {
        AnimationRenderer.getAndIncrementCharIndex();

        if (descriptive$isNotAnimated() || !AnimationRenderer.shouldAnimate(descriptive$currentPlayerUuid)) {
            return glyph.createGlyph(x, y, color, shadowColor, style, shadowOffset, depth);
        }

        EffectSettings effect = descriptive$currentEffect;
        float glyphX = x + effect.offsetX;
        float glyphY = y + effect.offsetY;
        int glyphColor = descriptive$applyEffectColor(color, effect);

        TextRenderable.Styled baseGlyph = glyph.createGlyph(
                glyphX, glyphY, glyphColor, shadowColor, style, shadowOffset, depth
        );
        return descriptive$wrapGlyph(baseGlyph, effect);
    }

    @ModifyArg(
            method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/glyphs/EffectGlyph;createEffect(FFFFFIIF)Lnet/minecraft/client/gui/font/TextRenderable;",
                    ordinal = 0),
            index = 5)
    private int descriptive$modifyStrikeColor(int originalColor) {
        return descriptive$applyEffectColor(originalColor, descriptive$currentEffect);
    }

    @ModifyArg(
            method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/glyphs/EffectGlyph;createEffect(FFFFFIIF)Lnet/minecraft/client/gui/font/TextRenderable;",
                    ordinal = 1),
            index = 5)
    private int descriptive$modifyUnderlineColor(int originalColor) {
        return descriptive$applyEffectColor(originalColor, descriptive$currentEffect);
    }

    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("RETURN"))
    private void descriptive$afterText(int index, Style style, int codePoint,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            descriptive$currentStyle = null;
            descriptive$currentEffect = null;
            descriptive$currentPlayerUuid = null;
            PlayerAnimationContext.clear();
        }
    }

    @Unique
    private TextRenderable.Styled descriptive$wrapGlyph(TextRenderable.Styled glyph, EffectSettings effect) {
        if (effect == null || effect.rotationRadians == 0.0f) return glyph;
        return new TransformedGlyphRenderable(
                glyph,
                effect.rotationRadians,
                effect.pivotXFactor,
                effect.pivotYFactor
        );
    }

    @Unique
    private int descriptive$applyEffectColor(int originalColor, EffectSettings effect) {
        if (effect == null || descriptive$isNotAnimated() || !AnimationRenderer.shouldAnimate(descriptive$currentPlayerUuid)) {
            return originalColor;
        }

        int r = Math.min(255, (int) (effect.r * 255));
        int g = Math.min(255, (int) (effect.g * 255));
        int b = Math.min(255, (int) (effect.b * 255));
        int alpha = originalColor & 0xFF000000;
        return alpha | (r << 16) | (g << 8) | b;
    }

    @Unique
    private boolean descriptive$isNotAnimated() {
        return !AnimatedStyleMarker.shouldAnimate(descriptive$currentStyle)
                || descriptive$currentPlayerUuid == null;
    }
}
