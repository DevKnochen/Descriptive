package de.devknochen.descriptive.config;

import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.client.network.ClientNetworkHandler;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DescriptiveClientConfig {

    private final Path configPath;

    private int color                        = 0xFFFFFF;
    private boolean bold                     = false;
    private boolean italic                   = false;
    private boolean underlined               = false;
    private boolean strikethrough            = false;
    private float animationSpeed             = 1.0f;
    private boolean relayEnabled             = false;
    private List<String> animationTypes      = new ArrayList<>();
    private List<Integer> gradientColors     = new ArrayList<>(List.of(0xFF0000, 0x0000FF));
    private Set<UUID> disabledPlayers        = new HashSet<>();

    public DescriptiveClientConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("descriptive-client.toml");
    }

    public void load() {
        if (!Files.exists(configPath)) { save(); return; }
        try {
            for (String raw : Files.readAllLines(configPath)) {
                String line = stripComment(raw).trim();
                if (!line.contains("=")) continue;

                String key   = line.substring(0, line.indexOf('=')).trim();
                String value = line.substring(line.indexOf('=') + 1).trim();

                switch (key) {
                    case "color"          -> color         = parseHex(value);
                    case "bold"           -> bold          = Boolean.parseBoolean(value);
                    case "italic"         -> italic        = Boolean.parseBoolean(value);
                    case "underlined"     -> underlined    = Boolean.parseBoolean(value);
                    case "strikethrough"  -> strikethrough = Boolean.parseBoolean(value);
                    case "animation_speed" -> animationSpeed = parseFloat(value, 1.0f);
                    case "relay_enabled"  -> relayEnabled  = Boolean.parseBoolean(value);
                    case "animation_types" -> animationTypes = parseStringList(value);
                    case "gradient_colors" -> {
                        List<Integer> parsed = parseHexList(value);
                        gradientColors = parsed.size() >= 2 ? parsed : new ArrayList<>(List.of(0xFF0000, 0x0000FF));
                    }
                    case "disabled_players" -> disabledPlayers = parseUuidList(value);
                }
            }
        } catch (Exception e) {
            Descriptive.LOGGER.error("[Descriptive] Failed to load client config", e);
            save();
        }
    }

    public void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Name color as a hex value\n");
        sb.append("color = \"").append(String.format("#%06X", color & 0xFFFFFF)).append("\"\n\n");
        sb.append("# Gradient colors as hex values\n");
        sb.append("gradient_colors = ").append(toHexList(gradientColors)).append("\n\n");
        sb.append("# Text formatting\n");
        sb.append("bold          = ").append(bold).append("\n");
        sb.append("italic        = ").append(italic).append("\n");
        sb.append("underlined    = ").append(underlined).append("\n");
        sb.append("strikethrough = ").append(strikethrough).append("\n\n");
        sb.append("# Animation speed multiplier (0.1 - 5.0)\n");
        sb.append("animation_speed = ").append(animationSpeed).append("\n\n");
        sb.append("# Active animation types\n");
        sb.append("animation_types = ").append(toStringList(animationTypes)).append("\n\n");
        sb.append("# UUIDs of players whose Descriptive rendering is disabled\n");
        sb.append("disabled_players = ").append(toUuidList(disabledPlayers)).append("\n\n");
        sb.append("# Enable or disable relay function\n");
        sb.append("relay_enabled = ").append(relayEnabled).append("\n\n");
        sb.append("# Benefits:\n");
        sb.append("# Custom names will work on any server, vanilla or modded.\n");
        sb.append("# Connection opens on server join and closes when you leave. No persistent background process.\n");
        sb.append("# You can enable or disable this function at any time.\n");
        sb.append("# The relay connection code is open source.\n");
        sb.append("# \n");
        sb.append("# Important to know:\n");
        sb.append("# Only your Minecraft UUID, styling data and current server address are transmitted to a external websocket server called relay.\n");
        sb.append("# Transmitted data is never stored. It exists only in the relay's RAM and is cleared on disconnect.\n");
        sb.append("# The relay is community-run on a best-effort basis. There is no guaranteed uptime.\n");
        sb.append("\n");

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, sb.toString());
            ClientNetworkHandler.updateCustomName();
        } catch (IOException e) {
            Descriptive.LOGGER.error("[Descriptive] Failed to save client config", e);
        }
    }

    private static String stripComment(String line) {
        int i = line.indexOf('#');
        if (i == -1) return line;
        boolean inQuote = false;
        for (int j = 0; j < line.length(); j++) {
            char c = line.charAt(j);
            if (c == '"') inQuote = !inQuote;
            if (c == '#' && !inQuote) return line.substring(0, j);
        }
        return line;
    }

    private static int parseHex(String value) {
        try { return Integer.parseInt(value.replace("\"", "").replace("#", "").trim(), 16); }
        catch (NumberFormatException e) { return 0xFFFFFF; }
    }

    private static float parseFloat(String value, float fallback) {
        try { return Float.parseFloat(value.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static List<String> parseStringList(String value) {
        List<String> result = new ArrayList<>();
        String inner = value.trim();
        if (!inner.startsWith("[") || !inner.endsWith("]")) return result;
        inner = inner.substring(1, inner.length() - 1).trim();
        if (inner.isEmpty()) return result;
        for (String part : inner.split(",")) {
            String s = part.trim().replace("\"", "");
            if (!s.isEmpty()) result.add(s);
        }
        return result;
    }

    private static List<Integer> parseHexList(String value) {
        List<Integer> result = new ArrayList<>();
        for (String part : parseStringList(value)) {
            try { result.add(Integer.parseInt(part.replace("#", "").trim(), 16)); }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private static Set<UUID> parseUuidList(String value) {
        Set<UUID> result = new HashSet<>();
        for (String part : parseStringList(value)) {
            try { result.add(UUID.fromString(part.trim())); }
            catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    private static String toStringList(List<String> list) {
        if (list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    private static String toHexList(List<Integer> list) {
        if (list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(String.format("#%06X", list.get(i) & 0xFFFFFF)).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    private static String toUuidList(Set<UUID> set) {
        if (set.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        List<UUID> list = new ArrayList<>(set);
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    public void setColorInternal(int color)                { this.color = color & 0xFFFFFF; }
    public void setBoldInternal(boolean bold)              { this.bold = bold; }
    public void setItalicInternal(boolean italic)          { this.italic = italic; }
    public void setUnderlinedInternal(boolean underlined)  { this.underlined = underlined; }
    public void setStrikethroughInternal(boolean st)       { this.strikethrough = st; }
    public void setAnimationTypesInternal(List<String> t)  { this.animationTypes = new ArrayList<>(t); }
    public void setAnimationSpeedInternal(float speed)     { this.animationSpeed = Math.max(0.1f, Math.min(5.0f, speed)); }
    public void setGradientColorsInternal(List<Integer> c) { this.gradientColors = new ArrayList<>(c); }
    public void setGradientColors(List<Integer> c)         { this.gradientColors = new ArrayList<>(c); save(); }

    public int getColor()                    { return color; }
    public boolean isBold()                  { return bold; }
    public boolean isItalic()                { return italic; }
    public boolean isUnderlined()            { return underlined; }
    public boolean isStrikethrough()         { return strikethrough; }
    public float getAnimationSpeed()         { return animationSpeed; }
    public boolean isAnimationEnabled()      { return !animationTypes.isEmpty(); }
    public List<String> getAnimationTypes()  { return new ArrayList<>(animationTypes); }
    public List<Integer> getGradientColors() { return new ArrayList<>(gradientColors); }
    public boolean isRelayEnabled()          { return relayEnabled; }
    public void setRelayEnabled(boolean v)   { this.relayEnabled = v; }

    public boolean isPlayerEnabled(UUID uuid)           { return !disabledPlayers.contains(uuid); }
    public Set<UUID> getDisabledPlayers()               { return new HashSet<>(disabledPlayers); }
    public void setDisabledPlayers(Set<UUID> disabled)  { this.disabledPlayers = new HashSet<>(disabled); }
}