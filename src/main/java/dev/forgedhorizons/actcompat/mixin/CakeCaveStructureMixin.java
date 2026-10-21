package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.structure.CakeCaveStructure;
import dev.forgedhorizons.actcompat.CompatConfig;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Candy Cavity (Cake Cave) um pouco maior: largura x multiplicador, altura x metade do ganho. */
@Mixin(value = CakeCaveStructure.class, remap = false)
public abstract class CakeCaveStructureMixin {

    @Inject(method = "getWidthRadius", at = @At("RETURN"), cancellable = true)
    private void actcompat$wider(WorldgenRandom random, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) Math.round(cir.getReturnValue() * CompatConfig.candySizeMultiplier));
    }

    @Inject(method = "getHeightRadius", at = @At("RETURN"), cancellable = true)
    private void actcompat$taller(WorldgenRandom random, int seaLevel, CallbackInfoReturnable<Integer> cir) {
        double m = 1.0D + (CompatConfig.candySizeMultiplier - 1.0D) * 0.5D;
        cir.setReturnValue((int) Math.round(cir.getReturnValue() * m));
    }
}
