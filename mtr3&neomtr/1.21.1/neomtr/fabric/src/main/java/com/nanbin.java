package com;

import com.Blocks.command_block;
import com.Blocks.crt_platform;
import com.Items.Emetic;
import com.Items.Track;
import mtr.Items;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class nanbin implements ModInitializer {

    //通用ID声明
    public static final String MOD_ID = "nanbin";

   //物品栏的有关声明以及引用
    public static final ItemGroup USING_RAILWAY_BUILD = FabricItemGroup.builder()
            .icon(() -> new ItemStack(crt_platform.CRT_PLATFORM))
            .displayName(Text.translatable("itemGroup.nanbin.using_railway_build"))
            .entries((context, entries) -> {
                entries.add(Items.BRUSH.get()); // 引用刷子（虽然我也不知道有什么用，但应该可以方便一点）
                entries.add(crt_platform.CRT_PLATFORM);
                entries.add(Track.TRASH);
                entries.add(Emetic.EMETIC);
                entries.add(command_block.CRT_OLD_WALL1);
                entries.add(command_block.CRT_OLD_WALL2);
                entries.add(command_block.NANBIN_WHITE_BLOCK);
                entries.add(command_block.NANBIN_RED_BLOCK);
                entries.add(command_block.NANBIN_YELLOW_BLOCK);
                entries.add(command_block.NANBIN_GREEN_BLOCK);
                entries.add(command_block.NANBIN_BLUE_BLOCK);
                entries.add(command_block.NANBIN_PURPLE_BLOCK);
                entries.add(command_block.NANBIN_PINK_BLOCK);
                entries.add(command_block.LIGHT_RED_BLOCK);
                entries.add(command_block.LIGHT_YELLOW_BLOCK);
                entries.add(command_block.LIGHT_GREEN_BLOCK);
                entries.add(command_block.LIGHT_BLUE_BLOCK);
                entries.add(command_block.LIGHT_PURPLE_BLOCK);
                entries.add(command_block.LIGHT_PINK_BLOCK);
                entries.add(command_block.CRT_GRADIENT_RED_WALL_BLOCK);
                entries.add(command_block.CRT_GRADIENT_YELLOW_WALL_BLOCK);
                entries.add(command_block.CRT_GRADIENT_GREEN_WALL_BLOCK);
                entries.add(command_block.CRT_GRADIENT_BLUE_WALL_BLOCK);
                entries.add(command_block.CRT_GRADIENT_PURPLE_WALL_BLOCK);
                entries.add(command_block.CRT_GRADIENT_PINK_WALL_BLOCK);
                entries.add(command_block.CRT_RED_WALL_BLOCK);
                entries.add(command_block.CRT_YELLOW_WALL_BLOCK);
                entries.add(command_block.CRT_GREEN_WALL_BLOCK);
                entries.add(command_block.CRT_BLUE_WALL_BLOCK);
                entries.add(command_block.CRT_PURPLE_WALL_BLOCK);
                entries.add(command_block.CRT_PINK_WALL_BLOCK);
                entries.add(command_block.WHITE_MARBLE);
                entries.add(command_block.BLACK_MARBLE);
                entries.add(command_block.CEMENT);
            })
            .build();


    @Override
    public void onInitialize() {
        //注册物品栏
        Registry.register(Registries.ITEM_GROUP, Identifier.of("nanbin","using_railway_build"),USING_RAILWAY_BUILD);

        //导入物品
        Emetic.initialize();
        Track.initialize();
        crt_platform.initialize();
        command_block.initialize();
    }
}
