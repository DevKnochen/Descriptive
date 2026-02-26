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

package de.devknochen.descriptive.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.devknochen.descriptive.Descriptive;
import de.devknochen.descriptive.client.network.ClientNetworkHandler;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.List;

public class DescriptiveConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "descriptive.json";

    private final Path configPath;

    private int color = 0xFFFFFF;
    private boolean bold = false;
    private boolean italic = false;
    private boolean underlined = false;
    private boolean strikethrough = false;
    private List<Integer> gradientColors = new ArrayList<>(List.of(0xFF0000, 0x0000FF));
    private List<String> animationTypes = new ArrayList<>();
    private float animationSpeed = 1.0f;

    // ── Per-subject rendering toggles ──────────────────────────────────
    // "My name" toggles
    // "Other players" toggles
    // Per-player disable set — UUIDs whose Descriptive rendering is turned off
    private Set<UUID> disabledPlayers = new HashSet<>();

    private boolean relayEnabled = false;

    public DescriptiveConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try {
            String json = Files.readString(configPath);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);

            if (obj.has("color")) {
                this.color = Integer.parseInt(obj.get("color").getAsString().replace("#", ""), 16);
            }
            if (obj.has("bold"))          this.bold          = obj.get("bold").getAsBoolean();
            if (obj.has("italic"))        this.italic        = obj.get("italic").getAsBoolean();
            if (obj.has("underlined"))    this.underlined    = obj.get("underlined").getAsBoolean();
            if (obj.has("strikethrough")) this.strikethrough = obj.get("strikethrough").getAsBoolean();

            if (obj.has("gradientColors")) {
                gradientColors = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("gradientColors")) {
                    try { gradientColors.add(Integer.parseInt(e.getAsString().replace("#", ""), 16)); }
                    catch (NumberFormatException ignored) {}
                }
                if (gradientColors.size() < 2) gradientColors = new ArrayList<>(List.of(0xFF0000, 0x0000FF));
            }

            if (obj.has("animationTypes")) {
                animationTypes = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("animationTypes"))
                    animationTypes.add(e.getAsString());
            }
            if (obj.has("animationSpeed")) this.animationSpeed = obj.get("animationSpeed").getAsFloat();


            if (obj.has("relayEnabled")) this.relayEnabled = obj.get("relayEnabled").getAsBoolean();

            if (obj.has("disabledPlayers")) {
                disabledPlayers = new HashSet<>();
                for (JsonElement e : obj.getAsJsonArray("disabledPlayers"))
                    try { disabledPlayers.add(UUID.fromString(e.getAsString())); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            Descriptive.LOGGER.error("Failed to load configuration", e);
            save();
        }
    }

    public void save() {
        JsonObject obj = new JsonObject();

        obj.addProperty("color", String.format("#%06X", color & 0xFFFFFF));
        obj.addProperty("bold", bold);
        obj.addProperty("italic", italic);
        obj.addProperty("underlined", underlined);
        obj.addProperty("strikethrough", strikethrough);

        JsonArray gradArray = new JsonArray();
        for (int c : gradientColors) gradArray.add(String.format("#%06X", c & 0xFFFFFF));
        obj.add("gradientColors", gradArray);

        JsonArray animArray = new JsonArray();
        for (String t : animationTypes) animArray.add(t);
        obj.add("animationTypes", animArray);
        obj.addProperty("animationSpeed", animationSpeed);

        obj.addProperty("relayEnabled", relayEnabled);

        JsonArray disabledArray = new JsonArray();
        for (UUID uuid : disabledPlayers) disabledArray.add(uuid.toString());
        obj.add("disabledPlayers", disabledArray);

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(obj));
            ClientNetworkHandler.updateCustomName();
        } catch (IOException e) {
            Descriptive.LOGGER.error("Failed to save configuration", e);
        }
    }

    // Internal setters — no save, used by config screen's batch Done button
    public void setColorInternal(int color)               { this.color = color & 0xFFFFFF; }
    public void setBoldInternal(boolean bold)             { this.bold = bold; }
    public void setItalicInternal(boolean italic)         { this.italic = italic; }
    public void setUnderlinedInternal(boolean underlined) { this.underlined = underlined; }
    public void setStrikethroughInternal(boolean st)      { this.strikethrough = st; }
    public void setAnimationTypesInternal(List<String> t) { this.animationTypes = new ArrayList<>(t); }
    public void setAnimationSpeedInternal(float speed)    { this.animationSpeed = Math.max(0.1f, Math.min(5.0f, speed)); }

    public int getColor()               { return color; }
    public boolean isBold()             { return bold; }
    public boolean isItalic()           { return italic; }
    public boolean isUnderlined()       { return underlined; }
    public boolean isStrikethrough()    { return strikethrough; }
    public boolean isAnimationEnabled() { return !animationTypes.isEmpty(); }
    public float getAnimationSpeed()    { return animationSpeed; }

    public List<Integer> getGradientColors() { return new ArrayList<>(gradientColors); }
    public void setGradientColors(List<Integer> colors) { this.gradientColors = new ArrayList<>(colors); save(); }
    public void setGradientColorsInternal(List<Integer> colors) { this.gradientColors = new ArrayList<>(colors); }

    public List<String> getAnimationTypes() { return new ArrayList<>(animationTypes); }


    // ── Per-subject toggle getters ──────────────────────────────────────


    public boolean isRelayEnabled()              { return relayEnabled; }
    public void setRelayEnabled(boolean v)       { this.relayEnabled = v; }

    public boolean isPlayerEnabled(UUID uuid)             { return !disabledPlayers.contains(uuid); }
    public void setPlayerEnabled(UUID uuid, boolean on)   { if (on) disabledPlayers.remove(uuid); else disabledPlayers.add(uuid); }
    public Set<UUID> getDisabledPlayers()                 { return new HashSet<>(disabledPlayers); }
    public void setDisabledPlayers(Set<UUID> disabled)    { this.disabledPlayers = new HashSet<>(disabled); }
}