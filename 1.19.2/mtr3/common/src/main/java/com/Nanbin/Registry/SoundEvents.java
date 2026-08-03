package com.Nanbin.Registry;

import com.Nanbin.Init;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENT = DeferredRegister.create(Init.MOD_ID, Registry.SOUND_EVENT_KEY);

    public static final RegistrySupplier<SoundEvent> CRT_TICKET = SOUND_EVENT.register("crt_ticket", () -> new SoundEvent(new Identifier(Init.MOD_ID, "crt_ticket")));

    public static void init(){
        SOUND_EVENT.register();
    }
}
