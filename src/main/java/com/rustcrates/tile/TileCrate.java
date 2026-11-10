package com.rustcrates.tile;

import com.rustcrates.inventory.ContainerCrate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

public class TileCrate extends TileEntity {
    public final ItemStackHandler inventory = new ItemStackHandler(ContainerCrate.SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    /** Название ящика, заданное админом (пустое = стандартное). Синхронизируется с клиентом. */
    private String crateName = "";

    public String getCrateName() {
        return crateName;
    }

    public void setCrateName(String name) {
        crateName = name == null ? "" : name;
        markDirty();
        World w = getWorld();
        if (w != null && !w.isRemote) {
            IBlockState s = w.getBlockState(getPos());
            w.notifyBlockUpdate(getPos(), s, s, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        inventory.deserializeNBT(nbt.getCompoundTag("Items"));
        crateName = nbt.getString("CrateName");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setTag("Items", inventory.serializeNBT());
        nbt.setString("CrateName", crateName);
        return super.writeToNBT(nbt);
    }

    // Синхронизация только названия (предметы на клиент не отправляются).

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound t = super.writeToNBT(new NBTTagCompound());
        t.setString("CrateName", crateName);
        return t;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        crateName = tag.getString("CrateName");
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        crateName = pkt.getNbtCompound().getString("CrateName");
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
