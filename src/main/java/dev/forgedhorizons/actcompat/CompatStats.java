package dev.forgedhorizons.actcompat;

import java.util.concurrent.atomic.AtomicLong;

/** Contadores para o /actcompat info: mostram se cada correcao esta mesmo rodando. */
public final class CompatStats {
    public static final AtomicLong FORLORN_ORIGINAL_SHAPE = new AtomicLong();
    public static final AtomicLong FORLORN_STARTS = new AtomicLong();
    public static final AtomicLong MAGNETIC_OPEN_SHAPE = new AtomicLong();

    private CompatStats() {}
}
