package com.Nanbin.Blocks;

import com.Nanbin.Registry.Init;
import com.Nanbin.Registry.RegBlock.BlockCRTTicketBarrier1;
import com.Nanbin.Registry.RegBlock.BlockCRTTicketBarrier2;
import com.Nanbin.Registry.RegBlock.BlockCRTTicketMachine1;
import mtr.block.BlockGlassFence;
import mtr.block.BlockPlatform;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.Nanbin.Registry.Init.MOD_ID;


public class Blocks {
    // 方块注册器
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

    //快捷注册方块
    public static final DeferredBlock<Block>
//            CRT_LOGO = reg("crt_logo", BlockCRTLogo::new),
            CRT_PLATFORM = reg("crt_platform", () -> new BlockPlatform(BlockBehaviour.Properties.of(),true)),
            CRT_TICKET_1_ENTER = reg("crt_barrier_entrance_1", () -> new BlockCRTTicketBarrier1(true)),
            CRT_TICKET_1_EXIT = reg("crt_barrier_exit_1", () -> new BlockCRTTicketBarrier1(false)),
            CRT_TICKET_2_ENTER = reg("crt_barrier_entrance_2", () -> new BlockCRTTicketBarrier2(true)),
            CRT_TICKET_2_EXIT = reg("crt_barrier_exit_2", () -> new BlockCRTTicketBarrier2(false)),
            CRT_TICKET_MACHINE_1 = reg("crt_ticket_machine_1", () -> new BlockCRTTicketMachine1(BlockBehaviour.Properties.of())),
            CRT_OLD_WALL1 = reg("crt_old_wall_1", () -> new Block(BlockBehaviour.Properties.of())),
            CRT_OLD_WALL2 = reg("crt_old_wall_2", () -> new Block(BlockBehaviour.Properties.of())),
            CRT_FENCE1 = reg("crt_fence_1", BlockGlassFence::new),
            CRT_FENCE8 = reg("crt_fence_8", BlockGlassFence::new),
            CRT_FENCE9 = reg("crt_fence_9", BlockGlassFence::new),
//            CRT_LIFT_TIPS = reg("crt_lift_tips", BlockMTRLogo::new),
//            CRT_LIFT_TIPS_3 = reg("crt_lift_tips_3", BlockMTRLogo::new),
//            CRT_PSD_CAB_DOOR = reg("crt_psd_cab_door", BlockOrdinaryPSDCabDoor::new),
//            CRT_APG_CAB_DOOR_OLD = reg("crt_apg_cab_door_old", BlockOrdinaryPSDCabDoor::new),
//            CRT_APG_CAB_FENCE_OLD = reg("crt_apg_cab_fence_old", BlockCRTCabFence::new),
//            CRT_APG_CAB_FENCE_OLD_CONNECT = reg("crt_apg_cab_fence_old_connect", BlockCRTCabFenceConnect::new),
//            CRT_APG_CAB_DOOR_NEW = reg("crt_apg_cab_door_new", BlockOrdinaryPSDCabDoor::new),
//            BEHAVIORALBLOCK = reg("behavioral_block", BlockBehavioral::new),
//            BEHAVIORALBLOCK_ = reg("behavioral_block_90", BlockBehavioralVertical::new),
//            LOGO = reg("logo", BlockMTRLogo::new),
//            CEILING = reg("ceiling", BlockCeiling::new),
//            CEILING_OVERHEAD = reg("ceiling_overhead", BlockCeiling::new),
//            CEILING_LIGHT = reg("ceiling_light", BlockCeilingLight::new),
//            ORDINARY_PSD_CAB_DOOR = reg("ordinary_psd_cab_door", BlockOrdinaryPSDCabDoor::new),
//            PSD_TOP = reg("psd_top", BlockPSDTOP::new),
//            BLUEFENCE = reg("bluefence", BlockBlueFence::new),
//            BLUEFENCE_TOP = reg("bluefence_top", BlockBlueFenceTop::new),
//            GREENFENCE = reg("greenfence", BlockGreenFence::new),
//            GREENFENCE_TOP = reg("greenfence_top", BlockGreenFenceTop::new),
//            METALFENCE = reg("metalfence", BlockMetalFence::new),
//            METALFENCE_TOP = reg("metalfence_top", BlockMetalFenceTop::new),
            NANBIN_WHITE_BLOCK = reg("white_block", () -> new Block(BlockBehaviour.Properties.of())),
            NANBIN_RED_BLOCK = reg("red_block", () -> new Block(BlockBehaviour.Properties.of())),
            NANBIN_YELLOW_BLOCK = reg("yellow_block", () -> new Block(BlockBehaviour.Properties.of())),
            NANBIN_GREEN_BLOCK = reg("green_block", () -> new Block(BlockBehaviour.Properties.of())),
            NANBIN_BLUE_BLOCK = reg("blue_block", () -> new Block(BlockBehaviour.Properties.of())),
            NANBIN_PURPLE_BLOCK = reg("purple_block",  () -> new Block(BlockBehaviour.Properties.of())),
            NANBIN_PINK_BLOCK = reg("pink_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_RED_WALL_BLOCK = reg("crt_red_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_YELLOW_WALL_BLOCK = reg("crt_yellow_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_GREEN_WALL_BLOCK = reg("crt_green_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_BLUE_WALL_BLOCK = reg("crt_blue_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_PURPLE_WALL_BLOCK = reg("crt_purple_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_PINK_WALL_BLOCK = reg("crt_pink_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_GRADIENT_RED_WALL_BLOCK = reg("crt_gradient_red_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_GRADIENT_YELLOW_WALL_BLOCK = reg("crt_gradient_yellow_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_GRADIENT_GREEN_WALL_BLOCK = reg("crt_gradient_green_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_GRADIENT_BLUE_WALL_BLOCK = reg("crt_gradient_blue_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_GRADIENT_PURPLE_WALL_BLOCK = reg("crt_gradient_purple_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            CRT_GRADIENT_PINK_WALL_BLOCK = reg("crt_gradient_pink_wall_block",  () -> new Block(BlockBehaviour.Properties.of())),
            LIGHT_RED_BLOCK = reg("light_red_block",  () -> new Block(BlockBehaviour.Properties.of())),
            LIGHT_YELLOW_BLOCK = reg("light_yellow_block",  () -> new Block(BlockBehaviour.Properties.of())),
            LIGHT_GREEN_BLOCK = reg("light_green_block",  () -> new Block(BlockBehaviour.Properties.of())),
            LIGHT_BLUE_BLOCK = reg("light_blue_block",  () -> new Block(BlockBehaviour.Properties.of())),
            LIGHT_PURPLE_BLOCK = reg("light_purple_block",  () -> new Block(BlockBehaviour.Properties.of())),
            LIGHT_PINK_BLOCK = reg("light_pink_block",  () -> new Block(BlockBehaviour.Properties.of())),
            BLACK_MARBLE = reg("black_marble",  () -> new Block(BlockBehaviour.Properties.of())),
            WHITE_MARBLE = reg("white_marble",  () -> new Block(BlockBehaviour.Properties.of())),
            CEMENT = reg("cement",  () -> new Block(BlockBehaviour.Properties.of())),
            PAVEMENT_1 = reg("pavement_1",  () -> new Block(BlockBehaviour.Properties.of())),
            PAVEMENT_1_HALF = reg("pavement_1_half",  () -> new Block(BlockBehaviour.Properties.of())),
            PAVEMENT_2 = reg("pavement_2",  () -> new Block(BlockBehaviour.Properties.of())),
            PAVEMENT_2_HALF = reg("pavement_2_half",  () -> new Block(BlockBehaviour.Properties.of())),
            PAVEMENT_3 = reg("pavement_3",  () -> new Block(BlockBehaviour.Properties.of())),
            PAVEMENT_3_HALF = reg("pavement_3_half",  () -> new Block(BlockBehaviour.Properties.of())),
            PAVEMENT_4 = reg("pavement_4",  () -> new Block(BlockBehaviour.Properties.of())),
            PAVEMENT_4_HALF = reg("pavement_4_half",  () -> new Block(BlockBehaviour.Properties.of())),
            DRAIN_COVER = reg("drain_cover",  () -> new Block(BlockBehaviour.Properties.of())),
//            GRAY_TACTILE_BAVING = reg("gray_tactile_paving", BlockFacingBlock::new),
//            GRAY_TACTILE_BAVING_HALF = reg("gray_tactile_paving_half", BlockFacingSlabBlock::new),
            GRAY_TACTILE_BAVING_CONNECT = reg("gray_tactile_paving_connect",  () -> new Block(BlockBehaviour.Properties.of())),
            GRAY_TACTILE_BAVING_CONNECT_HALF = reg("gray_tactile_paving_connect_half", () -> new SlabBlock(BlockBehaviour.Properties.of())),
//            YELLOW_TACTILE_BAVING = reg("yellow_tactile_paving", BlockFacingBlock::new),
//            YELLOW_TACTILE_BAVING_HALF = reg("yellow_tactile_paving_half", BlockFacingSlabBlock::new),
            YELLOW_TACTILE_BAVING_CONNECT = reg("yellow_tactile_paving_connect",  () -> new Block(BlockBehaviour.Properties.of())),
            YELLOW_TACTILE_BAVING_CONNECT_HALF = reg("yellow_tactile_paving_connect_half",  () -> new SlabBlock(BlockBehaviour.Properties.of()));


    //同时注册物品
    private static <T extends Block> DeferredBlock<T> reg(String name, Supplier<T> blockSupplier){
        DeferredBlock<Block> block = BLOCKS.register(name, blockSupplier);
        Init.ITEM_REGISTER.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return (DeferredBlock<T>) block;
    }
}