package com.Nanbin.Registry.RegBlock;

import com.Nanbin.Registry.SoundEvents;
import com.Nanbin.mapping.BlockExtension;
import com.Nanbin.mapping.DirectionHelper;
import com.Nanbin.mapping.TranslationProvider;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.mtr.block.IBlock;
import org.mtr.data.TicketSystem;
import org.mtr.data.TicketSystem.EnumTicketBarrierOpen;
import org.mtr.registry.Items;

import java.util.List;

/**
 * CRT 闸机（一代）。
 * <p>
 * 4.1 适配：{@code onUse2} / {@code onEntityCollision2} / {@code scheduledTick2} 三个钩子由
 * {@link BlockExtension} 转发；{@code PlayerEntity.isInstance} → {@code instanceof}；
 * {@code hasScheduledBlockTick}/{@code scheduleBlockTick} → 原版方块计划刻 API；
 * MTR 的 {@code passThrough} 在 4.1 需要 5 个音效参数。
 */
public class BlockCRTTicketBarrier1 extends BlockExtension {

	/** 闸机开合状态。 */
	public static final EnumProperty<EnumTicketBarrierOpen> OPEN = EnumProperty.of("open", EnumTicketBarrierOpen.class);

	private final boolean isEntrance;

	public BlockCRTTicketBarrier1(AbstractBlock.Settings settings, boolean isEntrance) {
		super(settings.nonOpaque().luminance(state -> 5));
		this.isEntrance = isEntrance;
	}

	@Override
	public void addBlockProperties(List<Property<?>> properties) {
		properties.add(Properties.HORIZONTAL_FACING);
		properties.add(OPEN);
		properties.add(TicketFault.FAULT);
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing()).with(OPEN, EnumTicketBarrierOpen.CLOSED);
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		return VoxelShapes.union(
				IBlock.getVoxelShapeByDirection(12, 0, -7, 16, 16, 23, facing),
				IBlock.getVoxelShapeByDirection(-4, 0, -7, 0, 16, 23, facing));
	}

	@Override
	public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final EnumTicketBarrierOpen open = IBlock.getStatePropertySafe(state, OPEN);
		final VoxelShape base = IBlock.getVoxelShapeByDirection(15, 0, 0, 16, 24, 16, facing);
		return open != EnumTicketBarrierOpen.OPEN && open != EnumTicketBarrierOpen.OPEN_CONCESSIONARY
				? VoxelShapes.union(
						IBlock.getVoxelShapeByDirection(-4, 0, -7, 0, 24, 23, facing),
						IBlock.getVoxelShapeByDirection(12, 0, -7, 16, 24, 23, facing),
						IBlock.getVoxelShapeByDirection(0, 0, 6, 12, 24, 8, facing))
				: base;
	}

	@Override
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (player.isHolding(Items.BRUSH.get())) {
			return IBlock.checkHoldingBrush(world, player, () -> toggleFault(world, pos, state, player));
		}
		return ActionResult.PASS;
	}

	/** 服务端：切换闸机故障状态并关闭闸门，故障期间闸机拒绝通行。 */
	private static void toggleFault(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final boolean fault = !TicketFault.isFault(state);
		world.setBlockState(pos, state.with(TicketFault.FAULT, fault).with(OPEN, EnumTicketBarrierOpen.CLOSED), 3);
		TicketFault.notifyToggle(player, fault);
	}

	@Override
	public void onEntityCollision2(BlockState state, World world, BlockPos pos, Entity entity) {
		if (world.isClient || !(entity instanceof PlayerEntity player)) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, Properties.HORIZONTAL_FACING);
		final Vec3d playerPosRotated = entity.getPos()
				.subtract(pos.getX() + 0.5, 0, pos.getZ() + 0.5)
				.rotateY((float) Math.toRadians(DirectionHelper.asRotation(facing)));
		final EnumTicketBarrierOpen open = IBlock.getStatePropertySafe(state, OPEN);

		if ((open == EnumTicketBarrierOpen.OPEN || open == EnumTicketBarrierOpen.OPEN_CONCESSIONARY) && playerPosRotated.getZ() > 0) {
			world.setBlockState(pos, state.with(OPEN, EnumTicketBarrierOpen.CLOSED));
		} else if (open == EnumTicketBarrierOpen.CLOSED && playerPosRotated.getZ() < 0) {
			if (TicketFault.isFault(state)) {
				TicketFault.notifyInUse(world, player);
			} else {
				world.setBlockState(pos, state.with(OPEN, EnumTicketBarrierOpen.PENDING));
				final SoundEvent ticketSound = SoundEvents.CRT_TICKET.get();
				TicketSystem.passThrough(world, pos, player, isEntrance, !isEntrance, ticketSound, ticketSound, ticketSound, ticketSound, ticketSound, false, newOpen -> {
					world.setBlockState(pos, state.with(OPEN, newOpen));
					if (newOpen != EnumTicketBarrierOpen.CLOSED && !world.getBlockTickScheduler().isQueued(pos, state.getBlock())) {
						world.scheduleBlockTick(pos, state.getBlock(), 40);
					}
				});
			}
		}
	}

	@Override
	public void scheduledTick2(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		world.setBlockState(pos, state.with(OPEN, EnumTicketBarrierOpen.CLOSED));
	}

	@Override
	public void addTooltips(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(TranslationProvider.TOOLTIP_STATION_COLOR.getMutableText().formatted(Formatting.DARK_GRAY));
	}
}
