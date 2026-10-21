package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.structure.piece.FerrocaveStructurePiece;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import dev.forgedhorizons.actcompat.CompatConfig;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Magnetic Caves mais "abertas".
 * Formula identica a do AC original, com dois ajustes controlados por magnetic_openness (0 = original):
 *  - as "cascas" de pedra soltas no ar (faixa do ruido innerCircleOrb entre -0.75 e -0.5) ficam mais finas e raras;
 *  - o raio da caverna varia menos (menos bolsões e pilares de pedra no meio do salão).
 */
@Mixin(value = FerrocaveStructurePiece.class, remap = false)
public abstract class FerrocaveStructurePieceMixin {

    @Inject(method = "inCircle(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void actcompat$moreOpen(BlockPos carve, CallbackInfoReturnable<Boolean> cir) {
        double openness = CompatConfig.magneticOpenness;
        if (openness <= 0) return; // comportamento original

        CavePieceAccessor self = (CavePieceAccessor) this;
        BlockPos holeCenter = self.actcompat$getHoleCenter();
        int height = self.actcompat$getHeight();
        int radius = self.actcompat$getRadius();
        if (height <= 0 || radius <= 0) return;
        dev.forgedhorizons.actcompat.CompatStats.MAGNETIC_OPEN_SHAPE.incrementAndGet();

        float df1 = (ACMath.sampleNoise3D(carve.getX(), carve.getY(), carve.getZ(), 30) + 1.0F) * 0.5F;
        float df2 = ACMath.sampleNoise3D(carve.getX() - 1200, carve.getY() + 100, carve.getZ() + 120, 10) * 0.15F;
        float innerCircleOrb = ACMath.sampleNoise3D(carve.getX() + 400, carve.getY() + 40, carve.getZ() - 600, 20);
        double df1Smooth = ACMath.smin(df1 + df2, 1.0F, 0.1F);
        df1Smooth = df1Smooth + (1.0D - df1Smooth) * openness * 0.5D;
        double yDist = ACMath.smin(1F - Math.abs(holeCenter.getY() - carve.getY()) / (float) (height * 0.5F), 1.0F, 0.3F);
        double distToCenter = carve.distToLowCornerSqr(holeCenter.getX(), carve.getY(), holeCenter.getZ());
        double targetRadius = yDist * (radius * df1Smooth) * radius;

        double bandHalf = 0.125D * (1.0D - openness);   // original: 0.125 (faixa -0.75..-0.5)
        boolean shell = Math.abs(innerCircleOrb + 0.625D) < bandHalf;
        cir.setReturnValue(distToCenter < targetRadius && !shell);
    }
}
