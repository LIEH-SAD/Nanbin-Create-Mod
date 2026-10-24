package com.Nanbin;

import net.fabricmc.api.ClientModInitializer;

public class NanbinClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InitClient.init();
    }
}
