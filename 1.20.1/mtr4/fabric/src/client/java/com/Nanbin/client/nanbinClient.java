package com.Nanbin.client;

import net.fabricmc.api.ClientModInitializer;

public class nanbinClient implements ClientModInitializer {
    //内部版本号
    public static final String VERSION_DATA = "26105.13";
    @Override
    public void onInitializeClient() {
        InitClient.init();
    }
}
