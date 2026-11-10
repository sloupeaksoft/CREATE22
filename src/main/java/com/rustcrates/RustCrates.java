package com.rustcrates;

import com.rustcrates.network.PacketHandler;
import com.rustcrates.proxy.CommonProxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = RustCrates.MODID, name = RustCrates.NAME, version = RustCrates.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class RustCrates {
    public static final String MODID = "rustcrates";
    public static final String NAME = "Rust Crates";
    public static final String VERSION = "1.1.0";

    @Mod.Instance(MODID)
    public static RustCrates instance;

    @SidedProxy(clientSide = "com.rustcrates.proxy.ClientProxy", serverSide = "com.rustcrates.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static final CreativeTabs TAB = new CreativeTabs(MODID) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(ModBlocks.MILITARY);
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PacketHandler.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }
}
