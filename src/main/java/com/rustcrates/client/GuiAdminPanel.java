package com.rustcrates.client;

import com.rustcrates.CrateTier;
import com.rustcrates.network.MsgPanelAction;
import com.rustcrates.network.MsgPanelData;
import com.rustcrates.network.PacketHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/** Панель управления ADMIN-ящика (Shift + ПКМ). */
public class GuiAdminPanel extends GuiScreen {
    private static final int W = 340, H = 256, ROWS = 7, ROW_H = 22;
    private static final int TAB_LOOT = 0, TAB_CRATES = 1, TAB_FREE = 2, TAB_SETTINGS = 3;
    private static final int[] CHANCE_DELTA = {-10, -1, 1, 10};
    private static final String[] CHANCE_LABEL = {"-10", "-1", "+1", "+10"};
    private static final int[] INTERVAL_DELTA = {-60, -10, 10, 60};
    private static final String[] INTERVAL_LABEL = {"-60", "-10", "+10", "+60"};

    private MsgPanelData data;
    private int tab = 0;
    private int scroll = 0;
    private int left, top;
    private int refreshTicks = 0;

    // Режим переименования ящика
    private GuiTextField field;
    private int renameDim;
    private BlockPos renamePos;
    private String renameText = "";

    public GuiAdminPanel(MsgPanelData data) {
        this.data = data;
    }

    public long admin() {
        return data.admin;
    }

    public int tier() {
        return data.tier;
    }

