package com.axial.cosmetics.client;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** World-border overview transport supplied by the standalone AxialWorldMap plugin. */
public record WorldMapPayload(byte[] data) implements CustomPayload {
    public static final Id<WorldMapPayload> ID = new Id<>(Identifier.of("axial_cosmetics", "world_map"));
    public static final PacketCodec<RegistryByteBuf, WorldMapPayload> CODEC = new PacketCodec<>() {
        @Override public WorldMapPayload decode(RegistryByteBuf buffer) {
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new WorldMapPayload(data);
        }

        @Override public void encode(RegistryByteBuf buffer, WorldMapPayload payload) {
            buffer.writeBytes(payload.data);
        }
    };

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
