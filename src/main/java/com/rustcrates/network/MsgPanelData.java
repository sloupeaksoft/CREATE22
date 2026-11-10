package com.rustcrates.network;

import com.rustcrates.CrateTier;
import com.rustcrates.RustCrates;
import com.rustcrates.block.BlockCrate;
import com.rustcrates.data.CrateData;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Сервер -> клиент: снимок настроек панели управления одного ADMIN-ящика. */
public class MsgPanelData implements IMessage {
    public boolean open;
    /** Позиция ADMIN-ящика (BlockPos.toLong). */
    public long admin;
    public int tier;
    public boolean locked;
    public boolean enabled, clearBefore, randomCount;
    public int interval, maxItems, secondsLeft;
    public final List<CrateInfo> crates = new ArrayList<>();
    public final List<CrateInfo> free = new ArrayList<>();
    public final List<LootInfo> loot = new ArrayList<>();

    public static class CrateInfo {
        public int dim;
        public BlockPos pos;
        public boolean selected;
        public boolean loaded;
        public String name = "";
    }

    public static class LootInfo {
        public int slot;
        public ItemStack stack;
        public int chance;
    }

    public MsgPanelData() {}

    private static void collect(CrateData data, CrateTier tier, List<CrateData.CrateEntry> src, List<CrateInfo> out) {
        Iterator<CrateData.CrateEntry> it = src.iterator();
        while (it.hasNext()) {
            CrateData.CrateEntry e = it.next();
            World w = DimensionManager.getWorld(e.dim);
            boolean loaded = w != null && w.isBlockLoaded(e.pos);
            if (loaded) {
                Block b = w.getBlockState(e.pos).getBlock();
                if (!(b instanceof BlockCrate) || ((BlockCrate) b).admin || ((BlockCrate) b).tier != tier) {
                    it.remove();
                    data.markDirty();
                    continue;
                }
            }
            CrateInfo ci = new CrateInfo();
            ci.dim = e.dim;
            ci.pos = e.pos;
            ci.selected = e.selected;
            ci.loaded = loaded;
            ci.name = e.name == null ? "" : e.name;
            out.add(ci);
        }
    }

    public static MsgPanelData build(CrateData data, CrateData.Group g, boolean open) {
        MsgPanelData m = new MsgPanelData();
        m.open = open;
        m.admin = g.pos.toLong();
        m.tier = g.tier.ordinal();
        m.locked = g.locked;
        m.enabled = g.enabled;
        m.clearBefore = g.clearBefore;
        m.randomCount = g.randomCount;
        m.interval = g.interval;
        m.maxItems = g.maxItems;
        m.secondsLeft = Math.max(0, (g.interval * 20 - g.ticks) / 20);
        collect(data, g.tier, g.crates, m.crates);
        collect(data, g.tier, data.free(g.tier), m.free);
        for (int i = 0; i < g.pool.getSlots(); i++) {
            ItemStack s = g.pool.getStackInSlot(i);
            if (s.isEmpty()) continue;
            LootInfo li = new LootInfo();
            li.slot = i;
            li.stack = s.copy();
            li.chance = g.chances[i];
            m.loot.add(li);
        }
        return m;
    }

    private static void writeCrates(ByteBuf buf, List<CrateInfo> list) {
        buf.writeInt(list.size());
        for (CrateInfo c : list) {
            buf.writeInt(c.dim);
            buf.writeLong(c.pos.toLong());
            buf.writeBoolean(c.selected);
            buf.writeBoolean(c.loaded);
            ByteBufUtils.writeUTF8String(buf, c.name == null ? "" : c.name);
        }
    }

    private static void readCrates(ByteBuf buf, List<CrateInfo> list) {
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            CrateInfo c = new CrateInfo();
            c.dim = buf.readInt();
            c.pos = BlockPos.fromLong(buf.readLong());
            c.selected = buf.readBoolean();
            c.loaded = buf.readBoolean();
            c.name = ByteBufUtils.readUTF8String(buf);
            list.add(c);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(open);
        buf.writeLong(admin);
        buf.writeByte(tier);
        buf.writeBoolean(locked);
        buf.writeBoolean(enabled);
        buf.writeBoolean(clearBefore);
        buf.writeBoolean(randomCount);
        buf.writeInt(interval);
        buf.writeInt(maxItems);
        buf.writeInt(secondsLeft);
        writeCrates(buf, crates);
        writeCrates(buf, free);
        buf.writeInt(loot.size());
        for (LootInfo l : loot) {
            buf.writeByte(l.slot);
            ByteBufUtils.writeItemStack(buf, l.stack);
            buf.writeByte(l.chance);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        open = buf.readBoolean();
        admin = buf.readLong();
        tier = buf.readByte();
        locked = buf.readBoolean();
        enabled = buf.readBoolean();
        clearBefore = buf.readBoolean();
        randomCount = buf.readBoolean();
        interval = buf.readInt();
        maxItems = buf.readInt();
        secondsLeft = buf.readInt();
        readCrates(buf, crates);
        readCrates(buf, free);
        int l = buf.readInt();
        for (int i = 0; i < l; i++) {
            LootInfo li = new LootInfo();
            li.slot = buf.readByte();
            li.stack = ByteBufUtils.readItemStack(buf);
            li.chance = buf.readByte();
            loot.add(li);
        }
    }

    public static class Handler implements IMessageHandler<MsgPanelData, IMessage> {
        @Override
        public IMessage onMessage(MsgPanelData message, MessageContext ctx) {
            RustCrates.proxy.handlePanelData(message);
            return null;
        }
    }
}
