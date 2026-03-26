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

package de.devknochen.descriptive.client.animation;

import net.minecraft.network.chat.Style;

import java.util.UUID;

public class AnimatedStyleMarker {

    private static final String PREFIX = "descriptive_anim:";

    public static Style mark(Style style, UUID playerUuid) {
        return style.withInsertion(PREFIX + playerUuid);
    }

    public static boolean shouldAnimate(Style style) {
        if (style == null) return false;
        String insertion = style.getInsertion();
        return insertion != null && insertion.startsWith(PREFIX);
    }

    public static UUID getPlayerForStyle(Style style) {
        if (style == null) return null;
        String insertion = style.getInsertion();
        if (insertion == null || !insertion.startsWith(PREFIX)) return null;
        try {
            return UUID.fromString(insertion.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}