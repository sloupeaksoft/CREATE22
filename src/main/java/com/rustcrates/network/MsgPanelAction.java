package com.rustcrates.network;

import com.rustcrates.AdminUtil;
import com.rustcrates.CrateTier;
import com.rustcrates.GuiHandler;
import com.rustcrates.LootSpawner;
import com.rustcrates.RustCrates;
import com.rustcrates.block.BlockCrate;
import com.rustcrates.data.CrateData;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Клиент -> сервер: действие в панели управления ADMIN-ящика. */
public class MsgPanelAction implements IMessage {
    public static final int REFRESH = 0, SET_CHANCE = 1, TOGGLE_CRATE = 2, SELECT_ALL = 3, DESELECT_ALL = 4,
            SET_INTERVAL = 5, TOGGLE_ENABLED = 6, TOGGLE_CLEAR = 7, TOGGLE_RANDOM = 8, SET_MAX = 9,
            SPAWN_NOW = 10, OPEN_POOL = 11, TELEPORT = 12,
            RENAME = 13, UNBIND = 14, BIND = 15, BIND_ALL = 16, TOGGLE_LOCK = 17;

    /** Позиция ADMIN-ящика, чья панель открыта. */
    public long admin;
    public int action, a, b;
    public long c;
    public String s = "";

    public MsgPanelAction() {}

    public MsgPanelAction(long admin, int action, int a, int b, long c, String s) {
        this.admin = admin;
        this.action = action;
        this.a = a;
        this.b = b;
        this.c = c;
        this.s = s == null ? "" : s;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(admin);
        buf.writeByte(action);
        buf.writeInt(a);
        buf.writeInt(b);
        buf.writeLong(c);
        ByteBufUtils.writeUTF8String(buf, s == null ? "" : s);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        admin = buf.readLong();
        action = buf.readByte();
        a = buf.readInt();
        b = buf.readInt();
        c = buf.readLong();
        s = ByteBufUtils.readUTF8String(buf);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    static void handle(MsgPanelAction m, EntityPlayerMP p) {
        if (!AdminUtil.isAdmin(p)) return;
        World w = p.world;
        BlockPos ap = BlockPos.fromLong(m.admin);
        if (!w.isBlockLoaded(ap)) return;
        Block blk = w.getBlockState(ap).getBlock();
        if (!(blk instanceof BlockCrate) || !((BlockCrate) blk).admin) return;
        CrateTier tier = ((BlockCrate) blk).tier;
        CrateData data = CrateData.get();
        CrateData.Group cfg = data.getOrCreateGroup(tier, w.provider.getDimension(), ap);
        switch (m.action) {
            case SET_CHANCE:
                if (m.a >= 0 && m.a < CrateData.Group.POOL) cfg.chances[m.a] = clamp(m.b, 0, 100);
                break;
            case TOGGLE_CRATE: {
                CrateData.CrateEntry e = cfg.find(m.a, BlockPos.fromLong(m.c));
                if (e != null) e.selected = !e.selected;
                break;
            }
            case SELECT_ALL:
                for (CrateData.CrateEntry e : cfg.crates) e.selected = true;
                break;
            case DESELECT_ALL:
                for (CrateData.CrateEntry e : cfg.crates) e.selected = false;
                break;
            case SET_INTERVAL:
                cfg.interval = clamp(m.b, 10, 86400);
                cfg.ticks = 0;
                break;
            case TOGGLE_ENABLED:
                cfg.enabled = !cfg.enabled;
                cfg.ticks = 0;
                break;
            case TOGGLE_CLEAR:
                cfg.clearBefore = !cfg.clearBefore;
                break;
            case TOGGLE_RANDOM:
                cfg.randomCount = !cfg.randomCount;
                break;
            case SET_MAX:
                cfg.maxItems = clamp(m.b, 0, 27);
                break;
            case SPAWN_NOW: {
                int n = LootSpawner.spawn(data, cfg);
                cfg.ticks = 0;
                p.sendMessage(new TextComponentString(TextFormatting.GREEN + "[" + tier.title + "] Лут заспавнен в ящиках: " + n));
                break;
            }
            case OPEN_POOL:
                data.markDirty();
                p.openGui(RustCrates.instance, GuiHandler.GUI_POOL, w, ap.getX(), ap.getY(), ap.getZ());
                return;
            case TELEPORT: {
                BlockPos pos = BlockPos.fromLong(m.c);
                if (m.a != p.dimension) {
                    p.sendMessage(new TextComponentString(TextFormatting.RED + "Ящик находится в другом измерении (" + m.a + ")."));
                } else {
                    p.setPositionAndUpdate(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                }
                return;
            }
            case RENAME: {
                BlockPos pos = BlockPos.fromLong(m.c);
                // Можно переименовать только свои или свободные ящики этого типа.
                boolean own = cfg.find(m.a, pos) != null;
                boolean isFree = false;
                for (CrateData.CrateEntry e : data.free(tier)) {
                    if (e.dim == m.a && e.pos.equals(pos)) isFree = true;
                }
                if (own || isFree) data.rename(m.a, pos, m.s);
                break;
            }
            case UNBIND:
                data.unbind(cfg, m.a, BlockPos.fromLong(m.c));
                break;
            case BIND:
                data.bind(cfg, m.a, BlockPos.fromLong(m.c));
                break;
            case BIND_ALL: {
                int n = data.bindAll(cfg);
                p.sendMessage(new TextComponentString(TextFormatting.GREEN + "Привязано ящиков: " + n));
                break;
            }
            case TOGGLE_LOCK:
                data.setLocked(cfg, !cfg.locked);
                break;
            default:
                break;
        }
        data.markDirty();
        PacketHandler.sendPanel(p, cfg, false);
    }

    public static class Handler implements IMessageHandler<MsgPanelAction, IMessage> {
        @Override
        public IMessage onMessage(final MsgPanelAction message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> handle(message, player));
            return null;
        }
    }
}
