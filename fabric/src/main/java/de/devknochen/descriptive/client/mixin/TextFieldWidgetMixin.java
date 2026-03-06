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
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(TextFieldWidget.class)
public class TextFieldWidgetMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/client/font/TextRenderer;IIIILnet/minecraft/client/gui/widget/TextFieldWidget;Lnet/minecraft/text/Text;)V",
            at = @At("RETURN")
    )
    private void descriptive$addFormatter(CallbackInfo ci) {
        TextFieldWidget self = (TextFieldWidget) (Object) this;
        self.addFormatter((string, firstCharIndex) -> descriptive$format(string));
    }

    @Unique
    private static OrderedText descriptive$format(String string) {
        if (string == null || string.isEmpty()) return null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return null;

        String bestName = null;
        UUID bestUuid = null;
        int bestIdx = -1;

        for (var player : client.world.getPlayers()) {
            String playerName = player.getName().getString();
            if (playerName.isEmpty()) continue;
            int idx = descriptive$findExactMatch(string, playerName);
            if (idx == -1) continue;
            if (bestName == null
                    || playerName.length() > bestName.length()
                    || (playerName.length() == bestName.length() && idx < bestIdx)) {
                bestName = playerName;
                bestUuid = player.getUuid();
                bestIdx = idx;
            }
        }

        if (bestName == null) return null;

        PlayerAnimationContext.setCurrentPlayer(bestUuid);
        Text customName = NameBuilder.buildCustomName(bestUuid, bestName);

        if (customName.getString().equals(bestName)
                && customName.getStyle().equals(Style.EMPTY)) return null;

        if (customName.getStyle().isBold()) {
            Style strippedStyle = customName.getStyle().withBold(false);
            customName = Text.literal(customName.getString()).setStyle(strippedStyle);
        }

        String before = string.substring(0, bestIdx);
        String after = string.substring(bestIdx + bestName.length());

        return Text.literal(before)
                .append(customName)
                .append(Text.literal(after))
                .asOrderedText();
    }

    @Unique
    private static int descriptive$findExactMatch(String text, String playerName) {
        int idx = 0;
        while (idx <= text.length() - playerName.length()) {
            int found = text.indexOf(playerName, idx);
            if (found == -1) return -1;
            boolean validBefore = found == 0
                    || !Character.isLetterOrDigit(text.charAt(found - 1));
            int afterIdx = found + playerName.length();
            boolean validAfter = afterIdx == text.length()
                    || !Character.isLetterOrDigit(text.charAt(afterIdx));
            if (validBefore && validAfter) return found;
            idx = found + 1;
        }
        return -1;
    }
}