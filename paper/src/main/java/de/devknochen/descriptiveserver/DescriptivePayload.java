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

package de.devknochen.descriptiveserver;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Latest Protocol version: 3 (The Gradient Colors Update)

public record DescriptivePayload(
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
) {

    public static final String CHANNEL         = "descriptive:sync";
    public static final int    PROTOCOL_VERSION = 3;

    public DescriptivePayload {
        animationTypes = new ArrayList<>(animationTypes);
        gradientColors = new ArrayList<>(gradientColors);
    }

    public byte[] encode() {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream      out  = new java.io.DataOutputStream(baos);

            writeVarInt(out, PROTOCOL_VERSION);
            writeUuid(out, playerUuid);
            out.writeInt(color);
            out.writeBoolean(bold);
            out.writeBoolean(italic);
            out.writeBoolean(underlined);
            out.writeBoolean(strikethrough);

            out.writeBoolean(animationEnabled);
            writeVarInt(out, animationTypes.size());
            for (String type : animationTypes) writeString(out, type);
            out.writeFloat(animationSpeed);

            writeVarInt(out, gradientColors.size());
            for (int c : gradientColors) out.writeInt(c);

            return baos.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to encode DescriptivePayload", e);
        }
    }

    public static DescriptivePayload decode(byte[] data) {
        try {
            java.io.DataInputStream in =
                    new java.io.DataInputStream(new java.io.ByteArrayInputStream(data));

            int version = readVarInt(in);
            if (version < 2) return null;

            UUID    uuid          = readUuid(in);
            int     color         = in.readInt();
            boolean bold          = in.readBoolean();
            boolean italic        = in.readBoolean();
            boolean underlined    = in.readBoolean();
            boolean strikethrough = in.readBoolean();

            boolean      animEnabled = in.readBoolean();
            int          animCount   = readVarInt(in);
            List<String> animTypes   = new ArrayList<>(animCount);
            for (int i = 0; i < animCount; i++) animTypes.add(readString(in));
            float animSpeed = in.readFloat();

            List<Integer> gradientColors = new ArrayList<>(List.of(0xFF0000, 0x0000FF));
            if (version >= 3) {
                int gradCount = readVarInt(in);
                gradientColors = new ArrayList<>(gradCount);
                for (int i = 0; i < gradCount; i++) gradientColors.add(in.readInt());
            }

            return new DescriptivePayload(uuid, color, bold, italic, underlined,
                    strikethrough, animTypes, animSpeed, animEnabled, gradientColors);

        } catch (Exception e) {
            return null;
        }
    }

    private static void writeVarInt(java.io.DataOutputStream out, int value) throws java.io.IOException {
        while ((value & 0xFFFFFF80) != 0) { out.writeByte((value & 0x7F) | 0x80); value >>>= 7; }
        out.writeByte(value & 0x7F);
    }

    private static int readVarInt(java.io.DataInputStream in) throws java.io.IOException {
        int value = 0, position = 0; byte currentByte;
        do {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;
            position += 7;
            if (position >= 32) throw new java.io.IOException("VarInt too big");
        } while ((currentByte & 0x80) != 0);
        return value;
    }

    private static void writeUuid(java.io.DataOutputStream out, UUID uuid) throws java.io.IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(java.io.DataInputStream in) throws java.io.IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private static void writeString(java.io.DataOutputStream out, String s) throws java.io.IOException {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(java.io.DataInputStream in) throws java.io.IOException {
        byte[] bytes = new byte[readVarInt(in)];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
