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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.common.util.NameBuilder;
import net.minecraft.client.gui.screen.ingame.BookSigningScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(BookSigningScreen.class)
public class BookSigningScreenMixin {

    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Ljava/util/List;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;"
            )
    )
    private MutableText descriptive$modifyBylineText(MutableText original,
                                                     @Local(argsOnly = true) PlayerEntity player) {
        if (original == null || player == null) return original;
        try {
            PlayerAnimationContext.setCurrentPlayer(player.getUuid());
            Text customName = NameBuilder.buildCustomName(player.getUuid(), player.getName().getString());
            return Text.translatable("book.byAuthor", customName);
        } catch (Exception ignored) {
            PlayerAnimationContext.clear();
            return original;
        }
    }
}