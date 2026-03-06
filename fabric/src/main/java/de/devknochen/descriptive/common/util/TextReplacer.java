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
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;

public class TextReplacer {

    public static Text replaceText(Text original, String target, Text replacement) {
        if (original == null || target == null || target.isEmpty()) return original;

        String fullString = original.getString();
        if (!fullString.contains(target)) return original;

        TextContent content = original.getContent();

        if (content instanceof PlainTextContent plainContent) {
            String text = plainContent.string();
            if (text.contains(target)) {
                MutableText result = replaceInString(text, target, replacement, original.getStyle());
                for (Text sibling : original.getSiblings()) {
                    result.append(replaceText(sibling, target, replacement));
                }
                return result;
            }
        }

        if (!original.getSiblings().isEmpty()) {
            MutableText result = MutableText.of(content).setStyle(original.getStyle());
            for (Text sibling : original.getSiblings()) {
                result.append(replaceText(sibling, target, replacement));
            }
            return result;
        }

        if (original.getString().contains(target)) {
            return replaceInString(original.getString(), target, replacement, original.getStyle());
        }

        MutableText result = MutableText.of(content).setStyle(original.getStyle());
        for (Text sibling : original.getSiblings()) result.append(sibling);
        return result;
    }

    private static MutableText replaceInString(String text, String target, Text replacement, Style surroundingStyle) {
        MutableText result = Text.empty();
        int lastIndex = 0;
        int index;

        while ((index = text.indexOf(target, lastIndex)) != -1) {
            if (index > lastIndex) {
                result.append(Text.literal(text.substring(lastIndex, index)).setStyle(Style.EMPTY));
            }

            MutableText replacementCopy = replacement.copy();
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

        if (lastIndex < text.length()) {
            result.append(Text.literal(text.substring(lastIndex)).setStyle(Style.EMPTY));
        }

        return result;
    }
}