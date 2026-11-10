package com.rustcrates;

import com.rustcrates.block.BlockCrate;
import net.minecraft.block.Block;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = RustCrates.MODID)
public class CommonEvents {
    /** Чтобы Shift + ПКМ по ADMIN-ящику работал даже с предметом в руке. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Block b = event.getWorld().getBlockState(event.getPos()).getBlock();
        if (b instanceof BlockCrate && ((BlockCrate) b).admin && event.getEntityPlayer().isSneaking()) {
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
