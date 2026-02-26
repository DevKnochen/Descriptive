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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Inject(
            method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void descriptive$interceptStringDraw(TextRenderer textRenderer, String text,
                                                 int x, int y, int color, boolean shadow,
                                                 CallbackInfo ci) {
        if (text == null || text.isEmpty()) return;
        Text styled = descriptive$buildStyledText(text, true);
        if (styled == null) return;

        ci.cancel();
        DrawContext self = (DrawContext) (Object) this;
        self.drawText(textRenderer, styled.asOrderedText(), x, y, color, shadow);
    }

    @ModifyVariable(
            method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private Text descriptive$interceptTextDraw(Text text) {
        if (text == null) return null;
        String str = text.getString();
        if (str.isEmpty()) return text;
        Text replacement = descriptive$buildStyledText(str, false);
        return replacement != null ? replacement : text;
    }

    @Unique
    private static Text descriptive$buildStyledText(String string, boolean stripBold) {
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

        if (stripBold && customName.getStyle().isBold()) {
            Style strippedStyle = customName.getStyle().withBold(false);
            customName = Text.literal(customName.getString()).setStyle(strippedStyle);
        }

        String before = string.substring(0, bestIdx);
        String after = string.substring(bestIdx + bestName.length());

        return Text.literal(before)
                .append(customName)
                .append(Text.literal(after));
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