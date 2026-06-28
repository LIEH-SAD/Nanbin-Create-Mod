package com.Nanbin;

import net.fabricmc.api.ModInitializer;

public class entrypoint implements ModInitializer {

    //内部版本号
    public static final String VERSION = "3.0";
    public static final String VERSION_DATA = "26628";
    public static final String DESIGNED_MTR_VERSION = "4.0.0";
    public static final String FINAL_VERSION = VERSION + "." + VERSION_DATA + " + mtr" + DESIGNED_MTR_VERSION;

    @Override
    public void onInitialize() {
        Init.init();
    }
}
