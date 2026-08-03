package com.Nanbin.mapping;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import mtr.mappings.NetworkUtilities;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class Registry {

    public static void sendPacketToClient(ServerPlayerEntity player, Identifier id, PacketByteBuf packet) {
        NetworkUtilities.sendToPlayer(player, id, packet);
    }

    public static void sendPacketToServer(Identifier id, PacketByteBuf packet) {
        NetworkUtilities.sendToServer(id, packet);
    }

    public static void sendPacketToClient(ServerPlayerEntity player, Identifier id, Consumer<PacketByteBuf> writer) {
        sendPacketToClient(player, id, createPacket(writer));
    }

    public static void sendPacketToServer(Identifier id, Consumer<PacketByteBuf> writer) {
        sendPacketToServer(id, createPacket(writer));
    }

    public static void registerReceiverC2S(Identifier id, NetworkUtilities.PacketCallback callback) {
        NetworkUtilities.registerReceiverC2S(id, callback);
    }

    public static void registerReceiverS2C(Identifier id, NetworkManager.NetworkReceiver callback) {
        NetworkUtilities.registerReceiverS2C(id, callback);
    }

    private static PacketByteBuf createPacket(Consumer<PacketByteBuf> writer) {
        PacketByteBuf packet = new PacketByteBuf(Unpooled.buffer());
        writer.accept(packet);
        return packet;
    }
}
