package com.axial.cosmetics.client;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server gang directory transport. */
public record GangDirectoryPayload(byte[] data) implements CustomPayload {
    public static final Id<GangDirectoryPayload> ID = new Id<>(Identifier.of("axial_cosmetics", "gangs"));
    public static final PacketCodec<RegistryByteBuf, GangDirectoryPayload> CODEC = new PacketCodec<>() {
        @Override public GangDirectoryPayload decode(RegistryByteBuf buffer) {
            if(buffer.readableBytes()>28017)throw new IllegalArgumentException("Gang directory packet exceeds limit");
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new GangDirectoryPayload(data);
        }

        @Override public void encode(RegistryByteBuf buffer, GangDirectoryPayload payload) {
            buffer.writeBytes(payload.data);
        }
    };

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
