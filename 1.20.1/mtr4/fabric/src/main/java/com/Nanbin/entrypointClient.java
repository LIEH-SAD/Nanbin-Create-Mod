package com.Nanbin;

import net.fabricmc.api.ClientModInitializer;

public class entrypointClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InitClient.init();
    }
}
