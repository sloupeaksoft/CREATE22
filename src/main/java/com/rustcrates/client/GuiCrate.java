package com.rustcrates.client;

import com.rustcrates.inventory.ContainerCrate;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiCrate extends GuiContainer {
    private static final ResourceLocation TEX = new ResourceLocation("textures/gui/container/generic_54.png");
    private final String title;
    private final InventoryPlayer playerInv;

    public GuiCrate(ContainerCrate container, InventoryPlayer playerInv, String title) {
        super(container);
        this.title = title;
        this.playerInv = playerInv;
        this.ySize = 114 + ContainerCrate.ROWS * 18;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(title, 8, 6, 0x404040);
        fontRenderer.drawString(playerInv.getDisplayName().getUnformattedText(), 8, ySize - 96 + 2, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEX);
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        int rows = ContainerCrate.ROWS;
        drawTexturedModalRect(i, j, 0, 0, xSize, rows * 18 + 17);
        drawTexturedModalRect(i, j + rows * 18 + 17, 0, 126, xSize, 96);
    }
}
