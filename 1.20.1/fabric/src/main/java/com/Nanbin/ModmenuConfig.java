package com.Nanbin;

import com.Nanbin.client.Screen.NanbinConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModmenuConfig implements ModMenuApi {
    public ModmenuConfig() {
    }

    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> new NanbinConfigScreen(parent);
    }
}
