package com.rustcrates.proxy;

import com.rustcrates.client.GuiAdminPanel;
import com.rustcrates.network.MsgPanelData;
import net.minecraft.client.Minecraft;

public class ClientProxy extends CommonProxy {
    @Override
    public void handlePanelData(final MsgPanelData message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof GuiAdminPanel && ((GuiAdminPanel) mc.currentScreen).admin() == message.admin) {
                ((GuiAdminPanel) mc.currentScreen).setData(message);
            } else if (message.open) {
                mc.displayGuiScreen(new GuiAdminPanel(message));
            }
        });
    }
}
