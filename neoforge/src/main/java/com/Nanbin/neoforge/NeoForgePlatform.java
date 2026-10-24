package com.Nanbin.neoforge;

import com.Nanbin.Init;
import com.Nanbin.mapping.RegistrySettings;
import com.Nanbin.mapping.ItemGroupHolder;
import com.Nanbin.mapping.ObjectHolder;
import com.Nanbin.mapping.Platform;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * NeoForge 在模组构造期就已经冻结原版注册表，所以这里用 {@link DeferredRegister}
 * 把注册推迟到 RegisterEvent。
 */
public final class NeoForgePlatform implements Platform {

	private final DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, Init.MOD_ID);
	private final DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, Init.MOD_ID);
	private final DeferredRegister<SoundEvent> soundEvents = DeferredRegister.create(Registries.SOUND_EVENT, Init.MOD_ID);
	private final DeferredRegister<ItemGroup> itemGroups = DeferredRegister.create(Registries.ITEM_GROUP, Init.MOD_ID);
	private final DeferredRegister<BlockEntityType<?>> blockEntityTypes = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Init.MOD_ID);

	public void register(IEventBus modEventBus) {
		blocks.register(modEventBus);
		items.register(modEventBus);
		soundEvents.register(modEventBus);
		itemGroups.register(modEventBus);
		blockEntityTypes.register(modEventBus);
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public void openSignEditor(net.minecraft.client.gui.screen.Screen parent, BlockPos signPos, int lines, int length, String[][] signIds, java.util.List<org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet> selectedIds, String[][] routeNumbers, boolean hasBlockEntityData, Runnable onSave) {
		// 这个界面在 common/src/elementa 里共享；NeoForge 端 elementa 被 MTR 重定位到
		// org.mtr.libraries.gg.essential.*，由 build.gradle 里的 generateElementaSources 替换后编译。
		// 注意它放在独立的 com.Nanbin.signeditor 包里：NeoForge 的模块系统不允许两个模块导出同一个包，
		// 而 common 与各平台模块是分开的模块，所以不能塞进 com.Nanbin.client.Screen。
		net.minecraft.client.MinecraftClient.getInstance().setScreen(
				new com.Nanbin.signeditor.RailwaySignEditorScreen(parent, signPos, lines, length, signIds, selectedIds, false, routeNumbers, onSave));
	}

	@Override
	public void openRailwaySignEditor(net.minecraft.client.gui.screen.Screen parent, BlockPos signPos, int length, String[][] signIds, java.util.List<org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet> selectedIds, String[] routeNumbers, Runnable onSave) {
		net.minecraft.client.MinecraftClient.getInstance().setScreen(
				new com.Nanbin.signeditor.RailwaySignEditorScreen(parent, signPos, 1, length, signIds, selectedIds, true, new String[][]{routeNumbers}, onSave));
	}

	@Override
	public ObjectHolder<Block> registerBlock(Identifier identifier, Function<AbstractBlock.Settings, Block> factory) {
		final DeferredHolder<Block, Block> holder = blocks.register(identifier.getPath(), () -> factory.apply(RegistrySettings.createBlock(identifier)));
		return new ObjectHolder<>(holder::get);
	}

	@Override
	public ObjectHolder<Item> registerItem(Identifier identifier, Function<Item.Settings, Item> factory) {
		final DeferredHolder<Item, Item> holder = items.register(identifier.getPath(), () -> factory.apply(RegistrySettings.createItem(identifier)));
		return new ObjectHolder<>(holder::get);
	}

	@Override
	public ObjectHolder<Item> registerBlockItem(Identifier blockIdentifier, ObjectHolder<Block> block) {
		final DeferredHolder<Item, Item> holder = items.register(blockIdentifier.getPath(), () -> new BlockItem(block.get(), RegistrySettings.createBlockItem(blockIdentifier)));
		return new ObjectHolder<>(holder::get);
	}

	@Override
	public <T extends BlockEntity> ObjectHolder<BlockEntityType<T>> registerBlockEntityType(Identifier identifier, BiFunction<BlockPos, BlockState, T> factory, Supplier<Block>... blocks) {
		final DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> holder = blockEntityTypes.register(identifier.getPath(), () -> BlockEntityTypeFactory.create(factory, blocks));
		return new ObjectHolder<>(holder::get);
	}

	@Override
	public ObjectHolder<SoundEvent> registerSoundEvent(Identifier identifier) {
		final DeferredHolder<SoundEvent, SoundEvent> holder = soundEvents.register(identifier.getPath(), () -> SoundEvent.of(identifier));
		return new ObjectHolder<>(holder::get);
	}

	@Override
	public ObjectHolder<ItemGroup> registerItemGroup(ItemGroupHolder holder) {
		final DeferredHolder<ItemGroup, ItemGroup> deferredHolder = itemGroups.register(holder.getPath(), () -> ItemGroup.builder()
				.displayName(Text.translatable("itemGroup." + Init.MOD_ID + "." + holder.getPath()))
				.icon(holder::getIcon)
				.entries((displayContext, entries) -> holder.getEntries().forEach(entry -> entries.add(entry.get().getDefaultStack())))
				.build());
		return new ObjectHolder<>(deferredHolder::get);
	}
}
