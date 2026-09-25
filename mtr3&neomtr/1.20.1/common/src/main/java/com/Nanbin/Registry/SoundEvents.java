package com.Nanbin.Registry;

import com.Nanbin.Init;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;

public class SoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENT = DeferredRegister.create(Init.MOD_ID, RegistryKeys.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> CRT_TICKET = SOUND_EVENT.register("crt_ticket", () -> SoundEvent.of(new Identifier(Init.MOD_ID, "crt_ticket")));
    /** 失败提示音（复用 crt_ticket 音频资源，见 sounds.json）。 */
    public static final RegistrySupplier<SoundEvent> CRT_TICKET_ERROR = SOUND_EVENT.register("crt_ticket_error", () -> SoundEvent.of(new Identifier(Init.MOD_ID, "crt_ticket_error")));

    public static void init(){
        SOUND_EVENT.register();
    }
}
