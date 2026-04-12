package com.Nanbin;

import com.Nanbin.Registry.Init;
import net.fabricmc.api.ModInitializer;

public class nanbin implements ModInitializer {

    @Override
    public void onInitialize() {
        Init.init();
        }
}
