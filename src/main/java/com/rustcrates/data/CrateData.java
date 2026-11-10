package com.rustcrates.data;

import com.rustcrates.CrateTier;
import com.rustcrates.tile.TileCrate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

/**
 * Все данные мода. Сохраняется в папке мира (data/rustcrates.dat).
 *
 * У каждого поставленного ADMIN-ящика своя группа (Group): свой пул лута, шансы, настройки и
 * список привязанных к нему ящиков. Группа бывает:
 *  - открытой (locked = false) — новые поставленные ящики этого типа попадают в неё;
 *  - привязанной/закреплённой (locked = true) — новые ящики в неё больше не попадают.
 * Открытой может быть только одна группа каждого типа — самая новая.
 * Ящики, которые поставили, когда открытой группы нет, попадают в «свободные».
 */
public class CrateData extends WorldSavedData {
    public static final String NAME = "rustcrates";
    public static final int MAX_NAME = 32;

    public final List<Group> groups = new ArrayList<>();
    private final EnumMap<CrateTier, List<CrateEntry>> free = new EnumMap<>(CrateTier.class);
    /** Настройки из старой версии мода (1.0.0), ещё не забранные ни одним ADMIN-ящиком. */
    private final EnumMap<CrateTier, Group> legacy = new EnumMap<>(CrateTier.class);
    private long nextSeq = 1;

    public CrateData() {
        this(NAME);
    }

    public CrateData(String name) {
        super(name);
        for (CrateTier t : CrateTier.values()) free.put(t, new ArrayList<CrateEntry>());
    }

