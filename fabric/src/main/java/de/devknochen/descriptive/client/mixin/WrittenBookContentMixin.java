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
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(WrittenBookContentComponent.class)
public class WrittenBookContentMixin {

    @Shadow @Final private String author;
    @Shadow @Final private int generation;

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void descriptive$modifyAuthorTooltip(Item.TooltipContext context,
                                                 Consumer<Text> textConsumer,
                                                 TooltipType type,
                                                 ComponentsAccess components,
                                                 CallbackInfo ci) {
        if (StringHelper.isBlank(author)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;

        for (var player : client.world.getPlayers()) {
            if (author.equals(player.getName().getString())) {
                PlayerAnimationContext.setCurrentPlayer(player.getUuid());
                Text customName = NameBuilder.buildCustomName(player.getUuid(), author);
                textConsumer.accept(Text.translatable("book.byAuthor", customName)
                        .formatted(Formatting.GRAY));
                textConsumer.accept(Text.translatable("book.generation." + generation)
                        .formatted(Formatting.GRAY));
                ci.cancel();
                return;
            }
        }
    }
}