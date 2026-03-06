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
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignText.class)
public class SignBlockEntityRendererMixin {

    @Inject(method = "getMessages", at = @At("RETURN"), cancellable = true)
    private void descriptive$modifySignLines(boolean filtered, CallbackInfoReturnable<Text[]> cir) {
        Text[] original = cir.getReturnValue();
        if (original == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;

        Text[] result = original.clone();
        boolean modified = false;
        try {
            for (int i = 0; i < result.length; i++) {
                Text line = result[i];
                if (line == null) continue;
                String lineStr = line.getString();
                for (var player : client.world.getPlayers()) {
                    String name = player.getName().getString();
                    if (lineStr.contains(name)) {
                        PlayerAnimationContext.setCurrentPlayer(player.getUuid());
                        result[i] = TextReplacer.replaceText(line, name,
                                NameBuilder.buildCustomName(player.getUuid(), name));
                        modified = true;
                    }
                }
            }
        } catch (Exception ignored) {
            PlayerAnimationContext.clear();
            return;
        }
        if (modified) cir.setReturnValue(result);
    }
}