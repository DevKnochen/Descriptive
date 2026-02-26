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
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.common.util.NameBuilder;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void afterUpdateRenderState(PlayerLikeEntity player,
                                        PlayerEntityRenderState state,
                                        float tickDelta,
                                        CallbackInfo ci) {
        if (state.displayName == null) return;
        try {
            UUID playerUuid = player.getUuid();
            String playerName = player.getName().getString();
            if (!CustomNameCache.has(playerUuid)) return;
            PlayerAnimationContext.setCurrentPlayer(playerUuid);
            state.displayName = NameBuilder.buildCustomName(playerUuid, playerName);
        } catch (Exception ignored) {
            PlayerAnimationContext.clear();
        }
    }
}