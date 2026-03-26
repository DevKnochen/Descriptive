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
import de.devknochen.descriptive.client.network.CustomNameCache;
import de.devknochen.descriptive.common.network.packet.CustomNameData;
import de.devknochen.descriptive.common.util.NameBuilder;
import de.devknochen.descriptive.common.util.TextReplacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;
import java.util.UUID;

@Mixin(ChatComponent.class)
public class ChatHudMixin {

    @Shadow @Final Minecraft minecraft;

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component modifyMessage(Component message) {
        if (minecraft.level == null || message == null) return message;
        try {
            return descriptive$processMessage(message);
        } catch (Exception e) {
            Descriptive.LOGGER.error("[ChatHud] Error processing message", e);
            PlayerAnimationContext.clear();
            return message;
        }
    }

    @Unique
    private Component descriptive$processMessage(Component text) {
        if (text == null) return null;
        ComponentContents content = text.getContents();
        if (content instanceof TranslatableContents translatableContent) {
            return descriptive$processTranslatableText(text, translatableContent);
        }
        return descriptive$processRegularText(text);
    }

    @Unique
    private Component descriptive$processTranslatableText(Component original, TranslatableContents translatable) {
        if (minecraft.level == null) return original;

        Object[] args = translatable.getArgs();
        Object[] newArgs = new Object[args.length];
        boolean modified = false;

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Component argText) {
                String argString = argText.getString();
                Component processedArg = descriptive$resolveNameFromAnySource(argString, argText);
                if (processedArg != argText) modified = true;
                newArgs[i] = processedArg;
            } else {
                newArgs[i] = arg;
            }
        }

        if (modified) {
            MutableComponent result = MutableComponent.create(
                    new TranslatableContents(translatable.getKey(), translatable.getFallback(), newArgs)
            );
            result.setStyle(original.getStyle());
            for (Component sibling : original.getSiblings()) result.append(descriptive$processMessage(sibling));
            return result;
        }

        if (!original.getSiblings().isEmpty()) {
            MutableComponent result = MutableComponent.create(translatable).setStyle(original.getStyle());
            for (Component sibling : original.getSiblings()) result.append(descriptive$processMessage(sibling));
            return result;
        }

        return original;
    }

    @Unique
    private Component descriptive$resolveNameFromAnySource(String nameString, Component original) {
        if (minecraft.level != null) {
            for (var player : minecraft.level.players()) {
                String playerName = player.getName().getString();
                if (nameString.equals(playerName) || nameString.contains(playerName)) {
                    PlayerAnimationContext.setCurrentPlayer(player.getUUID());
                    return TextReplacer.replaceText(original, playerName,
                            NameBuilder.buildCustomName(player.getUUID(), playerName));
                }
            }
        }

        if (minecraft.getConnection() != null) {
            for (PlayerInfo entry : minecraft.getConnection().getOnlinePlayers()) {
                String entryName = entry.getProfile().name();
                if (nameString.equals(entryName) || nameString.contains(entryName)) {
                    UUID uuid = entry.getProfile().id();
                    PlayerAnimationContext.setCurrentPlayer(uuid);
                    return TextReplacer.replaceText(original, entryName,
                            NameBuilder.buildCustomName(uuid, entryName));
                }
            }
        }

        if (minecraft.getConnection() != null) {
            Map<UUID, CustomNameData> allEntries = CustomNameCache.getAllEntries();
            for (UUID uuid : allEntries.keySet()) {
                PlayerInfo listEntry = minecraft.getConnection().getPlayerInfo(uuid);
                if (listEntry != null) {
                    String entryName = listEntry.getProfile().name();
                    if (nameString.equals(entryName) || nameString.contains(entryName)) {
                        PlayerAnimationContext.setCurrentPlayer(uuid);
                        return TextReplacer.replaceText(original, entryName,
                                NameBuilder.buildCustomName(uuid, entryName));
                    }
                }
            }
        }

        return original;
    }

    @Unique
    private Component descriptive$processRegularText(Component text) {
        if (minecraft.level == null) return text;

        String textString = text.getString();
        Component result = text;

        for (var player : minecraft.level.players()) {
            String playerName = player.getName().getString();
            if (textString.contains(playerName)) {
                PlayerAnimationContext.setCurrentPlayer(player.getUUID());
                result = TextReplacer.replaceText(result, playerName,
                        NameBuilder.buildCustomName(player.getUUID(), playerName));
            }
        }

        if (!text.getSiblings().isEmpty()) {
            MutableComponent mutableResult = result.copy();
            mutableResult.getSiblings().clear();
            for (Component sibling : text.getSiblings()) mutableResult.append(descriptive$processMessage(sibling));
            return mutableResult;
        }

        return result;
    }
}
