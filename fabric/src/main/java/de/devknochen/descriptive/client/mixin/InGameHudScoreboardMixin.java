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
import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.common.util.NameBuilder;
import de.devknochen.descriptive.common.util.TextReplacer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTeam.class)
public class InGameHudScoreboardMixin {

    @Inject(
            method = "formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void descriptive$modifyDecoratedName(@Nullable Team team, Component name,
                                                        CallbackInfoReturnable<MutableComponent> cir) {
        if (descriptive$hasMarker(name)) return;

        Component decorated = cir.getReturnValue();
        if (decorated == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return;

        try {
            String str = decorated.getString();
            for (var player : client.level.players()) {
                String playerName = player.getName().getString();
                if (str.contains(playerName)) {
                    PlayerAnimationContext.setCurrentPlayer(player.getUUID());
                    cir.setReturnValue((MutableComponent) TextReplacer.replaceText(decorated, playerName,
                            NameBuilder.buildCustomName(player.getUUID(), playerName)));
                    return;
                }
            }
        } catch (Exception ignored) {
            PlayerAnimationContext.clear();
        }
    }

    @Unique
    private static boolean descriptive$hasMarker(Component text) {
        if (text == null) return false;
        return text.visit((style, str) ->
                        AnimatedStyleMarker.shouldAnimate(style)
                                ? java.util.Optional.of(true)
                                : java.util.Optional.empty(),
                text.getStyle()
        ).isPresent();
    }
}
