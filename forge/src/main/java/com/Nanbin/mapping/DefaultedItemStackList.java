package com.Nanbin.mapping;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.mtr.mapping.annotation.MappedMethod;
import org.mtr.mapping.tool.HolderBase;

/**
 * @author ZiYueCommentary
 * @since 1.0.0-beta-2
 */

public class DefaultedItemStackList extends HolderBase<NonNullList<ItemStack>>
{
    public DefaultedItemStackList(DefaultedItemStackList data) {
        super(data.data);
    }

    public DefaultedItemStackList(NonNullList<ItemStack> data) {
        super(data);
    }

    public static DefaultedItemStackList ofSize(int size) {
        return new DefaultedItemStackList(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    @MappedMethod
    public int size() {
        return this.data.size();
    }

    @MappedMethod
    public void set(int i, ItemStack stack) {
        this.data.set(i, stack);
    }

    @MappedMethod
    public ItemStack get(int i) {
        return this.data.get(i);
    }
}
