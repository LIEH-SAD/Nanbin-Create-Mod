package com.Nanbin;

import com.Nanbin.client.Menu.NanbinConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModmenuConfig implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> new NanbinConfigScreen();
    }
}
