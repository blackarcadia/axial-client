package com.axial.cosmetics.client;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Raw plugin-message payload shared with PrisonsCore's minimap overview service. */
public record MinimapMapPayload(byte[] data) implements CustomPayload {
    public static final Id<MinimapMapPayload> ID = new Id<>(Identifier.of("axial_cosmetics", "minimap_map"));
    public static final PacketCodec<RegistryByteBuf, MinimapMapPayload> CODEC = new PacketCodec<>() {
        @Override public MinimapMapPayload decode(RegistryByteBuf buffer) {
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new MinimapMapPayload(data);
        }

        @Override public void encode(RegistryByteBuf buffer, MinimapMapPayload payload) {
            buffer.writeBytes(payload.data);
        }
    };

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
