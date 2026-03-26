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

public record ServerStatusPayload(boolean enabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerStatusPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Descriptive.MOD_ID, "server_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerStatusPayload> CODEC = new StreamCodec<>() {
        @Override
        public ServerStatusPayload decode(RegistryFriendlyByteBuf buf) {
            return new ServerStatusPayload(buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ServerStatusPayload p) {
            buf.writeBoolean(p.enabled());
        }
    };

    @Override
    @SuppressWarnings("NullableProblems")
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register() {
        try {
            PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        } catch (IllegalArgumentException ignored) {}
    }
}