package com.rustcrates;

import com.rustcrates.block.BlockCrate;
import com.rustcrates.data.CrateData;
import com.rustcrates.tile.TileCrate;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = RustCrates.MODID)
public class LootSpawner {
    private static final Random RAND = new Random();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (DimensionManager.getWorld(0) == null) return;
        CrateData data = CrateData.get();
        for (CrateData.Group g : new ArrayList<>(data.groups)) {
            if (!g.enabled) continue;
            g.ticks++;
            if (g.ticks >= Math.max(1, g.interval) * 20) {
                g.ticks = 0;
                // ADMIN-ящик исчез (например, удалён командой) — убираем его группу.
                World aw = DimensionManager.getWorld(g.dim);
                if (aw != null && aw.isBlockLoaded(g.pos)) {
                    Block ab = aw.getBlockState(g.pos).getBlock();
                    if (!(ab instanceof BlockCrate) || !((BlockCrate) ab).admin || ((BlockCrate) ab).tier != g.tier) {
                        data.onAdminRemoved(g.dim, g.pos);
                        continue;
                    }
                }
                spawn(data, g);
            }
        }
    }

    /** Спавнит лут во всех выбранных ящиках ADMIN-ящика. Возвращает число заполненных ящиков. */
    public static int spawn(CrateData data, CrateData.Group c) {
        int filled = 0;
        Iterator<CrateData.CrateEntry> it = c.crates.iterator();
        while (it.hasNext()) {
            CrateData.CrateEntry e = it.next();
            if (!e.selected) continue;
            World w = DimensionManager.getWorld(e.dim);
            if (w == null) continue;
            Block b = w.getBlockState(e.pos).getBlock();
            TileEntity te = w.getTileEntity(e.pos);
            if (!(b instanceof BlockCrate) || ((BlockCrate) b).admin || ((BlockCrate) b).tier != c.tier || !(te instanceof TileCrate)) {
                it.remove();
                data.markDirty();
                continue;
            }
            fill((TileCrate) te, c);
            filled++;
        }
        return filled;
    }

    private static void fill(TileCrate te, CrateData.Group c) {
        ItemStackHandler inv = te.inventory;
        if (c.clearBefore) {
            for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, ItemStack.EMPTY);
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < c.pool.getSlots(); i++) {
            if (!c.pool.getStackInSlot(i).isEmpty()) order.add(i);
        }
        Collections.shuffle(order, RAND);
        int placed = 0;
        for (int s : order) {
            if (c.maxItems > 0 && placed >= c.maxItems) break;
            if (RAND.nextInt(100) >= c.chances[s]) continue;
            ItemStack src = c.pool.getStackInSlot(s);
            ItemStack out = src.copy();
            out.setCount(c.randomCount ? 1 + RAND.nextInt(Math.max(1, src.getCount())) : src.getCount());
            List<Integer> empty = new ArrayList<>();
            for (int i = 0; i < inv.getSlots(); i++) if (inv.getStackInSlot(i).isEmpty()) empty.add(i);
            if (empty.isEmpty()) break;
            inv.setStackInSlot(empty.get(RAND.nextInt(empty.size())), out);
            placed++;
        }
        te.markDirty();
    }
}
