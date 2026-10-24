package com.axial.cosmetics.client;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server-authoritative combat countdown transport. */
public record CombatCooldownPayload(byte[] data) implements CustomPayload {
    public static final Id<CombatCooldownPayload> ID = new Id<>(Identifier.of("axial_cosmetics", "combat"));
    public static final PacketCodec<RegistryByteBuf, CombatCooldownPayload> CODEC = new PacketCodec<>() {
        @Override public CombatCooldownPayload decode(RegistryByteBuf buffer) {
            if(buffer.readableBytes()!=9)throw new IllegalArgumentException("Invalid combat cooldown packet");
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new CombatCooldownPayload(data);
        }

        @Override public void encode(RegistryByteBuf buffer, CombatCooldownPayload payload) {
            buffer.writeBytes(payload.data);
        }
    };

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
