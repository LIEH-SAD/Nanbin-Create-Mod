package com.Nanbin.mapping;

// 1.16.5 uses net.minecraft.world.GameRules; only 1.17+ is supported here
//? if >=1.17 {

import net.minecraft.world.level.GameRules;
import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.tool.HolderBase;

/**
 * @author ZiYueCommentary
 * @since 1.0.0-beta-2
 */

public class BooleanGameRule extends HolderBase<GameRules.Key<GameRules.BooleanValue>>
{
    public BooleanGameRule(GameRules.Key<GameRules.BooleanValue> data) {
        super(data);
    }

    public static boolean getValue(ServerWorld world, BooleanGameRule rule) {
        return world.data.getGameRules().getBoolean(rule.data);
    }
}
//?}
