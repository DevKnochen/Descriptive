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
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameRenderer.class)
public class ItemFrameEntityRendererMixin {

    @Inject(
            method = "getNameTag(Lnet/minecraft/world/entity/decoration/ItemFrame;)Lnet/minecraft/network/chat/Component;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void descriptive$modifyDisplayName(ItemFrame entity,
                                               CallbackInfoReturnable<Component> cir) {
        Component text = cir.getReturnValue();
        if (text == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return;

        try {
            String str = text.getString();
            Component result = text;
            for (var player : client.level.players()) {
                String playerName = player.getName().getString();
                if (str.contains(playerName)) {
                    PlayerAnimationContext.setCurrentPlayer(player.getUUID());
                    result = TextReplacer.replaceText(result, playerName,
                            NameBuilder.buildCustomName(player.getUUID(), playerName));
                }
            }
            if (result != text) cir.setReturnValue(result);
        } catch (Exception ignored) {
            PlayerAnimationContext.clear();
        }
    }
}
