package com.Nanbin.mapping;

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
