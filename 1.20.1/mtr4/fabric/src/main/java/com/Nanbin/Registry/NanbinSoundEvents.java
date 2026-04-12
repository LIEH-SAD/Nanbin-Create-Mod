package com.Nanbin.Registry;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.SoundEventRegistryObject;

import static com.Nanbin.Registry.Init.MOD_ID;
import static com.Nanbin.Registry.Init.REGISTRY;

public class NanbinSoundEvents {
    public final static SoundEventRegistryObject CRT_TICKET;

    static {
        CRT_TICKET = REGISTRY.registerSoundEvent(new Identifier(MOD_ID, "crt_ticket"));
    }

    public static void init(){
    }
}
