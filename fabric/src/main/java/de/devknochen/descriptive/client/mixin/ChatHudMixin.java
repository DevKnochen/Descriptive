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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;
import java.util.UUID;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Shadow @Final MinecraftClient client;

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text modifyMessage(Text message) {
        if (client.world == null || message == null) return message;
        try {
            return descriptive$processMessage(message);
        } catch (Exception e) {
            Descriptive.LOGGER.error("[ChatHud] Error processing message", e);
            PlayerAnimationContext.clear();
            return message;
        }
    }

    @Unique
    private Text descriptive$processMessage(Text text) {
        if (text == null) return null;
        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatableContent) {
            return descriptive$processTranslatableText(text, translatableContent);
        }
        return descriptive$processRegularText(text);
    }

    @Unique
    private Text descriptive$processTranslatableText(Text original, TranslatableTextContent translatable) {
        if (client.world == null) return original;

        Object[] args = translatable.getArgs();
        Object[] newArgs = new Object[args.length];
        boolean modified = false;

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Text argText) {
                String argString = argText.getString();
                Text processedArg = descriptive$resolveNameFromAnySource(argString, argText);
                if (processedArg != argText) modified = true;
                newArgs[i] = processedArg;
            } else {
                newArgs[i] = arg;
            }
        }

        if (modified) {
            MutableText result = MutableText.of(
                    new TranslatableTextContent(translatable.getKey(), translatable.getFallback(), newArgs)
            );
            result.setStyle(original.getStyle());
            for (Text sibling : original.getSiblings()) result.append(descriptive$processMessage(sibling));
            return result;
        }

        if (!original.getSiblings().isEmpty()) {
            MutableText result = MutableText.of(translatable).setStyle(original.getStyle());
            for (Text sibling : original.getSiblings()) result.append(descriptive$processMessage(sibling));
            return result;
        }

        return original;
    }

    @Unique
    private Text descriptive$resolveNameFromAnySource(String nameString, Text original) {
        // Source 1: world players
        if (client.world != null) {
            for (var player : client.world.getPlayers()) {
                String playerName = player.getName().getString();
                if (nameString.equals(playerName) || nameString.contains(playerName)) {
                    PlayerAnimationContext.setCurrentPlayer(player.getUuid());
                    return TextReplacer.replaceText(original, playerName,
                            NameBuilder.buildCustomName(player.getUuid(), playerName));
                }
            }
        }

        if (client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                String entryName = entry.getProfile().name();
                if (nameString.equals(entryName) || nameString.contains(entryName)) {
                    UUID uuid = entry.getProfile().id();
                    PlayerAnimationContext.setCurrentPlayer(uuid);
                    return TextReplacer.replaceText(original, entryName,
                            NameBuilder.buildCustomName(uuid, entryName));
                }
            }
        }

        if (client.getNetworkHandler() != null) {
            Map<UUID, CustomNameData> allEntries = CustomNameCache.getAllEntries();
            for (UUID uuid : allEntries.keySet()) {
                PlayerListEntry listEntry = client.getNetworkHandler().getPlayerListEntry(uuid);
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
    private Text descriptive$processRegularText(Text text) {
        if (client.world == null) return text;

        String textString = text.getString();
        Text result = text;

        for (var player : client.world.getPlayers()) {
            String playerName = player.getName().getString();
            if (textString.contains(playerName)) {
                PlayerAnimationContext.setCurrentPlayer(player.getUuid());
                result = TextReplacer.replaceText(result, playerName,
                        NameBuilder.buildCustomName(player.getUuid(), playerName));
            }
        }

        if (!text.getSiblings().isEmpty()) {
            MutableText mutableResult = result.copy();
            mutableResult.getSiblings().clear();
            for (Text sibling : text.getSiblings()) mutableResult.append(descriptive$processMessage(sibling));
            return mutableResult;
        }

        return result;
    }
}