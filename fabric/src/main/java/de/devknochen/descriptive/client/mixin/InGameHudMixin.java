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

import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.common.util.NameBuilder;
import de.devknochen.descriptive.common.util.TextReplacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Hud.class)
public class InGameHudMixin {

    @Shadow @Final private Minecraft minecraft;

    @ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
    private Component descriptive$modifyTitle(Component title) {
        return descriptive$replaceNames(title);
    }

    @ModifyVariable(method = "setSubtitle", at = @At("HEAD"), argsOnly = true)
    private Component descriptive$modifySubtitle(Component subtitle) {
        return descriptive$replaceNames(subtitle);
    }

    @ModifyVariable(method = "setOverlayMessage", at = @At("HEAD"), argsOnly = true)
    private Component descriptive$modifyOverlay(Component message) {
        return descriptive$replaceNames(message);
    }

    @ModifyArg(
            method = "extractSelectedItemName",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"),
            index = 1
    )
    private Component descriptive$modifyHeldItemName(Component name) {
        return descriptive$replaceNames(name);
    }

    @Unique
    private Component descriptive$replaceNames(Component text) {
        if (text == null || minecraft.level == null) return text;
        Component result = text;
        try {
            for (var player : minecraft.level.players()) {
                String playerName = player.getName().getString();
                if (result.getString().contains(playerName)) {
                    PlayerAnimationContext.setCurrentPlayer(player.getUUID());
                    result = TextReplacer.replaceText(result, playerName,
                            NameBuilder.buildCustomName(player.getUUID(), playerName));
                }
            }
        } catch (Exception ignored) {
            PlayerAnimationContext.clear();
        }
        return result;
    }
}
