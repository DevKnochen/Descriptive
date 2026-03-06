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

package de.devknochen.descriptive.client.network;

import de.devknochen.descriptive.common.network.packet.CustomNameData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomNameCache {

    private static final Map<UUID, CustomNameData> cache = new ConcurrentHashMap<>();

    public static void put(CustomNameData data) {
        cache.put(data.playerUuid(), data);
    }

    public static CustomNameData get(UUID playerUuid) {
        return cache.get(playerUuid);
    }

    public static boolean has(UUID playerUuid) {
        return cache.containsKey(playerUuid);
    }

    public static void clear() {
        cache.clear();
    }

    public static int size() {
        return cache.size();
    }

    public static Map<UUID, CustomNameData> getAllEntries() {
        return new ConcurrentHashMap<>(cache);
    }
}