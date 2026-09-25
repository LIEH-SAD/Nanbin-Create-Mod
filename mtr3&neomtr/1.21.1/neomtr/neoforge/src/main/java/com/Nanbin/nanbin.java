package com.Nanbin;


import com.Nanbin.Blocks.Blocks;
import com.Nanbin.Registry.Init;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.Nanbin.Registry.Init.MOD_ID;

@Mod(MOD_ID)
public class nanbin {
    public static final Logger LOGGER = LoggerFactory.getLogger("Nanbin Create Mod");
    public static final Path GAME_DIR = FMLPaths.GAMEDIR.get();

    public nanbin(IEventBus modEventBus) {
        Init.init(modEventBus);
    }

}