package com.rustcrates;

public enum CrateTier {
    NORMAL("normal", "Обычные ящики"),
    ELITE("elite", "Элитные ящики"),
    MILITARY("military", "Военные ящики");

    public final String id;
    public final String title;

    CrateTier(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public static CrateTier byId(int i) {
        CrateTier[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }
}
