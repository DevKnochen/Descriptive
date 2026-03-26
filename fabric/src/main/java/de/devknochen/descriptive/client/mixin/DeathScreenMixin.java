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

import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.common.util.NameBuilder;
import de.devknochen.descriptive.common.util.TextReplacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Mutable
    @Final private @Nullable Component message;

    @Inject(
            method = "<init>(Lnet/minecraft/network/chat/Component;ZLnet/minecraft/client/player/LocalPlayer;)V",
            at = @At("RETURN")
    )
    private void modifyDeathMessage(@Nullable Component message, boolean isHardcore,
                                    LocalPlayer decedent, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null || this.message == null) return;

        Descriptive.LOGGER.info("[DeathScreen] Processing death message: {}", this.message.getString());

        Component result = this.message;
        String messageString = result.getString();

        try {
            for (var player : client.level.players()) {
                String playerName = player.getName().getString();

                if (messageString.contains(playerName)) {
                    Descriptive.LOGGER.info("[DeathScreen] Found player name '{}' in death message", playerName);
                    PlayerAnimationContext.setCurrentPlayer(player.getUUID());
                    result = TextReplacer.replaceText(result, playerName,
                            NameBuilder.buildCustomName(player.getUUID(), playerName));
                    Descriptive.LOGGER.info("[DeathScreen] Replaced '{}'", playerName);
                }
            }
        } catch (Exception e) {
            Descriptive.LOGGER.error("[DeathScreen] Error processing death message", e);
            PlayerAnimationContext.clear();
        }

        this.message = result;
    }
}
