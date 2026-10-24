package com.Nanbin.mapping;

import com.Nanbin.Init;
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

public final class Registry {

	private Registry() {
	}

	public static Identifier id(String path) {
		return Identifier.of(Init.MOD_ID, path);
	}

	public static ObjectHolder<Block> registerBlock(String path, Function<AbstractBlock.Settings, Block> factory) {
		return PlatformHolder.get().registerBlock(id(path), factory);
	}

	public static ObjectHolder<Block> registerBlockWithBlockItem(String path, Function<AbstractBlock.Settings, Block> factory, ItemGroupHolder itemGroup) {
		final Identifier identifier = id(path);
		final ObjectHolder<Block> block = PlatformHolder.get().registerBlock(identifier, factory);
		final ObjectHolder<Item> item = PlatformHolder.get().registerBlockItem(identifier, block);
		if (itemGroup != null) {
			itemGroup.addItem(item);
		}
		return block;
	}

	public static ObjectHolder<Block> registerBlockWithBlockItem(String path, Function<AbstractBlock.Settings, Block> factory) {
		return registerBlockWithBlockItem(path, factory, null);
	}

	public static ObjectHolder<Item> registerItem(String path, Function<Item.Settings, Item> factory) {
		return PlatformHolder.get().registerItem(id(path), factory);
	}

	public static ObjectHolder<Item> registerItem(String path, Function<Item.Settings, Item> factory, ItemGroupHolder itemGroup) {
		final ObjectHolder<Item> item = PlatformHolder.get().registerItem(id(path), factory);
		if (itemGroup != null) {
			itemGroup.addItem(item);
		}
		return item;
	}

	public static ObjectHolder<SoundEvent> registerSoundEvent(String path) {
		return PlatformHolder.get().registerSoundEvent(id(path));
	}

	/** 注册创作模式标签页；标签页里的物品由 {@link ItemGroupHolder#addItem} 收集。 */
	public static ObjectHolder<ItemGroup> registerItemGroup(ItemGroupHolder holder) {
		return PlatformHolder.get().registerItemGroup(holder);
	}

	/** 注册方块实体类型。 */
	@SafeVarargs
	public static <T extends net.minecraft.block.entity.BlockEntity> ObjectHolder<net.minecraft.block.entity.BlockEntityType<T>> registerBlockEntityType(String path, java.util.function.BiFunction<net.minecraft.util.math.BlockPos, net.minecraft.block.BlockState, T> factory, Supplier<net.minecraft.block.Block>... blocks) {
		return PlatformHolder.get().registerBlockEntityType(id(path), factory, blocks);
	}

	/**
	 * 注册自定义网络包。
	 * <p>
	 * MTR 4.1 的包通道按「类名 → 构造函数」查表，且注册表是静态的，
	 * 所以附属 mod 可以直接复用它的通道：用 {@code org.mtr.registry.RegistryServer} 注册/发送，
	 * 不需要自己再建一套 payload。
	 */
	public static <T extends org.mtr.packet.PacketHandler> void registerPacket(Class<T> classObject, java.util.function.Function<org.mtr.packet.PacketBufferReceiver, T> getInstance) {
		org.mtr.registry.RegistryServer.registerPacket(classObject, getInstance);
	}

	/** 服务端 → 客户端发送。 */
	public static <T extends org.mtr.packet.PacketHandler> void sendPacketToClient(net.minecraft.server.network.ServerPlayerEntity player, T data) {
		org.mtr.registry.RegistryServer.sendPacketToClient(player, data);
	}

	/** 客户端 → 服务端发送。 */
	public static <T extends org.mtr.packet.PacketHandler> void sendPacketToServer(T data) {
		org.mtr.registry.RegistryClient.sendPacketToServer(data);
	}

	// ---- 客户端注册（只会在客户端初始化阶段被调用）----
	// MTR 4.1 的 RegistryClient 同样是 public static 入口，而且它内部已经处理了
	// Fabric / NeoForge 两端的差异（fabric-api 的 BlockRenderLayerMap / ColorProviderRegistry /
	// BlockEntityRenderers 与 NeoForge 的 ItemBlockRenderTypes / 事件总线），
	// 所以这里直接转发，不需要自己再写一套平台实现。
	// 注意：RegistryClient 收的是它自己的 ObjectHolder，所以调用前要包一层。

	/** 注册方块的渲染层（例如 cutout）。 */
	public static void registerBlockRenderLayer(net.minecraft.client.render.RenderLayer renderLayer, ObjectHolder<net.minecraft.block.Block> block) {
		org.mtr.registry.RegistryClient.registerBlockRenderType(renderLayer, mtrHolder(block));
	}

	/** 注册方块染色（例如站点颜色染色）。 */
	@SafeVarargs
	public static void registerBlockColors(net.minecraft.client.color.block.BlockColorProvider blockColor, ObjectHolder<net.minecraft.block.Block>... blocks) {
		final org.mtr.registry.ObjectHolder<net.minecraft.block.Block>[] holders = new org.mtr.registry.ObjectHolder[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			holders[i] = mtrHolder(blocks[i]);
		}
		org.mtr.registry.RegistryClient.registerBlockColors(blockColor, holders);
	}

	/**
	 * 注册方块实体渲染器。
	 * <p>
	 * 刻意只用<b>一个</b>类型参数（MTR 那边是 {@code <T extends BlockEntity, U extends T>}）：
	 * 两个类型参数时，像 {@code dispatcher -> new RenderCRTStationInfo1()} 这种
	 * 「lambda 返回具体子类」的写法会让 javac 把 T 推成具体渲染器类，从而报
	 * 「推论变量 T 具有不兼容的上限」。这里收窄成单参数后，T 由
	 * {@code ObjectHolder<BlockEntityType<T>>} 唯一定出，lambda 只要返回它的子类型即可。
	 */
	public static <T extends BlockEntity> void registerBlockEntityRenderer(ObjectHolder<BlockEntityType<T>> blockEntityType, net.minecraft.client.render.block.entity.BlockEntityRendererFactory<T> provider) {
		org.mtr.registry.RegistryClient.registerBlockEntityRenderer(mtrHolder(blockEntityType), provider);
	}

	/** 把本项目的 ObjectHolder 适配成 MTR 4.1 的 ObjectHolder。 */
	public static <T> org.mtr.registry.ObjectHolder<T> mtrHolder(ObjectHolder<T> holder) {
		return new org.mtr.registry.ObjectHolder<>(holder::get);
	}
}
