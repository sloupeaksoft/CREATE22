package com.rustcrates;

import net.minecraft.entity.player.EntityPlayer;

public final class AdminUtil {
    private AdminUtil() {}

    /** Админ = оператор сервера (уровень 2+) или игрок в креативе. */
    public static boolean isAdmin(EntityPlayer p) {
        if (p == null) return false;
        if (p.capabilities.isCreativeMode) return true;
        return p.canUseCommand(2, "rustcrates");
    }
}
