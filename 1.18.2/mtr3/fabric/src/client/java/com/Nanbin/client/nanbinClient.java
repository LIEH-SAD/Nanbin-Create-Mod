package com.Nanbin.client;

import com.Nanbin.client.ItemsGroup.FiltersGroup;
import com.Nanbin.client.Registry.RenderLayerMap;
import net.fabricmc.api.ClientModInitializer;

public class nanbinClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //方块透明度定义类
        RenderLayerMap.onInitialize();
        //滤波器
        FiltersGroup.onInitialize();
    }
}
