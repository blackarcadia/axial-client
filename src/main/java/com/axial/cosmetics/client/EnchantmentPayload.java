package com.axial.cosmetics.client;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server enchantment catalogue transport. */
public record EnchantmentPayload(byte[] data) implements CustomPayload {
    public static final Id<EnchantmentPayload> ID = new Id<>(Identifier.of("axial_cosmetics", "enchantments"));
    public static final PacketCodec<RegistryByteBuf, EnchantmentPayload> CODEC = new PacketCodec<>() {
        @Override public EnchantmentPayload decode(RegistryByteBuf buffer) {
            if(buffer.readableBytes()>28017)throw new IllegalArgumentException("Enchantment packet exceeds limit");
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new EnchantmentPayload(data);
        }

        @Override public void encode(RegistryByteBuf buffer, EnchantmentPayload payload) {
            buffer.writeBytes(payload.data);
        }
    };

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
