
package com.Nanbin.Registry;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import static com.Nanbin.nanbin.LOGGER;

public class NanbinSoundEvents {
    public static final SoundEvent CRT_TICKET_BARRIER = new SoundEvent(new Identifier("crt_ticket"));

    public static void onInitialize() {
        LOGGER.info("[Nanbin Create Mod] Registering Items...");

    }
}






