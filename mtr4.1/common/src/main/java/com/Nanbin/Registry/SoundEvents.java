package com.Nanbin.Registry;

import com.Nanbin.mapping.ObjectHolder;
import com.Nanbin.mapping.Registry;
import net.minecraft.sound.SoundEvent;

public class SoundEvents {

	public static final ObjectHolder<SoundEvent> CRT_TICKET;
	public static final ObjectHolder<SoundEvent> CRT_TICKET_ERROR;

	static {
		CRT_TICKET = Registry.registerSoundEvent("crt_ticket");
		CRT_TICKET_ERROR = Registry.registerSoundEvent("crt_ticket_error");
	}

	public static void init() {
	}
}
