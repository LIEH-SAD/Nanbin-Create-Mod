package com.Nanbin.packet;

import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

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
