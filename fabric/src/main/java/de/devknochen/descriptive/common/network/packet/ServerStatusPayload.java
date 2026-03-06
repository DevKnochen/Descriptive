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

public record ServerStatusPayload(boolean enabled) implements CustomPayload {

    public static final Id<ServerStatusPayload> ID =
            new Id<>(Identifier.of(Descriptive.MOD_ID, "server_status"));

    public static final PacketCodec<PacketByteBuf, ServerStatusPayload> CODEC = new PacketCodec<>() {
        @Override
        public ServerStatusPayload decode(PacketByteBuf buf) {
            return new ServerStatusPayload(buf.readBoolean());
        }

        @Override
        public void encode(PacketByteBuf buf, ServerStatusPayload p) {
            buf.writeBoolean(p.enabled);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        try { PayloadTypeRegistry.playS2C().register(ID, CODEC); }
        catch (IllegalArgumentException ignored) {}
    }
}