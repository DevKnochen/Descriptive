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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.WrittenBookContent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(WrittenBookContent.class)
public class WrittenBookContentMixin {

    @Shadow @Final private String author;
    @Shadow @Final private int generation;

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void descriptive$modifyAuthorTooltip(Item.TooltipContext context,
                                                 Consumer<Component> textConsumer,
                                                 TooltipFlag type,
                                                 DataComponentGetter components,
                                                 CallbackInfo ci) {
        if (StringUtil.isBlank(author)) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return;

        for (var player : client.level.players()) {
            if (author.equals(player.getName().getString())) {
                PlayerAnimationContext.setCurrentPlayer(player.getUUID());
                Component customName = NameBuilder.buildCustomName(player.getUUID(), author);
                textConsumer.accept(Component.translatable("book.byAuthor", customName)
                        .withStyle(ChatFormatting.GRAY));
                textConsumer.accept(Component.translatable("book.generation." + generation)
                        .withStyle(ChatFormatting.GRAY));
                ci.cancel();
                return;
            }
        }
    }
}
