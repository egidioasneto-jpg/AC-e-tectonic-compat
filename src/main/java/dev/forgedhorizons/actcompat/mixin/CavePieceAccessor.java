package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.structure.piece.AbstractCaveGenerationStructurePiece;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Acesso aos campos protegidos das pecas de caverna do Alex's Caves. */
@Mixin(value = AbstractCaveGenerationStructurePiece.class, remap = false)
public interface CavePieceAccessor {
    @Accessor("holeCenter")
    BlockPos actcompat$getHoleCenter();

    @Accessor("height")
    int actcompat$getHeight();

    @Accessor("radius")
    int actcompat$getRadius();
}