    public static CrateData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        CrateData data = (CrateData) storage.getOrLoadData(CrateData.class, NAME);
        if (data == null) {
            data = new CrateData();
            storage.setData(NAME, data);
        }
        return data;
    }

    // ---------------------------------------------------------------- группы (ADMIN-ящики)

    public List<CrateEntry> free(CrateTier t) {
        return free.get(t);
    }

    public Group findGroup(int dim, BlockPos pos) {
        for (Group g : groups) if (g.dim == dim && g.pos.equals(pos)) return g;
        return null;
    }

    /** Открытая (не привязанная) группа типа, в которую попадают новые ящики, или null. */
    public Group openGroup(CrateTier t) {
        Group best = null;
        for (Group g : groups) {
            if (g.tier == t && !g.locked && (best == null || g.seq > best.seq)) best = g;
        }
        return best;
    }

    /** Группа ADMIN-ящика; создаётся, если её ещё нет (например, ящик поставлен в старой версии мода). */
    public Group getOrCreateGroup(CrateTier t, int dim, BlockPos pos) {
        Group g = findGroup(dim, pos);
        if (g != null) return g;
        g = legacy.remove(t);
        if (g == null) g = new Group(this);
        g.tier = t;
        g.dim = dim;
        g.pos = pos.toImmutable();
        g.seq = nextSeq++;
        g.locked = openGroup(t) != null;
        groups.add(g);
        if (!g.locked) adoptFree(g);
        markDirty();
        return g;
    }

    /** Поставлен новый ADMIN-ящик: старые ADMIN-ящики этого типа закрепляются, новые ящики идут в него. */
    public Group onAdminPlaced(CrateTier t, int dim, BlockPos pos) {
        Group g = findGroup(dim, pos);
        if (g != null && !g.locked) return g;
        for (Group o : groups) {
            if (o.tier == t && o != g) o.locked = true;
        }
        if (g == null) {
            g = getOrCreateGroup(t, dim, pos);
        } else {
            g.locked = false;
            g.seq = nextSeq++;
            adoptFree(g);
        }
        markDirty();
        return g;
    }

    /** ADMIN-ящик сломан: его ящики становятся свободными. */
    public void onAdminRemoved(int dim, BlockPos pos) {
        Group g = findGroup(dim, pos);
        if (g == null) return;
        groups.remove(g);
        free(g.tier).addAll(g.crates);
        g.crates.clear();
        markDirty();
    }

    /** Включить/выключить привязку. Снятие привязки закрепляет остальные ADMIN-ящики этого типа. */
    public void setLocked(Group g, boolean locked) {
        if (locked) {
            g.locked = true;
        } else {
            for (Group o : groups) {
                if (o.tier == g.tier && o != g) o.locked = true;
            }
            g.locked = false;
            g.seq = nextSeq++;
            adoptFree(g);
        }
        markDirty();
    }

    private void adoptFree(Group g) {
        List<CrateEntry> f = free(g.tier);
        if (!f.isEmpty()) {
            g.crates.addAll(f);
            f.clear();
            markDirty();
        }
    }

    /** Привязать свободный ящик к группе. */
    public boolean bind(Group g, int dim, BlockPos pos) {
        Iterator<CrateEntry> it = free(g.tier).iterator();
        while (it.hasNext()) {
            CrateEntry e = it.next();
            if (e.dim == dim && e.pos.equals(pos)) {
                it.remove();
                g.crates.add(e);
                markDirty();
                return true;
            }
        }
        return false;
    }

    /** Привязать все свободные ящики типа к группе. */
    public int bindAll(Group g) {
        int n = free(g.tier).size();
        adoptFree(g);
        return n;
    }

    /** Отвязать ящик от группы — он становится свободным. */
    public boolean unbind(Group g, int dim, BlockPos pos) {
        Iterator<CrateEntry> it = g.crates.iterator();
        while (it.hasNext()) {
            CrateEntry e = it.next();
            if (e.dim == dim && e.pos.equals(pos)) {
                it.remove();
                free(g.tier).add(e);
                markDirty();
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- обычные ящики

    public CrateEntry findEntry(int dim, BlockPos pos) {
        for (Group g : groups) {
            CrateEntry e = g.find(dim, pos);
            if (e != null) return e;
        }
        for (List<CrateEntry> list : free.values()) {
            for (CrateEntry e : list) if (e.dim == dim && e.pos.equals(pos)) return e;
        }
        return null;
    }

    public void addCrate(CrateTier t, int dim, BlockPos pos) {
        if (findEntry(dim, pos) != null) return;
        CrateEntry e = new CrateEntry(dim, pos.toImmutable(), true, "");
        Group g = openGroup(t);
        if (g != null) g.crates.add(e);
        else free(t).add(e);
        markDirty();
    }

    public void removeCrate(int dim, BlockPos pos) {
        boolean changed = false;
        for (Group g : groups) {
            if (g.crates.removeIf(e -> e.dim == dim && e.pos.equals(pos))) changed = true;
        }
        for (List<CrateEntry> list : free.values()) {
            if (list.removeIf(e -> e.dim == dim && e.pos.equals(pos))) changed = true;
        }
        if (changed) markDirty();
    }

    /** Переименовать поставленный ящик (название видно в панели и в заголовке ящика). */
    public boolean rename(int dim, BlockPos pos, String name) {
        CrateEntry e = findEntry(dim, pos);
        if (e == null) return false;
        if (name == null) name = "";
        name = name.trim();
        if (name.length() > MAX_NAME) name = name.substring(0, MAX_NAME);
        e.name = name;
        World w = DimensionManager.getWorld(dim);
        if (w != null && w.isBlockLoaded(pos)) {
            TileEntity te = w.getTileEntity(pos);
            if (te instanceof TileCrate) ((TileCrate) te).setCrateName(name);
        }
        markDirty();
        return true;
    }

    // ---------------------------------------------------------------- NBT

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        groups.clear();
        legacy.clear();
        for (List<CrateEntry> list : free.values()) list.clear();
        nextSeq = Math.max(1L, nbt.getLong("Seq"));
        if (nbt.getInteger("Version") >= 2) {
            NBTTagList gl = nbt.getTagList("Groups", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < gl.tagCount(); i++) {
                Group g = new Group(this);
                g.read(gl.getCompoundTagAt(i));
                groups.add(g);
                if (g.seq >= nextSeq) nextSeq = g.seq + 1;
            }
            for (CrateTier t : CrateTier.values()) {
                readEntries(nbt.getTagList("Free_" + t.id, Constants.NBT.TAG_COMPOUND), free(t));
                if (nbt.hasKey("Legacy_" + t.id)) {
                    Group g = new Group(this);
                    g.read(nbt.getCompoundTag("Legacy_" + t.id));
                    g.tier = t;
                    legacy.put(t, g);
                }
            }
        } else {
            // Старый формат 1.0.0: одна настройка на каждый тип ящиков.
            for (CrateTier t : CrateTier.values()) {
                if (nbt.hasKey(t.id)) {
                    Group g = new Group(this);
                    g.read(nbt.getCompoundTag(t.id));
                    g.tier = t;
                    legacy.put(t, g);
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Version", 2);
        nbt.setLong("Seq", nextSeq);
        NBTTagList gl = new NBTTagList();
        for (Group g : groups) gl.appendTag(g.write());
        nbt.setTag("Groups", gl);
        for (CrateTier t : CrateTier.values()) {
            nbt.setTag("Free_" + t.id, writeEntries(free(t)));
            Group lg = legacy.get(t);
            if (lg != null) nbt.setTag("Legacy_" + t.id, lg.write());
        }
        return nbt;
    }

    static void readEntries(NBTTagList list, List<CrateEntry> out) {
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            out.add(new CrateEntry(c.getInteger("Dim"), BlockPos.fromLong(c.getLong("Pos")), c.getBoolean("Sel"), c.getString("Name")));
        }
    }

    static NBTTagList writeEntries(List<CrateEntry> entries) {
        NBTTagList list = new NBTTagList();
        for (CrateEntry e : entries) {
            NBTTagCompound c = new NBTTagCompound();
            c.setInteger("Dim", e.dim);
            c.setLong("Pos", e.pos.toLong());
            c.setBoolean("Sel", e.selected);
            c.setString("Name", e.name == null ? "" : e.name);
            list.appendTag(c);
        }
        return list;
    }

    public static class CrateEntry {
        public final int dim;
        public final BlockPos pos;
        public boolean selected;
        public String name;

        public CrateEntry(int dim, BlockPos pos, boolean selected, String name) {
            this.dim = dim;
            this.pos = pos;
            this.selected = selected;
            this.name = name == null ? "" : name;
        }
    }

    /** Настройки одного ADMIN-ящика. */
    public static class Group {
        public static final int POOL = 27;
        public CrateTier tier = CrateTier.NORMAL;
        public int dim = 0;
        public BlockPos pos = BlockPos.ORIGIN;
        public boolean locked = false;
        public long seq = 0;

        public final ItemStackHandler pool;
        public final int[] chances = new int[POOL];
        public boolean enabled = true;
        public int interval = 300;
        public boolean clearBefore = true;
        public boolean randomCount = true;
        public int maxItems = 0;
        public final List<CrateEntry> crates = new ArrayList<>();
        public int ticks = 0;

        Group(final CrateData owner) {
            pool = new ItemStackHandler(POOL) {
                @Override
                protected void onContentsChanged(int slot) {
                    owner.markDirty();
                }
            };
            Arrays.fill(chances, 50);
        }

        public CrateEntry find(int dim, BlockPos pos) {
            for (CrateEntry e : crates) if (e.dim == dim && e.pos.equals(pos)) return e;
            return null;
        }

        void read(NBTTagCompound n) {
            tier = CrateTier.byId(n.getInteger("Tier"));
            dim = n.getInteger("Dim");
            if (n.hasKey("Pos")) pos = BlockPos.fromLong(n.getLong("Pos"));
            locked = n.getBoolean("Locked");
            seq = n.getLong("Seq");
            if (n.hasKey("Pool")) pool.deserializeNBT(n.getCompoundTag("Pool"));
            if (pool.getSlots() != POOL) pool.setSize(POOL);
            int[] ch = n.getIntArray("Chances");
            for (int i = 0; i < Math.min(ch.length, POOL); i++) chances[i] = ch[i];
            if (n.hasKey("Enabled")) enabled = n.getBoolean("Enabled");
            if (n.hasKey("Interval")) interval = n.getInteger("Interval");
            if (n.hasKey("Clear")) clearBefore = n.getBoolean("Clear");
            if (n.hasKey("Random")) randomCount = n.getBoolean("Random");
            maxItems = n.getInteger("Max");
            ticks = n.getInteger("Ticks");
            crates.clear();
            readEntries(n.getTagList("Crates", Constants.NBT.TAG_COMPOUND), crates);
        }

        NBTTagCompound write() {
            NBTTagCompound n = new NBTTagCompound();
            n.setInteger("Tier", tier.ordinal());
            n.setInteger("Dim", dim);
            n.setLong("Pos", pos.toLong());
            n.setBoolean("Locked", locked);
            n.setLong("Seq", seq);
            n.setTag("Pool", pool.serializeNBT());
            n.setIntArray("Chances", chances);
            n.setBoolean("Enabled", enabled);
            n.setInteger("Interval", interval);
            n.setBoolean("Clear", clearBefore);
            n.setBoolean("Random", randomCount);
            n.setInteger("Max", maxItems);
            n.setInteger("Ticks", ticks);
            n.setTag("Crates", writeEntries(crates));
            return n;
        }
    }
}
