package com.Nanbin.fabric;

import com.Nanbin.Init;
import com.Nanbin.mapping.RegistrySettings;
import com.Nanbin.mapping.ItemGroupHolder;
import com.Nanbin.mapping.ObjectHolder;
import com.Nanbin.mapping.Platform;
import com.Nanbin.mapping.Registry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
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

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricPlatform implements Platform {

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public void openSignEditor(net.minecraft.client.gui.screen.Screen parent, BlockPos signPos, int lines, int length, String[][] signIds, java.util.List<org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet> selectedIds, String[][] routeNumbers, boolean hasBlockEntityData, Runnable onSave) {
		// 这个界面在 common/src/elementa 里共享；Fabric 端 elementa 的包名就是 gg.essential.*，直接编译。
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
		net.minecraft.registry.Registry.register(Registries.BLOCK, identifier, factory.apply(RegistrySettings.createBlock(identifier)));
		return new ObjectHolder<>(() -> Registries.BLOCK.get(identifier));
	}

	@Override
	public ObjectHolder<Item> registerItem(Identifier identifier, Function<Item.Settings, Item> factory) {
		net.minecraft.registry.Registry.register(Registries.ITEM, identifier, factory.apply(RegistrySettings.createItem(identifier)));
		return new ObjectHolder<>(() -> Registries.ITEM.get(identifier));
	}

	@Override
	public ObjectHolder<Item> registerBlockItem(Identifier blockIdentifier, ObjectHolder<Block> block) {
		net.minecraft.registry.Registry.register(Registries.ITEM, blockIdentifier, new BlockItem(block.get(), RegistrySettings.createBlockItem(blockIdentifier)));
		return new ObjectHolder<>(() -> Registries.ITEM.get(blockIdentifier));
	}

	@Override
	public <T extends BlockEntity> ObjectHolder<BlockEntityType<T>> registerBlockEntityType(Identifier identifier, BiFunction<BlockPos, BlockState, T> factory, Supplier<Block>... blocks) {
		final Block[] blockArray = new Block[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			blockArray[i] = blocks[i].get();
		}
		final BlockEntityType<T> type = net.minecraft.registry.Registry.register(Registries.BLOCK_ENTITY_TYPE, identifier,
				FabricBlockEntityTypeBuilder.create(factory::apply, blockArray).build());
		return new ObjectHolder<>(() -> type);
	}

	@Override
	public ObjectHolder<SoundEvent> registerSoundEvent(Identifier identifier) {
		net.minecraft.registry.Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
		return new ObjectHolder<>(() -> Registries.SOUND_EVENT.get(identifier));
	}

	@Override
	public ObjectHolder<ItemGroup> registerItemGroup(ItemGroupHolder holder) {
		final Identifier identifier = Registry.id(holder.getPath());
		final ItemGroup itemGroup = net.minecraft.registry.Registry.register(Registries.ITEM_GROUP, identifier, FabricItemGroup.builder()
				.displayName(Text.translatable("itemGroup." + Init.MOD_ID + "." + holder.getPath()))
				.icon(holder::getIcon)
				.entries((displayContext, entries) -> holder.getEntries().forEach(entry -> entries.add(entry.get().getDefaultStack())))
				.build());
		return new ObjectHolder<>(() -> itemGroup);
	}
}
