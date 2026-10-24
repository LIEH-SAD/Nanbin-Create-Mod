package com.Nanbin.packet;

import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public final class PacketSyncCustomBalance extends PacketHandler {

	private final int balance;

	public PacketSyncCustomBalance(PacketBufferReceiver packetBufferReceiver) {
		this.balance = packetBufferReceiver.readInt();
	}

	public PacketSyncCustomBalance(int balance) {
		this.balance = balance;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeInt(balance);
	}

	@Override
	public void runClient() {
		ClientPacketHelper.syncTicketMachineBalance(balance);
	}
}
