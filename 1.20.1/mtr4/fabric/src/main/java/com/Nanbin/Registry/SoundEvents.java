package com.Nanbin.Registry;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.SoundEventRegistryObject;

import static com.Nanbin.Init.MOD_ID;
import static com.Nanbin.Init.REGISTRY;

public class SoundEvents {
    public static final SoundEventRegistryObject CRT_TICKET;
    public final static SoundEventRegistryObject CRT_TICKET_ERROR;

    static {
        CRT_TICKET = REGISTRY.registerSoundEvent(new Identifier(MOD_ID, "crt_ticket"));
        CRT_TICKET_ERROR = REGISTRY.registerSoundEvent(new Identifier(MOD_ID, "crt_ticket_error"));
    }

    public static void init(){
    }
}
