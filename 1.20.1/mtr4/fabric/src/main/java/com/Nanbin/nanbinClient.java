package com.Nanbin;

import net.fabricmc.api.ClientModInitializer;

public class nanbinClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InitClient.init();
    }
}
