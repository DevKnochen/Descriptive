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

package de.devknochen.descriptiveserver.common;

import java.io.*;
import java.nio.file.*;

public class ServerConfig {

    private static final String DEFAULT_CONFIG =
            "{\n" +
                    "  // Master switch. If false, the server will reject all Descriptive packets\n" +
                    "  // and notify clients that custom name display is disabled on this server.\n" +
                    "  \"enabled\": true\n" +
                    "}\n";

    private boolean enabled = true;
    private final Path configPath;

    public ServerConfig(Path configPath) {
        this.configPath = configPath;
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            log("Config created at " + configPath);
            return;
        }

        try {
            String content = Files.readString(configPath);
            String stripped = content.replaceAll("//[^\n]*", "").trim();
            if (stripped.contains("\"enabled\"")) {
                if (stripped.contains("\"enabled\": false") || stripped.contains("\"enabled\":false")) {
                    enabled = false;
                } else {
                    enabled = true;
                }
            }
            log("Config loaded - enabled=" + enabled);
        } catch (Exception e) {
            log("Failed to load config, using defaults: " + e.getMessage());
            enabled = true;
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, DEFAULT_CONFIG);
        } catch (IOException e) {
            log("Failed to save config: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void log(String msg) {
        System.out.println("[Descriptive/Config] " + msg);
    }
}