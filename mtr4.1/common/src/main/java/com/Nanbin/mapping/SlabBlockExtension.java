package com.Nanbin.mapping;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * 台阶方块适配层，取代 MTR 4.0 的 {@code org.mtr.mapping.mapper.SlabBlockExtension}。
 * 同样保留 {@code addBlockProperties} 的写法。
 */
public abstract class SlabBlockExtension extends SlabBlock {

	protected SlabBlockExtension(AbstractBlock.Settings settings) {
		super(settings);
	}

	/** 子类在这里声明自己用到的方块状态属性。 */
	public void addBlockProperties(List<Property<?>> properties) {
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		final List<Property<?>> properties = new ArrayList<>();
		addBlockProperties(properties);
		properties.forEach(builder::add);
	}
}
