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

package de.devknochen.descriptive.common.network.packet;

import de.devknochen.descriptive.Descriptive;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CustomNameData(
        int protocolVersion,
        UUID playerUuid,
        int color,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        List<String> animationTypes,
        float animationSpeed,
        boolean animationEnabled,
        List<Integer> gradientColors
) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(Descriptive.MOD_ID, "sync");
    public static final Id<CustomNameData> ID = new Id<>(PACKET_ID);
    public static final int CURRENT_PROTOCOL_VERSION = 3;

    public static final PacketCodec<PacketByteBuf, CustomNameData> CODEC = new PacketCodec<>() {
        @Override
        public CustomNameData decode(PacketByteBuf buf) {
            int version = buf.readVarInt();
            UUID playerUuid = buf.readUuid();
            int color = buf.readInt();
            boolean bold = buf.readBoolean();
            boolean italic = buf.readBoolean();
            boolean underlined = buf.readBoolean();
            boolean strikethrough = buf.readBoolean();

            List<String> animationTypes = new ArrayList<>();
            float animationSpeed = 1.0f;
            boolean animationEnabled = false;
            List<Integer> gradientColors = new ArrayList<>(List.of(0xFF0000, 0x0000FF));

            if (version >= 2) {
                animationEnabled = buf.readBoolean();
                int animCount = buf.readVarInt();
                for (int i = 0; i < animCount; i++) animationTypes.add(buf.readString());
                animationSpeed = buf.readFloat();
            }

            if (version >= 3) {
                int gradCount = buf.readVarInt();
                gradientColors = new ArrayList<>();
                for (int i = 0; i < gradCount; i++) gradientColors.add(buf.readInt());
            }

            return new CustomNameData(version, playerUuid, color, bold, italic, underlined,
                    strikethrough, animationTypes, animationSpeed, animationEnabled, gradientColors);
        }

        @Override
        public void encode(PacketByteBuf buf, CustomNameData data) {
            buf.writeVarInt(data.protocolVersion);
            buf.writeUuid(data.playerUuid);
            buf.writeInt(data.color);
            buf.writeBoolean(data.bold);
            buf.writeBoolean(data.italic);
            buf.writeBoolean(data.underlined);
            buf.writeBoolean(data.strikethrough);
            buf.writeBoolean(data.animationEnabled);
            buf.writeVarInt(data.animationTypes.size());
            for (String animType : data.animationTypes) buf.writeString(animType);
            buf.writeFloat(data.animationSpeed);
            buf.writeVarInt(data.gradientColors.size());
            for (int c : data.gradientColors) buf.writeInt(c);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        try {
            PayloadTypeRegistry.playC2S().register(ID, CODEC);
        } catch (IllegalArgumentException ignored) {}

        try {
            PayloadTypeRegistry.playS2C().register(ID, CODEC);
        } catch (IllegalArgumentException ignored) {}
    }

    public static CustomNameData create(UUID playerUuid, int color, boolean bold,
                                        boolean italic, boolean underlined,
                                        boolean strikethrough,
                                        List<String> animationTypes,
                                        float animationSpeed,
                                        boolean animationEnabled,
                                        List<Integer> gradientColors) {
        return new CustomNameData(CURRENT_PROTOCOL_VERSION, playerUuid, color,
                bold, italic, underlined, strikethrough,
                animationTypes, animationSpeed, animationEnabled, gradientColors);
    }
}