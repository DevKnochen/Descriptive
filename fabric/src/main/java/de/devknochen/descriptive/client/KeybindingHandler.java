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

import de.devknochen.descriptive.client.gui.DescriptiveConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeybindingHandler {

    private static KeyMapping configKeyBinding;
    private static final KeyMapping.Category DESCRIPTIVE_CATEGORY = KeyMapping.Category.register(
            Identifier.parse("descriptive:key_category")
    );

    public static void register() {
        configKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.descriptive.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                DESCRIPTIVE_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKeyBinding.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new DescriptiveConfigScreen(null));
                }
            }
        });
    }
}
