package com.Nanbin.mapping;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 由各平台（fabric / neoforge）在模组初始化时实现的加载器相关能力。
 * <p>
 * 注册必须放在平台层：Fabric 可以直接写原版注册表，NeoForge 在模组构造期注册表已经冻结，
 * 只能用 {@code DeferredRegister} 推迟到 RegisterEvent。
 */
public interface Platform {

	boolean isModLoaded(String modId);

	ObjectHolder<Block> registerBlock(Identifier identifier, Function<AbstractBlock.Settings, Block> factory);

	ObjectHolder<Item> registerItem(Identifier identifier, Function<Item.Settings, Item> factory);

	/** 为方块注册对应的方块物品（BlockItem）。 */
	ObjectHolder<Item> registerBlockItem(Identifier blockIdentifier, ObjectHolder<Block> block);

	ObjectHolder<SoundEvent> registerSoundEvent(Identifier identifier);

	/** 注册方块实体类型（各 loader 的构建方式不同，1.21.4 的原版构造器是私有的）。 */
	<T extends BlockEntity> ObjectHolder<BlockEntityType<T>> registerBlockEntityType(Identifier identifier, BiFunction<BlockPos, BlockState, T> factory, Supplier<Block>... blocks);

	/** 注册创作模式标签页（内容由 common 的 ItemGroupHolder 收集）。 */
	ObjectHolder<ItemGroup> registerItemGroup(ItemGroupHolder holder);

	/**
	 * 打开多行指示牌编辑器（站台信息屏用）。
	 * <p>
	 * 必须由各平台实现：这个界面基于 MTR 自带的界面组件（{@code org.mtr.widget.*} + elementa），
	 * 而 elementa 的包名两端不一样（Fabric 是 {@code gg.essential.*}，NeoForge 被 MTR 重定位成
	 * {@code org.mtr.libraries.gg.essential.*}），同一份 common 源码编译不出来。
	 * 所以界面源码放在 {@code common/src/elementa}，两端各自编译一份（见各自 build.gradle 的说明）。
	 */
	default void openSignEditor(net.minecraft.client.gui.screen.Screen parent, BlockPos signPos, int lines, int length, String[][] signIds, java.util.List<org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet> selectedIds, String[][] routeNumbers, boolean hasBlockEntityData, Runnable onSave) {
	}

	/**
	 * 打开单行指示牌编辑器（CRT 铁路告示牌用）。
	 * <p>
	 * 和 {@link #openSignEditor} 同一套实现，差别只在数据的组织方式：
	 * 这里的 {@code selectedIds} 是<b>逐格</b>的（MTR 4.1 的告示牌方块实体就是按格存配色），
	 * 而站台信息屏的双层指示牌是按行存。
	 */
	default void openRailwaySignEditor(net.minecraft.client.gui.screen.Screen parent, BlockPos signPos, int length, String[][] signIds, java.util.List<org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet> selectedIds, String[] routeNumbers, Runnable onSave) {
	}
}
