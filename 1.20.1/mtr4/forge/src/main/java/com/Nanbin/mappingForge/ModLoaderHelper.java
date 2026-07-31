package com.Nanbin.mappingForge;

import net.minecraftforge.fml.ModList;

/**
 * @author ZiYueCommentary
 * @since 1.0.0-beta-5
 */

public interface ModLoaderHelper
{
    static boolean hasClothConfig() {
        return ModList.get().isLoaded("cloth-config") || ModList.get().isLoaded("cloth-config2");
    }
}
