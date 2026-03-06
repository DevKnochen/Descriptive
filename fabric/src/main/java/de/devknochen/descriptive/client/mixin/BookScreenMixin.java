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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookScreen.Contents.class)
public class BookScreenMixin {

    @Inject(method = "getPage", at = @At("RETURN"), cancellable = true)
    private void descriptive$modifyPage(int index, CallbackInfoReturnable<Text> cir) {
        Text page = cir.getReturnValue();
        if (page == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;

        try {
            String pageStr = page.getString();
            Text result = page;
            for (var player : client.world.getPlayers()) {
                String name = player.getName().getString();
                if (pageStr.contains(name)) {
                    PlayerAnimationContext.setCurrentPlayer(player.getUuid());
                    result = TextReplacer.replaceText(result, name,
                            NameBuilder.buildCustomName(player.getUuid(), name));
                }
            }
            if (result != page) cir.setReturnValue(result);
        } catch (Exception ignored) {
            PlayerAnimationContext.clear();
        }
    }
}