package com.rustcrates.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.function.Predicate;

/** 27 слотов — как в обычном сундуке. */
public class ContainerCrate extends Container {
    public static final int ROWS = 3;
    public static final int SIZE = ROWS * 9;

    private final Predicate<EntityPlayer> canUse;
    private final Runnable onClose;

    public ContainerCrate(InventoryPlayer playerInv, IItemHandler inv, Predicate<EntityPlayer> canUse, Runnable onClose) {
        this.canUse = canUse;
        this.onClose = onClose;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < 9; c++)
                addSlotToContainer(new SlotItemHandler(inv, c + r * 9, 8 + c * 18, 18 + r * 18));
        int off = (ROWS - 4) * 18;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlotToContainer(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 103 + r * 18 + off));
        for (int c = 0; c < 9; c++)
            addSlotToContainer(new Slot(playerInv, c, 8 + c * 18, 161 + off));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return canUse.test(player);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        onClose.run();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < SIZE) {
                if (!mergeItemStack(stack, SIZE, inventorySlots.size(), true)) return ItemStack.EMPTY;
            } else if (!mergeItemStack(stack, 0, SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
        }
        return result;
    }
}
