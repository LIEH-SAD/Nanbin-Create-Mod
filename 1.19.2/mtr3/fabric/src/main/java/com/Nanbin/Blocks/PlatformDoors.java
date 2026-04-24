package com.Nanbin.Blocks;

import com.Nanbin.ItemsGroup.ItemsGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import static com.Nanbin.nanbin.LOGGER;

public class PlatformDoors {
    public static final Block PLATFORM_DOOR = new DoorBlock(FabricBlockSettings.copyOf(Blocks.IRON_DOOR).nonOpaque()) {
        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            // 判断是否手持钥匙
            boolean hasKey = player.getMainHandStack().isOf(mtr.Items.DRIVER_KEY.get()) || player.getOffHandStack().isOf(mtr.Items.DRIVER_KEY.get());
            if (!hasKey) {
                //开门失败
                return ActionResult.FAIL;
            }
            //执行开门逻辑
            if (!world.isClient) {
                state = state.cycle(OPEN);
                world.setBlockState(pos, state, 10);
            }
            return ActionResult.SUCCESS;
        }
    };

    public static void onInitialize() {
        LOGGER.info("[Nanbin Create Mod] [...] Adding PlatformDoor");

        Registry.register(Registry.BLOCK, new Identifier("nanbin","platform_door") , PLATFORM_DOOR);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "platform_door"), new BlockItem(PLATFORM_DOOR, new Item.Settings().group(ItemsGroup.USING_STATION_BUILDING_BLOCKS)));

        LOGGER.info("[Nanbin Create Mod] [√] PlatformDoor had been added!");
    }
}