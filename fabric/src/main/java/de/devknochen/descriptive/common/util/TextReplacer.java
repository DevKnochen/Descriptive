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

package de.devknochen.descriptive.common.util;

import de.devknochen.descriptive.client.animation.AnimatedStyleMarker;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;

public class TextReplacer {

    public static Component replaceText(Component original, String target, Component replacement) {
        if (original == null || target == null || target.isEmpty()) return original;
        if (!original.getString().contains(target)) return original;

        ComponentContents content = original.getContents();

        if (content instanceof PlainTextContents plainContent) {
            String text = plainContent.text();
            if (text.contains(target)) {
                MutableComponent result = replaceInString(text, target, replacement, original.getStyle());
                for (Component sibling : original.getSiblings())
                    result.append(replaceText(sibling, target, replacement));
                return result;
            }
        }

        if (original.getSiblings().isEmpty()) return original;

        MutableComponent result = MutableComponent.create(content).setStyle(original.getStyle());
        for (Component sibling : original.getSiblings())
            result.append(replaceText(sibling, target, replacement));
        return result;
    }

    private static MutableComponent replaceInString(String text, String target, Component replacement, Style surroundingStyle) {
        MutableComponent result = Component.empty();
        int lastIndex = 0, index;

        while ((index = text.indexOf(target, lastIndex)) != -1) {
            if (index > lastIndex)
                result.append(Component.literal(text.substring(lastIndex, index)).setStyle(Style.EMPTY));

            MutableComponent replacementCopy = replacement.copy();
            Style replacementStyle = replacementCopy.getStyle();
            Style mergedStyle = replacementStyle;

            if (surroundingStyle.getClickEvent() != null)
                mergedStyle = mergedStyle.withClickEvent(surroundingStyle.getClickEvent());
            if (surroundingStyle.getHoverEvent() != null)
                mergedStyle = mergedStyle.withHoverEvent(surroundingStyle.getHoverEvent());

            if (AnimatedStyleMarker.shouldAnimate(replacementStyle)) {
                var playerUuid = AnimatedStyleMarker.getPlayerForStyle(replacementStyle);
                if (playerUuid != null) mergedStyle = AnimatedStyleMarker.mark(mergedStyle, playerUuid);
            }

            replacementCopy.setStyle(mergedStyle);
            result.append(replacementCopy);
            lastIndex = index + target.length();
        }

        if (lastIndex < text.length())
            result.append(Component.literal(text.substring(lastIndex)).setStyle(Style.EMPTY));

        return result;
    }
}