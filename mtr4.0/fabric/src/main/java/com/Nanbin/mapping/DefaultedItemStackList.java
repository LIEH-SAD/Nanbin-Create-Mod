package com.Nanbin.mapping;

import org.mtr.mapping.annotation.MappedMethod;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.tool.HolderBase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZiYueCommentary
 * @since 1.0.0-beta-2
 */

public class DefaultedItemStackList extends HolderBase<List<ItemStack>>
{
    public DefaultedItemStackList(DefaultedItemStackList data) {
        super(data.data);
    }

    public DefaultedItemStackList(List<ItemStack> data) {
        super(data);
    }

    public static DefaultedItemStackList ofSize(int size) {
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new ItemStack(net.minecraft.item.ItemStack.EMPTY));
        }
        return new DefaultedItemStackList(list);
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