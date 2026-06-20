package com.Nanbin;

import net.fabricmc.api.ModInitializer;

public class nanbin implements ModInitializer {

    //内部版本号
    public static final String VERSION_DATA = "26105.13";

    @Override
    public void onInitialize() {
        Init.init();
    }
}
