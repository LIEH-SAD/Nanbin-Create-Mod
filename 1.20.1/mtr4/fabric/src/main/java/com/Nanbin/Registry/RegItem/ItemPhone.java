package com.Nanbin.Registry.RegItem;

import com.Nanbin.Registry.RegMean.TicketMachineHelper;
import org.mtr.mapping.holder.Hand;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.ItemExtension;


public class ItemPhone extends ItemExtension {

    public ItemPhone(ItemSettings itemSettings) {
        super(itemSettings.maxCount(1));
    }

    @Override
    public void useWithoutResult( World world,  PlayerEntity user,  Hand hand) {

        TicketMachineHelper.openTicketMachineScreen(world, user);
    }

}