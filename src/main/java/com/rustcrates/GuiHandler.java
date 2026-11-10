package com.rustcrates;

import com.rustcrates.block.BlockCrate;
import com.rustcrates.client.GuiCrate;
import com.rustcrates.data.CrateData;
import com.rustcrates.inventory.ContainerCrate;
import com.rustcrates.tile.TileCrate;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.items.ItemStackHandler;

public class GuiHandler implements IGuiHandler {
    public static final int GUI_CRATE = 0;
    /** Редактирование предметов лута ADMIN-ящика; x, y, z — позиция этого ADMIN-ящика. */
    public static final int GUI_POOL = 10;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        final BlockPos pos = new BlockPos(x, y, z);
        if (id == GUI_CRATE) {
            final TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileCrate) {
                return new ContainerCrate(player.inventory, ((TileCrate) te).inventory,
                        p -> !te.isInvalid() && p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64.0,
                        te::markDirty);
            }
        } else if (id == GUI_POOL) {
            Block b = world.getBlockState(pos).getBlock();
            if (b instanceof BlockCrate && ((BlockCrate) b).admin) {
                final CrateData data = CrateData.get();
                CrateData.Group g = data.getOrCreateGroup(((BlockCrate) b).tier, world.provider.getDimension(), pos);
                return new ContainerCrate(player.inventory, g.pool, AdminUtil::isAdmin, data::markDirty);
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        ContainerCrate c = new ContainerCrate(player.inventory, new ItemStackHandler(ContainerCrate.SIZE), p -> true, () -> {});
        if (id == GUI_CRATE) {
            String title = world.getBlockState(pos).getBlock().getLocalizedName();
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileCrate && !((TileCrate) te).getCrateName().isEmpty()) {
                title = ((TileCrate) te).getCrateName();
            }
            return new GuiCrate(c, player.inventory, title);
        } else if (id == GUI_POOL) {
            Block b = world.getBlockState(pos).getBlock();
            String title = b instanceof BlockCrate ? "Пул лута: " + ((BlockCrate) b).tier.title : "Пул лута";
            return new GuiCrate(c, player.inventory, title);
        }
        return null;
    }
}
