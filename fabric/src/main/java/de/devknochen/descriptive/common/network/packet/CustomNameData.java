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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

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
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CustomNameData> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Descriptive.MOD_ID, "sync"));
    public static final int CURRENT_PROTOCOL_VERSION = 3;

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomNameData> CODEC = new StreamCodec<>() {
        @Override
        public CustomNameData decode(RegistryFriendlyByteBuf buf) {
            int version = buf.readVarInt();
            UUID playerUuid = buf.readUUID();
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
                for (int i = 0; i < animCount; i++) animationTypes.add(buf.readUtf());
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
        public void encode(RegistryFriendlyByteBuf buf, CustomNameData data) {
            buf.writeVarInt(data.protocolVersion());
            buf.writeUUID(data.playerUuid());
            buf.writeInt(data.color());
            buf.writeBoolean(data.bold());
            buf.writeBoolean(data.italic());
            buf.writeBoolean(data.underlined());
            buf.writeBoolean(data.strikethrough());
            buf.writeBoolean(data.animationEnabled());
            buf.writeVarInt(data.animationTypes().size());
            for (String animType : data.animationTypes()) buf.writeUtf(animType);
            buf.writeFloat(data.animationSpeed());
            buf.writeVarInt(data.gradientColors().size());
            for (int c : data.gradientColors()) buf.writeInt(c);
        }
    };

    @Override
    @SuppressWarnings("NullableProblems")
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register() {
        try {
            PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        } catch (IllegalArgumentException ignored) {}

        try {
            PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        } catch (IllegalArgumentException ignored) {}

        ServerStatusPayload.register();
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