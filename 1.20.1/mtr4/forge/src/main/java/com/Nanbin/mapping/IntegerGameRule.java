package com.Nanbin.mapping;

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
