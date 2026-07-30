package com.Nanbin;

import net.fabricmc.api.ModInitializer;

public class nanbin implements ModInitializer {

    //内部版本号（VERSION_DATA = 编译时间）
    public static final String VERSION = "3.0";
    public static final String VERSION_DATA = "26801";
    public static final String DESIGNED_MTR_VERSION = "4.0.0";
    public static final String FINAL_VERSION = "{}.{}+MTR{}".formatted(VERSION, VERSION_DATA, DESIGNED_MTR_VERSION);

    @Override
    public void onInitialize() {
        Init.init();
    }
}
