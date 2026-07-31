package com.Nanbin.mappingForge;

import net.minecraft.world.level.GameRules;

/**
 * @since 1.0.0-beta-2
 */

public class GameRuleRegistry
{
    /**
     * Creates a boolean gamerule.
     *
     * @author ZiYueCommentary
     * @since 1.0.0-beta-2
     */
    public static BooleanGameRule registerBoolean(String name, boolean defaultValue) {
        return new BooleanGameRule(GameRules.register(name, GameRules.Category.MISC, GameRules.BooleanValue.create(defaultValue)));
    }

    /**
     * Creates an integer gamerule.
     *
     * @author ZiYueCommentary
     * @since 1.1.0
     */
    public static IntegerGameRule registerInteger(String name, int defaultValue) {
        return new IntegerGameRule(GameRules.register(name, GameRules.Category.MISC, GameRules.IntegerValue.create(defaultValue)));
    }
}
