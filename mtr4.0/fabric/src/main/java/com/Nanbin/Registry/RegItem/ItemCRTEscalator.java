package com.Nanbin.Registry.RegItem;

import com.Nanbin.Blocks.Blocks;
import com.Nanbin.mapping.TranslationProvider;
import org.mtr.mapping.holder.ActionResult;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.ItemUsageContext;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Property;
import org.mtr.mapping.holder.TextFormatting;
import org.mtr.mapping.holder.TooltipContext;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mod.block.BlockEscalatorBase;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.mtr.mod.item.ItemPSDAPGBase.blocksNotReplaceable;

/**
 * CRT 自动扶梯物品：一次放置 2 格宽的梯级与两侧侧板（各 2 格高）。
 * 在已有扶梯的末端继续放置会自动反向衔接，从而延长整段扶梯；
 * 平台（landing）、斜坡（slope）与上下过渡段由方块自身按相邻方块判定。
 *
 * @see com.Nanbin.Registry.RegBlock.Escalator.BlockCRTEscalatorStep
 * @see com.Nanbin.Registry.RegBlock.Escalator.BlockCRTEscalatorSide
 */
public class ItemCRTEscalator extends ItemExtension implements IBlock {

    public ItemCRTEscalator(ItemSettings settings) {
        super(settings);
    }

    @Nonnull
    @Override
    public ActionResult useOnBlock2(ItemUsageContext context) {
        // 2 格宽 × 2 格高的空间必须都可替换
        if (blocksNotReplaceable(context, 2, 2, null)) {
            return ActionResult.PASS;
        }

        final World world = context.getWorld();
        Direction facing = context.getPlayerFacing();
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        BlockPos pos2 = pos.offset(facing.getOpposite());

        // 贴着已有扶梯放置时反向衔接，使新的一段与已有段朝向一致
        final BlockState adjacentState = world.getBlockState(pos.offset(facing));
        if (adjacentState.getBlock().data instanceof BlockEscalatorBase && IBlock.getStatePropertySafe(adjacentState, DirectionHelper.FACING) == facing.getOpposite()) {
            facing = facing.getOpposite();
            final BlockPos temp = pos;
            pos = pos2;
            pos2 = temp;
        }

        final BlockState stepState = Blocks.CRT_ESCALATOR_STEP.get().getDefaultState().with(new Property<>(DirectionHelper.FACING.data), facing.data);
        world.setBlockState(pos, stepState.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.LEFT));
        world.setBlockState(pos2, stepState.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.RIGHT));

        final BlockState sideState = Blocks.CRT_ESCALATOR_SIDE.get().getDefaultState().with(new Property<>(DirectionHelper.FACING.data), facing.data);
        world.setBlockState(pos.up(), sideState.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.LEFT));
        world.setBlockState(pos2.up(), sideState.with(new Property<>(IBlock.SIDE.data), IBlock.EnumSide.RIGHT));

        context.getStack().decrement(1);
        return ActionResult.SUCCESS;
    }

    @Override
    public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TranslationProvider.BRUSH_USE.getMutableText(new Object[0]).formatted(TextFormatting.DARK_GRAY));
    }
}
