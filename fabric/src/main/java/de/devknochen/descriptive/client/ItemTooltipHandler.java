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

package de.devknochen.descriptive.client;

import de.devknochen.descriptive.client.animation.PlayerAnimationContext;
import de.devknochen.descriptive.common.util.NameBuilder;
import de.devknochen.descriptive.common.util.TextReplacer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.ListIterator;

public class ItemTooltipHandler {

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null) return;

            List<AbstractClientPlayerEntity> players = client.world.getPlayers();
            if (players.isEmpty()) return;

            ListIterator<Text> it = lines.listIterator();
            while (it.hasNext()) {
                Text line = it.next();
                if (line == null) continue;
                String lineStr = line.getString();
                Text result = line;
                for (AbstractClientPlayerEntity player : players) {
                    String name = player.getName().getString();
                    if (lineStr.contains(name)) {
                        PlayerAnimationContext.setCurrentPlayer(player.getUuid());
                        result = TextReplacer.replaceText(result, name,
                                NameBuilder.buildCustomName(player.getUuid(), name));
                    }
                }
                if (result != line) it.set(result);
            }
        });
    }
}