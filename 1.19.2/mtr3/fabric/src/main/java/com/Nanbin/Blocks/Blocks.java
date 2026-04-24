package com.Nanbin.Blocks;

import com.Nanbin.ItemsGroup.ItemsGroup;
import com.Nanbin.Registry.RegBlock.*;
import com.Nanbin.Registry.RegBlock.SoundproofNet.BlockSoundproofNet;
import com.Nanbin.Registry.RegBlock.SoundproofNet.BlockSoundproofNetBase;
import com.Nanbin.Registry.RegBlock.SoundproofNet.BlockSoundproofNetMiddle;
import com.Nanbin.Registry.RegBlock.SoundproofNet.BlockSoundproofNetTop;
import com.Nanbin.Registry.RegBlock.TallFence.*;
import mtr.block.BlockPlatform;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.Nanbin.nanbin.LOGGER;


public class Blocks {
    public static final Block CRT_LOGO = new BlockCRTLogo(FabricBlockSettings.of(Material.GLASS).hardness(16.0f));
    public static final Block LOGO = new BlockMTRLogo(FabricBlockSettings.of(Material.GLASS).hardness(1.0f).nonOpaque());
    public static final Block CRT_OLD_WALL1 = new Block(FabricBlockSettings.of(Material.GLASS).hardness(16.0f));
    public static final Block CRT_OLD_WALL2 = new Block(FabricBlockSettings.of(Material.GLASS).hardness(16.0f));
    public static final Block CRT_PLATFORM = new BlockPlatform(FabricBlockSettings.of(Material.METAL),true);
    public static final Block CRT_TICKET_1_ENTER = new BlockCrtTicket(true);
    public static final Block CRT_TICKET_1_EXIT = new BlockCrtTicket(false);
    public static final Block CRT_TICKET_2_ENTER = new BlockCrtTicket(true);
    public static final Block CRT_TICKET_2_EXIT = new BlockCrtTicket(false);
    public static final Block CRT_LIFT_TIPS = new BlockMTRLogo(FabricBlockSettings.of(Material.GLASS).hardness(16.0f));
    public static final Block CRT_FENCE1 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE2 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE3 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE4 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE5 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE6 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE7 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE8 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE9 = new BlockCRTFence(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block CRT_FENCE10 = new BlockCRTFenceVertical(FabricBlockSettings.of(Material.GLASS).hardness(16.0f).nonOpaque());
    public static final Block SOUNDPROOFNET = new BlockSoundproofNet();
    public static final Block SOUNDPROOFNET_TOP = new BlockSoundproofNetTop();
    public static final Block SOUNDPROOFNET_GLASS = new BlockSoundproofNetBase();
    public static final Block SOUNDPROOFNET_BASE = new BlockSoundproofNetBase();
    public static final Block SOUNDPROOFNET_UP = new BlockSoundproofNetMiddle(FabricBlockSettings.of(Material.METAL));
    public static final Block PSD_TOP = new BlockPSDTop(FabricBlockSettings.of(Material.METAL));
    public static final Block BEHAVIORALBLOCK = new BlockBehavioral(FabricBlockSettings.of(Material.METAL));
    public static final Block BEHAVIORALBLOCK_ = new BlockBehavioralVertical(FabricBlockSettings.of(Material.METAL));
    public static final Block CEILING_LIGHT = new BlockCeilingLight(FabricBlockSettings.of(Material.GLASS).hardness(16.0f));
    public static final Block CEILING = new BlockCeiling(FabricBlockSettings.of(Material.GLASS).hardness(16.0f));
    public static final Block GREENFENCE = new BlockGreenFence();
    public static final Block GREENFENCE_TOP = new BlockGreenFenceTop();
    public static final Block BLUEFENCE = new BlockBlueFence();
    public static final Block BLUEFENCE_TOP = new BlockBlueFenceTop();
    public static final Block METALFENCE = new BlockMetalFence();
    public static final Block METALFENCE_TOP = new BlockMetalFenceTop();
    public static final Block NANBIN_WHITE_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block NANBIN_RED_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block NANBIN_YELLOW_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block NANBIN_GREEN_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block NANBIN_BLUE_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block NANBIN_PURPLE_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block NANBIN_PINK_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block LIGHT_RED_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block LIGHT_YELLOW_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block LIGHT_GREEN_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block LIGHT_BLUE_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block LIGHT_PURPLE_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block LIGHT_PINK_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_RED_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_YELLOW_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_GREEN_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_BLUE_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_PURPLE_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_PINK_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_GRADIENT_RED_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_GRADIENT_YELLOW_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_GRADIENT_GREEN_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_GRADIENT_BLUE_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_GRADIENT_PURPLE_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block CRT_GRADIENT_PINK_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.METAL).hardness(16.0f));
    public static final Block BLACK_MARBLE = new Block(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block WHITE_MARBLE = new Block(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block CEMENT = new Block(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block PAVEMENT_1 = new Block(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block PAVEMENT_1_HALF = new SlabBlock(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block PAVEMENT_2 = new Block(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block PAVEMENT_2_HALF = new SlabBlock(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block PAVEMENT_3 = new Block(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block PAVEMENT_3_HALF = new SlabBlock(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block PAVEMENT_4 = new Block(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block PAVEMENT_4_HALF = new SlabBlock(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block DRAIN_COVER = new SlabBlock(FabricBlockSettings.of(Material.STONE).hardness(1.0f));
    public static final Block GRAY_TACTILE_BAVING = new BlockFacingBlock(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block GRAY_TACTILE_BAVING_HALF = new BlockFacingSlabBlock(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block GRAY_TACTILE_BAVING_CONNECT = new Block(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block GRAY_TACTILE_BAVING_CONNECT_HALF = new SlabBlock(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block YELLOW_TACTILE_BAVING = new BlockFacingBlock(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block YELLOW_TACTILE_BAVING_HALF = new BlockFacingSlabBlock(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block YELLOW_TACTILE_BAVING_CONNECT = new Block(FabricBlockSettings.of(Material.STONE).hardness(16.0f));
    public static final Block YELLOW_TACTILE_BAVING_CONNECT_HALF = new SlabBlock(FabricBlockSettings.of(Material.STONE).hardness(16.0f));

    public static void onInitialize() {
        LOGGER.info("[Nanbin Create Mod] Registering Blocks...");

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_logo"), CRT_LOGO);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_logo"), new BlockItem(CRT_LOGO, new Item.Settings().group(ItemsGroup.CRT)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "logo"), LOGO);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "logo"), new BlockItem(LOGO, new Item.Settings().group(ItemsGroup.USING_STATION_BUILDING_BLOCKS)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_old_wall_1"), CRT_OLD_WALL1);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_old_wall_1"), new BlockItem(CRT_OLD_WALL1, new Item.Settings().group(ItemsGroup.CRT)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_old_wall_2"), CRT_OLD_WALL2);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_old_wall_2"), new BlockItem(CRT_OLD_WALL2, new Item.Settings().group(ItemsGroup.CRT)));

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_platform"), new BlockItem(CRT_PLATFORM, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_platform"), CRT_PLATFORM);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_barrier_entrance_1"), new BlockItem(CRT_TICKET_1_ENTER, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_barrier_entrance_1"), CRT_TICKET_1_ENTER);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_barrier_exit_1"), new BlockItem(CRT_TICKET_1_EXIT, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_barrier_exit_1"), CRT_TICKET_1_EXIT);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_barrier_entrance_2"), new BlockItem(CRT_TICKET_2_ENTER, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_barrier_entrance_2"), CRT_TICKET_2_ENTER);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_barrier_exit_2"), new BlockItem(CRT_TICKET_2_EXIT, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_barrier_exit_2"), CRT_TICKET_2_EXIT);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_lift_tips"), new BlockItem(CRT_LIFT_TIPS, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_lift_tips"), CRT_LIFT_TIPS);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_1"), new BlockItem(CRT_FENCE1, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_1"), CRT_FENCE1);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_2"), new BlockItem(CRT_FENCE2, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_2"), CRT_FENCE2);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_3"), new BlockItem(CRT_FENCE3, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_3"), CRT_FENCE3);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_4"), new BlockItem(CRT_FENCE4, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_4"), CRT_FENCE4);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_5"), new BlockItem(CRT_FENCE5, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_5"), CRT_FENCE5);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_6"), new BlockItem(CRT_FENCE6, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_6"), CRT_FENCE6);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_7"), new BlockItem(CRT_FENCE7, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_7"), CRT_FENCE7);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_8"), new BlockItem(CRT_FENCE8, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_8"), CRT_FENCE8);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_9"), new BlockItem(CRT_FENCE9, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_9"), CRT_FENCE9);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_fence_10"), new BlockItem(CRT_FENCE10, new Item.Settings().group(ItemsGroup.CRT)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_fence_10"), CRT_FENCE10);

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "soundproofnet"), SOUNDPROOFNET);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "soundproofnet"), new BlockItem(SOUNDPROOFNET, new Item.Settings().group(ItemsGroup.USING_RAILWAY_BUILD)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "soundproofnet_up"), SOUNDPROOFNET_UP);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "soundproofnet_up"), new BlockItem(SOUNDPROOFNET_UP, new Item.Settings().group(ItemsGroup.USING_RAILWAY_BUILD)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "soundproofnet_top"), SOUNDPROOFNET_TOP);
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "soundproofnet_base"), SOUNDPROOFNET_BASE);
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "soundproofnet_glass"), SOUNDPROOFNET_GLASS);

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "behavioral_block"), BEHAVIORALBLOCK);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "behavioral_block"), new BlockItem(BEHAVIORALBLOCK, new Item.Settings().group(ItemsGroup.USING_RAILWAY_BUILD)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "behavioral_block_90"), BEHAVIORALBLOCK_);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "behavioral_block_90"), new BlockItem(BEHAVIORALBLOCK_, new Item.Settings().group(ItemsGroup.USING_RAILWAY_BUILD)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "psd_top"), PSD_TOP);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "psd_top"), new BlockItem(PSD_TOP, new Item.Settings().group(ItemsGroup.USING_STATION_BUILDING_BLOCKS)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "ceiling_light"), CEILING_LIGHT);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "ceiling_light"), new BlockItem(CEILING_LIGHT, new Item.Settings().group(ItemsGroup.USING_STATION_BUILDING_BLOCKS)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "ceiling"), CEILING);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "ceiling"), new BlockItem(CEILING, new Item.Settings().group(ItemsGroup.USING_STATION_BUILDING_BLOCKS)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "greenfence_top"), GREENFENCE_TOP);

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "bluefence_top"), BLUEFENCE_TOP);

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "metalfence_top"), METALFENCE_TOP);

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "greenfence"), GREENFENCE);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "greenfence"), new BlockItem(GREENFENCE, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "bluefence"), BLUEFENCE);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "bluefence"), new BlockItem(BLUEFENCE, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));

        Registry.register(Registry.BLOCK, new Identifier("nanbin", "metalfence"), METALFENCE);
        Registry.register(Registry.ITEM, new Identifier("nanbin", "metalfence"), new BlockItem(METALFENCE, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));

        Registry.register(Registry.ITEM, new Identifier("nanbin", "white_block"), new BlockItem(NANBIN_WHITE_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "white_block"), NANBIN_WHITE_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "red_block"), new BlockItem(NANBIN_RED_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "red_block"), NANBIN_RED_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "yellow_block"), new BlockItem(NANBIN_YELLOW_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "yellow_block"), NANBIN_YELLOW_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "green_block"), new BlockItem(NANBIN_GREEN_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "green_block"), NANBIN_GREEN_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "blue_block"), new BlockItem(NANBIN_BLUE_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "blue_block"), NANBIN_BLUE_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "purple_block"), new BlockItem(NANBIN_PURPLE_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "purple_block"), NANBIN_PURPLE_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pink_block"), new BlockItem(NANBIN_PINK_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pink_block"), NANBIN_PINK_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "light_red_block"), new BlockItem(LIGHT_RED_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "light_red_block"), LIGHT_RED_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "light_yellow_block"), new BlockItem(LIGHT_YELLOW_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "light_yellow_block"), LIGHT_YELLOW_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "light_green_block"), new BlockItem(LIGHT_GREEN_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "light_green_block"), LIGHT_GREEN_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "light_blue_block"), new BlockItem(LIGHT_BLUE_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "light_blue_block"), LIGHT_BLUE_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "light_purple_block"), new BlockItem(LIGHT_PURPLE_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "light_purple_block"), LIGHT_PURPLE_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "light_pink_block"), new BlockItem(LIGHT_PINK_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "light_pink_block"), LIGHT_PINK_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_red_wall_block"), new BlockItem(CRT_RED_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_red_wall_block"), CRT_RED_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_yellow_wall_block"), new BlockItem(CRT_YELLOW_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_yellow_wall_block"), CRT_YELLOW_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_green_wall_block"), new BlockItem(CRT_GREEN_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_green_wall_block"), CRT_GREEN_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_blue_wall_block"), new BlockItem(CRT_BLUE_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_blue_wall_block"), CRT_BLUE_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_purple_wall_block"), new BlockItem(CRT_PURPLE_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_purple_wall_block"), CRT_PURPLE_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_pink_wall_block"), new BlockItem(CRT_PINK_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_pink_wall_block"), CRT_PINK_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_gradient_red_wall_block"), new BlockItem(CRT_GRADIENT_RED_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_gradient_red_wall_block"), CRT_GRADIENT_RED_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_gradient_yellow_wall_block"), new BlockItem(CRT_GRADIENT_YELLOW_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_gradient_yellow_wall_block"), CRT_GRADIENT_YELLOW_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_gradient_green_wall_block"), new BlockItem(CRT_GRADIENT_GREEN_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_gradient_green_wall_block"), CRT_GRADIENT_GREEN_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_gradient_blue_wall_block"), new BlockItem(CRT_GRADIENT_BLUE_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_gradient_blue_wall_block"), CRT_GRADIENT_BLUE_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_gradient_purple_wall_block"), new BlockItem(CRT_GRADIENT_PURPLE_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_gradient_purple_wall_block"), CRT_GRADIENT_PURPLE_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "crt_gradient_pink_wall_block"), new BlockItem(CRT_GRADIENT_PINK_WALL_BLOCK, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "crt_gradient_pink_wall_block"), CRT_GRADIENT_PINK_WALL_BLOCK);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "black_marble"), new BlockItem(BLACK_MARBLE, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "black_marble"), BLACK_MARBLE);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "white_marble"), new BlockItem(WHITE_MARBLE, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "white_marble"), WHITE_MARBLE);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "cement"), new BlockItem(CEMENT, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "cement"), CEMENT);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pavement_1"), new BlockItem(PAVEMENT_1, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pavement_1"), PAVEMENT_1);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pavement_1_half"), new BlockItem(PAVEMENT_1_HALF, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pavement_1_half"), PAVEMENT_1_HALF);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pavement_2"), new BlockItem(PAVEMENT_2, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pavement_2"), PAVEMENT_2);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pavement_2_half"), new BlockItem(PAVEMENT_2_HALF, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pavement_2_half"), PAVEMENT_2_HALF);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pavement_3"), new BlockItem(PAVEMENT_3, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pavement_3"), PAVEMENT_3);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pavement_3_half"), new BlockItem(PAVEMENT_3_HALF, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pavement_3_half"), PAVEMENT_3_HALF);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pavement_4"), new BlockItem(PAVEMENT_4, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pavement_4"), PAVEMENT_4);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "pavement_4_half"), new BlockItem(PAVEMENT_4_HALF, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "pavement_4_half"), PAVEMENT_4_HALF);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "drain_cover"), new BlockItem(DRAIN_COVER, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "drain_cover"), DRAIN_COVER);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "gray_tactile_paving"), new BlockItem(GRAY_TACTILE_BAVING, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "gray_tactile_paving"), GRAY_TACTILE_BAVING);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "gray_tactile_paving_half"), new BlockItem(GRAY_TACTILE_BAVING_HALF, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "gray_tactile_paving_half"), GRAY_TACTILE_BAVING_HALF);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "gray_tactile_paving_connect"), new BlockItem(GRAY_TACTILE_BAVING_CONNECT, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "gray_tactile_paving_connect"), GRAY_TACTILE_BAVING_CONNECT);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "gray_tactile_paving_connect_half"), new BlockItem(GRAY_TACTILE_BAVING_CONNECT_HALF, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "gray_tactile_paving_connect_half"), GRAY_TACTILE_BAVING_CONNECT_HALF);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "yellow_tactile_paving"), new BlockItem(YELLOW_TACTILE_BAVING, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "yellow_tactile_paving"), YELLOW_TACTILE_BAVING);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "yellow_tactile_paving_half"), new BlockItem(YELLOW_TACTILE_BAVING_HALF, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "yellow_tactile_paving_half"), YELLOW_TACTILE_BAVING_HALF);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "yellow_tactile_paving_connect"), new BlockItem(YELLOW_TACTILE_BAVING_CONNECT, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "yellow_tactile_paving_connect"), YELLOW_TACTILE_BAVING_CONNECT);

        Registry.register(Registry.ITEM, new Identifier("nanbin", "yellow_tactile_paving_connect_half"), new BlockItem(YELLOW_TACTILE_BAVING_CONNECT_HALF, new Item.Settings().group(ItemsGroup.CITY_BUILDING_BLOCKS)));
        Registry.register(Registry.BLOCK, new Identifier("nanbin", "yellow_tactile_paving_connect_half"), YELLOW_TACTILE_BAVING_CONNECT_HALF);

    }
}

