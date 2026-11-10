package com.rustcrates.client;

import com.rustcrates.ModBlocks;
import com.rustcrates.RustCrates;
import com.rustcrates.block.BlockCrate;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = RustCrates.MODID)
public class ClientModels {
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (BlockCrate b : ModBlocks.ALL) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(b), 0,
                    new ModelResourceLocation(b.getRegistryName(), "inventory"));
        }
    }
}