    public void setData(MsgPanelData d) {
        this.data = d;
        clampScroll();
        rebuild();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (renamePos != null) {
            String keep = field != null ? field.getText() : renameText;
            createField(keep);
        }
        rebuild();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private void createField(String text) {
        field = new GuiTextField(200, fontRenderer, left + 11, top + 43, 188, 18);
        field.setMaxStringLength(32);
        field.setText(text == null ? "" : text);
        field.setFocused(true);
    }

    private boolean renaming() {
        return renamePos != null && field != null;
    }

    private void startRename(MsgPanelData.CrateInfo ci) {
        renameDim = ci.dim;
        renamePos = ci.pos;
        renameText = ci.name == null ? "" : ci.name;
        createField(renameText);
        rebuild();
    }

    private void stopRename() {
        renamePos = null;
        field = null;
        renameText = "";
        rebuild();
    }

    private void submitRename() {
        if (!renaming()) return;
        send(MsgPanelAction.RENAME, renameDim, 0, renamePos.toLong(), field.getText());
        stopRename();
    }

    private List<MsgPanelData.CrateInfo> crateList() {
        return tab == TAB_FREE ? data.free : data.crates;
    }

    private int listSize() {
        if (tab == TAB_LOOT) return data.loot.size();
        if (tab == TAB_CRATES) return data.crates.size();
        if (tab == TAB_FREE) return data.free.size();
        return 0;
    }

    private int maxScroll() {
        return Math.max(0, listSize() - ROWS);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int rowY(int r) {
        return top + 66 + r * ROW_H;
    }

    private static String yesNo(boolean b) {
        return b ? "§aДа" : "§cНет";
    }

    private static String time(int s) {
        if (s >= 60) return (s / 60) + " мин " + (s % 60) + " сек";
        return s + " сек";
    }

    private void rebuild() {
        buttonList.clear();
        String[] tabNames = {
                "Лут (" + data.loot.size() + ")",
                "Ящики (" + data.crates.size() + ")",
                "Свободные (" + data.free.size() + ")",
                "Настройки"};
        for (int k = 0; k < 4; k++) {
            GuiButton b = new GuiButton(k, left + 10 + k * 81, top + 18, 77, 20, tabNames[k]);
            b.enabled = tab != k;
            buttonList.add(b);
        }
        buttonList.add(new GuiButton(6, left + W - 70, top + H - 26, 60, 20, "Закрыть"));

        int right = left + W - 10;
        if (renaming()) {
            buttonList.add(new GuiButton(50, left + 206, top + 42, 70, 20, "§aСохранить"));
            buttonList.add(new GuiButton(51, left + 280, top + 42, 50, 20, "Отмена"));
        }

        if (tab == TAB_LOOT) {
            if (!renaming()) buttonList.add(new GuiButton(10, left + 10, top + 42, 140, 20, "Изменить предметы"));
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.loot.size()) break;
                int bx = right - 4 * 24;
                for (int k = 0; k < 4; k++) {
                    buttonList.add(new GuiButton(100 + r * 10 + k, bx + k * 24, rowY(r), 22, 20, CHANCE_LABEL[k]));
                }
            }
        } else if (tab == TAB_CRATES) {
            if (!renaming()) {
                buttonList.add(new GuiButton(20, left + 10, top + 42, 90, 20, "Выбрать все"));
                buttonList.add(new GuiButton(21, left + 104, top + 42, 90, 20, "Снять все"));
            }
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.crates.size()) break;
                MsgPanelData.CrateInfo ci = data.crates.get(idx);
                int y = rowY(r);
                int xX = right - 20;
                int xTp = xX - 2 - 24;
                int xOn = xTp - 2 - 40;
                int xName = xOn - 2 - 34;
                buttonList.add(new GuiButton(100 + r * 10 + 2, xName, y, 34, 20, "Имя"));
                buttonList.add(new GuiButton(100 + r * 10, xOn, y, 40, 20, ci.selected ? "§aВкл" : "§cВыкл"));
                buttonList.add(new GuiButton(100 + r * 10 + 1, xTp, y, 24, 20, "ТП"));
                buttonList.add(new GuiButton(100 + r * 10 + 3, xX, y, 20, 20, "§cX"));
            }
        } else if (tab == TAB_FREE) {
            if (!renaming()) {
                GuiButton all = new GuiButton(22, left + 10, top + 42, 120, 20, "Привязать все");
                all.enabled = !data.free.isEmpty();
                buttonList.add(all);
            }
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.free.size()) break;
                int y = rowY(r);
                int xBind = right - 80;
                int xTp = xBind - 2 - 24;
                int xName = xTp - 2 - 34;
                buttonList.add(new GuiButton(100 + r * 10 + 2, xName, y, 34, 20, "Имя"));
                buttonList.add(new GuiButton(100 + r * 10 + 1, xTp, y, 24, 20, "ТП"));
                buttonList.add(new GuiButton(100 + r * 10, xBind, y, 80, 20, "§a+ Привязать"));
            }
        } else {
            buttonList.add(new GuiButton(30, left + 10, top + 42, 200, 20, "Автоспавн: " + (data.enabled ? "§aВкл" : "§cВыкл")));
            for (int k = 0; k < 4; k++) {
                buttonList.add(new GuiButton(31 + k, right - 4 * 30 + k * 30, top + 66, 28, 20, INTERVAL_LABEL[k]));
            }
            buttonList.add(new GuiButton(35, left + 10, top + 90, 240, 20, "Очищать ящик перед спавном: " + yesNo(data.clearBefore)));
            buttonList.add(new GuiButton(36, left + 10, top + 114, 240, 20, "Случайное кол-во (от 1 до N): " + yesNo(data.randomCount)));
            buttonList.add(new GuiButton(37, right - 2 * 30, top + 138, 28, 20, "-1"));
            buttonList.add(new GuiButton(38, right - 30, top + 138, 28, 20, "+1"));
            buttonList.add(new GuiButton(39, left + 10, top + 164, 200, 20, "§eЗаспавнить лут сейчас"));
            buttonList.add(new GuiButton(40, left + 10, top + 188, 320, 20, "Новые ящики: "
                    + (data.locked ? "§cне привязываются (закреплён)" : "§aпривязываются сюда")));
        }

        if (tab != TAB_SETTINGS && maxScroll() > 0 && !renaming()) {
            GuiButton up = new GuiButton(4, left + 10, top + H - 26, 50, 20, "Вверх");
            GuiButton down = new GuiButton(5, left + 64, top + H - 26, 50, 20, "Вниз");
            up.enabled = scroll > 0;
            down.enabled = scroll < maxScroll();
            buttonList.add(up);
            buttonList.add(down);
        }
    }

    private void send(int action, int a, int b, long c) {
        send(action, a, b, c, "");
    }

    private void send(int action, int a, int b, long c, String s) {
        PacketHandler.NET.sendToServer(new MsgPanelAction(data.admin, action, a, b, c, s));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;
        if (id <= 3) {
            tab = id;
            scroll = 0;
            rebuild();
            return;
        }
        switch (id) {
            case 4: scroll--; clampScroll(); rebuild(); return;
            case 5: scroll++; clampScroll(); rebuild(); return;
            case 6: mc.displayGuiScreen(null); return;
            case 10: send(MsgPanelAction.OPEN_POOL, 0, 0, 0); return;
            case 20: send(MsgPanelAction.SELECT_ALL, 0, 0, 0); return;
            case 21: send(MsgPanelAction.DESELECT_ALL, 0, 0, 0); return;
            case 22: send(MsgPanelAction.BIND_ALL, 0, 0, 0); return;
            case 30: send(MsgPanelAction.TOGGLE_ENABLED, 0, 0, 0); return;
            case 31: case 32: case 33: case 34:
                send(MsgPanelAction.SET_INTERVAL, 0, data.interval + INTERVAL_DELTA[id - 31], 0); return;
            case 35: send(MsgPanelAction.TOGGLE_CLEAR, 0, 0, 0); return;
            case 36: send(MsgPanelAction.TOGGLE_RANDOM, 0, 0, 0); return;
            case 37: send(MsgPanelAction.SET_MAX, 0, data.maxItems - 1, 0); return;
            case 38: send(MsgPanelAction.SET_MAX, 0, data.maxItems + 1, 0); return;
            case 39: send(MsgPanelAction.SPAWN_NOW, 0, 0, 0); return;
            case 40: send(MsgPanelAction.TOGGLE_LOCK, 0, 0, 0); return;
            case 50: submitRename(); return;
            case 51: stopRename(); return;
            default: break;
        }
        if (id >= 100) {
            int r = (id - 100) / 10;
            int k = (id - 100) % 10;
            int idx = scroll + r;
            if (tab == TAB_LOOT) {
                if (idx >= data.loot.size()) return;
                MsgPanelData.LootInfo li = data.loot.get(idx);
                li.chance = Math.max(0, Math.min(100, li.chance + CHANCE_DELTA[k]));
                send(MsgPanelAction.SET_CHANCE, li.slot, li.chance, 0);
                return;
            }
            List<MsgPanelData.CrateInfo> list = crateList();
            if (idx >= list.size()) return;
            MsgPanelData.CrateInfo ci = list.get(idx);
            if (k == 1) {
                send(MsgPanelAction.TELEPORT, ci.dim, 0, ci.pos.toLong());
                mc.displayGuiScreen(null);
            } else if (k == 2) {
                startRename(ci);
            } else if (tab == TAB_CRATES && k == 0) {
                send(MsgPanelAction.TOGGLE_CRATE, ci.dim, 0, ci.pos.toLong());
            } else if (tab == TAB_CRATES && k == 3) {
                send(MsgPanelAction.UNBIND, ci.dim, 0, ci.pos.toLong());
            } else if (tab == TAB_FREE && k == 0) {
                send(MsgPanelAction.BIND, ci.dim, 0, ci.pos.toLong());
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (renaming()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                submitRename();
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                stopRename();
            } else {
                field.textboxKeyTyped(typedChar, keyCode);
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (renaming()) field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dw = Mouse.getEventDWheel();
        if (dw != 0 && tab != TAB_SETTINGS) {
            scroll += dw > 0 ? -1 : 1;
            clampScroll();
            rebuild();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (renaming()) field.updateCursorCounter();
        if (++refreshTicks % 40 == 0) send(MsgPanelAction.REFRESH, 0, 0, 0);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void drawCrateRows(List<MsgPanelData.CrateInfo> list, int textWidth) {
        for (int r = 0; r < ROWS; r++) {
            int idx = scroll + r;
            if (idx >= list.size()) break;
            MsgPanelData.CrateInfo ci = list.get(idx);
            int y = rowY(r);
            boolean editing = renamePos != null && ci.dim == renameDim && ci.pos.equals(renamePos);
            drawRect(left + 8, y - 1, left + W - 8, y + 21, editing ? 0x40FFAA00 : (r % 2 == 0 ? 0x30FFFFFF : 0x18FFFFFF));
            String name = ci.name == null || ci.name.isEmpty() ? "§8Без названия" : "§f" + ci.name;
            drawString(fontRenderer, fontRenderer.trimStringToWidth("§7#" + (idx + 1) + " " + name, textWidth), left + 12, y + 1, 0xFFFFFF);
            String coords = "§b" + ci.pos.getX() + " " + ci.pos.getY() + " " + ci.pos.getZ() + "  §7изм. " + ci.dim
                    + (ci.loaded ? "" : "  §8(не загружен)");
            drawString(fontRenderer, fontRenderer.trimStringToWidth(coords, textWidth), left + 12, y + 11, 0xFFFFFF);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(left, top, left + W, top + H, 0xE0101010);
        drawRect(left, top, left + W, top + 1, 0xFFCC0000);
        drawRect(left, top + H - 1, left + W, top + H, 0xFFCC0000);
        drawRect(left, top, left + 1, top + H, 0xFFCC0000);
        drawRect(left + W - 1, top, left + W, top + H, 0xFFCC0000);
        CrateTier t = CrateTier.byId(data.tier);
        BlockPos ap = BlockPos.fromLong(data.admin);
        String title = "§c§lADMIN§r §f" + t.title + " §7(" + ap.getX() + " " + ap.getY() + " " + ap.getZ() + ")"
                + (data.locked ? " §6[закреплён]" : " §a[новые ящики сюда]");
        drawCenteredString(fontRenderer, title, left + W / 2, top + 6, 0xFFFFFF);

        if (tab == TAB_LOOT) {
            if (!renaming()) drawString(fontRenderer, "§7Шанс выпадения каждого предмета", left + 158, top + 48, 0xFFFFFF);
            if (data.loot.isEmpty()) {
                drawString(fontRenderer, "Пул лута пуст.", left + 12, rowY(0) + 4, 0xFFFFFF);
                drawString(fontRenderer, "§7Нажмите «Изменить предметы» и положите туда", left + 12, rowY(0) + 18, 0xFFFFFF);
                drawString(fontRenderer, "§7предметы, которые могут выпасть из ящиков.", left + 12, rowY(0) + 30, 0xFFFFFF);
            }
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.loot.size()) break;
                MsgPanelData.LootInfo li = data.loot.get(idx);
                int y = rowY(r);
                drawRect(left + 8, y - 1, left + W - 8, y + 21, r % 2 == 0 ? 0x30FFFFFF : 0x18FFFFFF);
                String name = fontRenderer.trimStringToWidth(li.stack.getDisplayName(), 140);
                drawString(fontRenderer, name + " §7x" + li.stack.getCount(), left + 32, y + 6, 0xFFFFFF);
                drawString(fontRenderer, "§e" + li.chance + "%", left + W - 10 - 4 * 24 - 34, y + 6, 0xFFFFFF);
            }
        } else if (tab == TAB_CRATES) {
            if (!renaming()) {
                int sel = 0;
                for (MsgPanelData.CrateInfo ci : data.crates) if (ci.selected) sel++;
                drawString(fontRenderer, "§7Выбрано: §f" + sel + " / " + data.crates.size(), left + 202, top + 48, 0xFFFFFF);
            }
            if (data.crates.isEmpty()) {
                drawString(fontRenderer, "К этому ADMIN-ящику не привязано ни одного ящика.", left + 12, rowY(0) + 4, 0xFFFFFF);
                drawString(fontRenderer, data.locked
                        ? "§7Он закреплён: привяжите ящики на вкладке «Свободные»."
                        : "§7Поставьте обычные ящики — они появятся здесь.", left + 12, rowY(0) + 18, 0xFFFFFF);
            }
            drawCrateRows(data.crates, 190);
        } else if (tab == TAB_FREE) {
            if (!renaming()) drawString(fontRenderer, "§7Ящики без ADMIN-ящика", left + 138, top + 48, 0xFFFFFF);
            if (data.free.isEmpty()) {
                drawString(fontRenderer, "Свободных ящиков этого типа нет.", left + 12, rowY(0) + 4, 0xFFFFFF);
                drawString(fontRenderer, "§7Сюда попадают отвязанные ящики и ящики,", left + 12, rowY(0) + 18, 0xFFFFFF);
                drawString(fontRenderer, "§7поставленные когда все ADMIN-ящики закреплены.", left + 12, rowY(0) + 30, 0xFFFFFF);
            }
            drawCrateRows(data.free, 170);
        } else {
            drawString(fontRenderer, "Интервал спавна: §e" + time(data.interval), left + 12, top + 72, 0xFFFFFF);
            drawString(fontRenderer, "Макс. предметов в ящике: §e" + (data.maxItems == 0 ? "без лимита" : String.valueOf(data.maxItems)), left + 12, top + 144, 0xFFFFFF);
            drawString(fontRenderer, "До следующего спавна: " + (data.enabled ? "§e" + time(data.secondsLeft) : "§cавтоспавн выключен"), left + 12, top + 214, 0xFFFFFF);
        }

        if (renaming()) {
            field.drawTextBox();
            drawString(fontRenderer, "§7Enter — сохранить, Esc — отмена (пусто = без названия)", left + 12, top + H - 20, 0xFFFFFF);
        } else if (tab != TAB_SETTINGS && maxScroll() > 0) {
            int from = scroll + 1;
            int to = Math.min(listSize(), scroll + ROWS);
            drawString(fontRenderer, "§7" + from + "–" + to + " из " + listSize(), left + 122, top + H - 20, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (tab == TAB_LOOT) {
            RenderHelper.enableGUIStandardItemLighting();
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.loot.size()) break;
                itemRender.renderItemAndEffectIntoGUI(data.loot.get(idx).stack, left + 12, rowY(r) + 2);
            }
            RenderHelper.disableStandardItemLighting();
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= data.loot.size()) break;
                int x = left + 12, y = rowY(r) + 2;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    renderToolTip(data.loot.get(idx).stack, mouseX, mouseY);
                }
            }
        }
    }
}
