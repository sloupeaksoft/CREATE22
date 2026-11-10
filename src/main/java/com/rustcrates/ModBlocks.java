package com.rustcrates;

import com.rustcrates.block.BlockCrate;
import com.rustcrates.tile.TileCrate;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = RustCrates.MODID)
public class ModBlocks {
    public static BlockCrate NORMAL, ELITE, MILITARY;
    public static BlockCrate ADMIN_NORMAL, ADMIN_ELITE, ADMIN_MILITARY;
    public static final List<BlockCrate> ALL = new ArrayList<>();

    private static BlockCrate add(BlockCrate b) {
        ALL.add(b);
        return b;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ALL.clear();
        NORMAL = add(new BlockCrate(CrateTier.NORMAL, false));
        ELITE = add(new BlockCrate(CrateTier.ELITE, false));
        MILITARY = add(new BlockCrate(CrateTier.MILITARY, false));
        ADMIN_NORMAL = add(new BlockCrate(CrateTier.NORMAL, true));
        ADMIN_ELITE = add(new BlockCrate(CrateTier.ELITE, true));
        ADMIN_MILITARY = add(new BlockCrate(CrateTier.MILITARY, true));
        for (BlockCrate b : ALL) event.getRegistry().register(b);
        GameRegistry.registerTileEntity(TileCrate.class, new ResourceLocation(RustCrates.MODID, "crate"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (BlockCrate b : ALL) {
            Item item = new ItemBlock(b).setRegistryName(b.getRegistryName());
            event.getRegistry().register(item);
        }
    }
}
