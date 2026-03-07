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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindingHandler {

    private static KeyBinding configKeyBinding;
    public static final KeyBinding.Category DESCRIPTIVE_CATEGORY = KeyBinding.Category.create(net.minecraft.util.Identifier.of("descriptive", "descriptive"));

    public static void register() {
        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.descriptive.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                DESCRIPTIVE_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKeyBinding.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new DescriptiveConfigScreen(null));
                }
            }
        });

    }
}