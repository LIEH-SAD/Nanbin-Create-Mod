package com.Nanbin.mapping;

// 1.16.5 uses net.minecraft.world.GameRules; only 1.17+ is supported here
//? if >=1.17 {

import net.minecraft.world.level.GameRules;
import org.mtr.mapping.holder.ServerWorld;
import org.mtr.mapping.tool.HolderBase;

/**
 * @author ZiYueCommentary
 * @since 1.1.0
 */

public class IntegerGameRule extends HolderBase<GameRules.Key<GameRules.IntegerValue>>
{
    public IntegerGameRule(GameRules.Key<GameRules.IntegerValue> data) {
        super(data);
    }

    public static int getValue(ServerWorld world, IntegerGameRule rule) {
        return world.data.getGameRules().getInt(rule.data);
    }
}
//?}
