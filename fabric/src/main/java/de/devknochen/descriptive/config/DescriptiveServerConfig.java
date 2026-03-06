package de.devknochen.descriptive.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DescriptiveServerConfig {

    private static final String DEFAULT_CONFIG =
                    "# Master switch. If false, the server will reject all Descriptive packets\n" +
                    "# and notify clients that custom name display is disabled on this server.\n" +
                    "enabled = true\n";

    private final Path configPath;
    private boolean enabled = true;

    public DescriptiveServerConfig(Path configPath) {
        this.configPath = configPath;
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }
        try {
            for (String raw : Files.readAllLines(configPath)) {
                String line = raw.contains("#") ? raw.substring(0, raw.indexOf('#')) : raw;
                line = line.trim();
                if (line.startsWith("enabled")) {
                    String value = line.substring(line.indexOf('=') + 1).trim();
                    enabled = Boolean.parseBoolean(value);
                }
            }
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

    public boolean isEnabled() { return enabled; }

    private void log(String msg) {
        System.out.println("[Descriptive/Config] " + msg);
    }
}