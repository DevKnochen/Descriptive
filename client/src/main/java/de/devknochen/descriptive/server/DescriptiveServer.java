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

package de.devknochen.descriptive.server;

import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.server.network.ServerNetworkHandler;
import net.fabricmc.api.DedicatedServerModInitializer;

public class DescriptiveServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        Descriptive.LOGGER.info("Initializing {} (Dedicated Server)", Descriptive.MOD_NAME);
        ServerNetworkHandler.initialize();
        Descriptive.LOGGER.info("{} Server initialized", Descriptive.MOD_NAME);
    }
}