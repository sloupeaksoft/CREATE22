package com.rustcrates.network;

import com.rustcrates.RustCrates;
import com.rustcrates.data.CrateData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper NET = NetworkRegistry.INSTANCE.newSimpleChannel(RustCrates.MODID);

    public static void init() {
        NET.registerMessage(MsgPanelData.Handler.class, MsgPanelData.class, 0, Side.CLIENT);
        NET.registerMessage(MsgPanelAction.Handler.class, MsgPanelAction.class, 1, Side.SERVER);
    }

    public static void sendPanel(EntityPlayerMP player, CrateData.Group group, boolean open) {
        NET.sendTo(MsgPanelData.build(CrateData.get(), group, open), player);
    }
}
