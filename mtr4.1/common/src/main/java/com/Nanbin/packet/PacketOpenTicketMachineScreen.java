package com.Nanbin.packet;

import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

public final class PacketOpenTicketMachineScreen extends PacketHandler {

	private final int balance;

	public PacketOpenTicketMachineScreen(PacketBufferReceiver packetBufferReceiver) {
		this.balance = packetBufferReceiver.readInt();
	}

	public PacketOpenTicketMachineScreen(int balance) {
		this.balance = balance;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeInt(balance);
	}

	@Override
	public void runClient() {
		ClientPacketHelper.openTicketMachineScreen(balance);
	}
}
