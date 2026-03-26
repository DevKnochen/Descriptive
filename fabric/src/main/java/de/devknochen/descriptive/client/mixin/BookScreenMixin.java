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
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookViewScreen.BookAccess.class)
public class BookScreenMixin {

    @Inject(method = "getPage", at = @At("RETURN"), cancellable = true)
    private void descriptive$modifyPage(int index, CallbackInfoReturnable<Component> cir) {
        Component page = cir.getReturnValue();
        if (page == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return;

        try {
            String pageStr = page.getString();
            Component result = page;
            for (var player : client.level.players()) {
                String name = player.getName().getString();
                if (pageStr.contains(name)) {
                    PlayerAnimationContext.setCurrentPlayer(player.getUUID());
                    result = TextReplacer.replaceText(result, name,
                            NameBuilder.buildCustomName(player.getUUID(), name));
                }
            }
            if (result != page) cir.setReturnValue(result);
        } catch (Exception ignored) {
            PlayerAnimationContext.clear();
        }
    }
}
