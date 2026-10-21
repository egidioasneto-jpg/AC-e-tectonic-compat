package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.structure.piece.ForlornCanyonStructurePiece;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forlorn Hollows: o port trocou o formato de canion (pilares, paredes em degraus, vaos enormes)
 * por uma "tigela" no estilo do Dino Bowl. Aqui volta a formula ORIGINAL do Alex's Caves 1.20.1.
 */
@Mixin(value = ForlornCanyonStructurePiece.class, remap = false)
public abstract class ForlornCanyonStructurePieceMixin {

    @Inject(method = "inCircle(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void actcompat$originalCanyonShape(BlockPos carve, CallbackInfoReturnable<Boolean> cir) {
        CavePieceAccessor self = (CavePieceAccessor) this;
        BlockPos holeCenter = self.actcompat$getHoleCenter();
        int height = self.actcompat$getHeight();
        int radius = self.actcompat$getRadius();
        if (height <= 0 || radius <= 0) return;
        dev.forgedhorizons.actcompat.CompatStats.FORLORN_ORIGINAL_SHAPE.incrementAndGet();

        float pillarNoise = (ACMath.sampleNoise3D(carve.getX(), (int) (carve.getY() * 0.4F), carve.getZ(), 30) + 1.0F) * 0.5F;
        float verticalNoise = (ACMath.sampleNoise2D(carve.getX(), carve.getZ(), 50) + 1.0F) * 0.2F
                - (ACMath.smin(ACMath.sampleNoise2D(carve.getX(), carve.getZ(), 20), -0.5F, 0.1F) + 0.5F) * 0.7F;
        double distToCenter = carve.distToLowCornerSqr(holeCenter.getX(), carve.getY(), holeCenter.getZ());
        float f = heightOf(carve, holeCenter, height);
        float f1 = (float) Math.pow(ACMath.canyonStep(f, 10), 2.5F);
        float rawHeight = Math.abs(holeCenter.getY() - carve.getY()) / (float) (height * 0.5F);
        float reverseRawHeight = 1F - rawHeight;
        double yDist = ACMath.smin((float) Math.pow(reverseRawHeight, 0.3F), 1.0F, 0.1F);
        double targetRadius = (yDist * (radius * pillarNoise * f1) * radius);
        cir.setReturnValue(distToCenter < targetRadius && rawHeight < 1 - verticalNoise);
    }

    private static float heightOf(BlockPos carve, BlockPos holeCenter, int height) {
        int halfHeight = height / 2;
        if (carve.getY() > holeCenter.getY() + halfHeight + 1 || carve.getY() < holeCenter.getY() - halfHeight) {
            return 0.0F;
        }
        return 1F - ((holeCenter.getY() + halfHeight - carve.getY()) / (float) (height * 2));
    }
}
