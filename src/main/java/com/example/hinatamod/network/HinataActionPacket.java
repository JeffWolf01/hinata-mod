package com.example.hinatamod.network;

import com.example.hinatamod.entity.HinataEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: acción elegida en el menú de Hinata. */
public class HinataActionPacket {
    private final int entityId;
    private final int command;

    public HinataActionPacket(int entityId, int command) {
        this.entityId = entityId;
        this.command = command;
    }

    public static void encode(HinataActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.command);
    }

    public static HinataActionPacket decode(FriendlyByteBuf buf) {
        return new HinataActionPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(HinataActionPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Entity e = player.level().getEntity(msg.entityId);
            if (e instanceof HinataEntity hinata && player.distanceToSqr(hinata) <= 64.0D) {
                hinata.handleAction(player, msg.command);
            }
        });
        ctx.setPacketHandled(true);
    }
}
