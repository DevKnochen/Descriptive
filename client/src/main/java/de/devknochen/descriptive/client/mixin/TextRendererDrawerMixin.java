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
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public class TextRendererDrawerMixin {

    @Unique private Style descriptive$currentStyle = null;
    @Unique private EffectSettings descriptive$currentEffect = null;
    @Unique private UUID descriptive$currentPlayerUuid = null;

    @Inject(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("HEAD"))
    private void descriptive$onTextStart(int index, Style style, int codePoint,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (index == 0) {
            AnimationRenderer.resetCharIndex();
            descriptive$currentStyle = style;

            if (AnimatedStyleMarker.shouldAnimate(style)) {
                UUID markerUuid  = AnimatedStyleMarker.getPlayerForStyle(style);
                UUID contextUuid = PlayerAnimationContext.getCurrentPlayer();
                descriptive$currentPlayerUuid = markerUuid != null ? markerUuid : contextUuid;
            } else {
                descriptive$currentPlayerUuid = null;
            }
        }
    }

    @Inject(
            method = "accept(ILnet/minecraft/text/Style;Lnet/minecraft/client/font/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/font/BakedGlyph;create(FFIILnet/minecraft/text/Style;FF)Lnet/minecraft/client/font/TextDrawable$DrawnGlyphRect;")
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

    @ModifyArg(
            method = "accept(ILnet/minecraft/text/Style;Lnet/minecraft/client/font/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/font/BakedGlyph;create(FFIILnet/minecraft/text/Style;FF)Lnet/minecraft/client/font/TextDrawable$DrawnGlyphRect;"),
            index = 0)
    private float descriptive$modifyGlyphX(float originalX) {
        return descriptive$currentEffect != null ? originalX + descriptive$currentEffect.offsetX : originalX;
    }

    @ModifyArg(
            method = "accept(ILnet/minecraft/text/Style;Lnet/minecraft/client/font/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/font/BakedGlyph;create(FFIILnet/minecraft/text/Style;FF)Lnet/minecraft/client/font/TextDrawable$DrawnGlyphRect;"),
            index = 1)
    private float descriptive$modifyGlyphY(float originalY) {
        return descriptive$currentEffect != null ? originalY + descriptive$currentEffect.offsetY : originalY;
    }

    @ModifyArg(
            method = "accept(ILnet/minecraft/text/Style;Lnet/minecraft/client/font/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/font/BakedGlyph;create(FFIILnet/minecraft/text/Style;FF)Lnet/minecraft/client/font/TextDrawable$DrawnGlyphRect;"),
            index = 2)
    private int descriptive$modifyGlyphColor(int originalColor) {
        AnimationRenderer.getAndIncrementCharIndex();

        if (descriptive$isNotAnimated() || !AnimationRenderer.shouldAnimate(descriptive$currentPlayerUuid)) {
            return originalColor;
        }

        if (descriptive$currentEffect != null) {
            int r     = Math.min(255, (int)(descriptive$currentEffect.r * 255));
            int g     = Math.min(255, (int)(descriptive$currentEffect.g * 255));
            int b     = Math.min(255, (int)(descriptive$currentEffect.b * 255));
            int alpha = originalColor & 0xFF000000;
            return alpha | (r << 16) | (g << 8) | b;
        }

        return originalColor;
    }

    @ModifyArg(
            method = "accept(ILnet/minecraft/text/Style;Lnet/minecraft/client/font/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/font/EffectGlyph;create(FFFFFIIF)Lnet/minecraft/client/font/TextDrawable;",
                    ordinal = 0),
            index = 5)
    private int descriptive$modifyStrikeColor(int originalColor) {
        return descriptive$applyEffectColor(originalColor);
    }

    @ModifyArg(
            method = "accept(ILnet/minecraft/text/Style;Lnet/minecraft/client/font/BakedGlyph;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/font/EffectGlyph;create(FFFFFIIF)Lnet/minecraft/client/font/TextDrawable;",
                    ordinal = 1),
            index = 5)
    private int descriptive$modifyUnderlineColor(int originalColor) {
        return descriptive$applyEffectColor(originalColor);
    }

    @Unique
    private int descriptive$applyEffectColor(int originalColor) {
        if (descriptive$isNotAnimated() || !AnimationRenderer.shouldAnimate(descriptive$currentPlayerUuid)) {
            return originalColor;
        }
        if (descriptive$currentEffect != null) {
            int r     = Math.min(255, (int)(descriptive$currentEffect.r * 255));
            int g     = Math.min(255, (int)(descriptive$currentEffect.g * 255));
            int b     = Math.min(255, (int)(descriptive$currentEffect.b * 255));
            int alpha = originalColor & 0xFF000000;
            return alpha | (r << 16) | (g << 8) | b;
        }
        return originalColor;
    }

    @Inject(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("RETURN"))
    private void descriptive$afterText(int index, Style style, int codePoint,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            descriptive$currentStyle      = null;
            descriptive$currentEffect     = null;
            descriptive$currentPlayerUuid = null;
            PlayerAnimationContext.clear();
        }
    }

    @Unique
    private boolean descriptive$isNotAnimated() {
        return !AnimatedStyleMarker.shouldAnimate(descriptive$currentStyle)
                || descriptive$currentPlayerUuid == null;
    }
}