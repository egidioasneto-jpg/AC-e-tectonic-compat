package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.structure.FerrocaveStructure;
import dev.forgedhorizons.actcompat.ACBiomeResolver;
import dev.forgedhorizons.actcompat.CompatConfig;
import dev.forgedhorizons.actcompat.CompatState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Magnetic Caves sob o oceano: o centro da caverna desce (magnetic_ocean_center_y),
 * para ela caber abaixo do teto e o inicio da estrutura cair dentro do bioma.
 * Em terra nada muda (o AC sorteia entre -20 e 20).
 */
@Mixin(value = FerrocaveStructure.class, remap = false)
public abstract class FerrocaveStructureMixin {

    @Inject(method = "getGenerateYHeight", at = @At("RETURN"), cancellable = true)
    private void actcompat$deeperUnderOcean(WorldgenRandom random, int x, int z, CallbackInfoReturnable<Integer> cir) {
        if (ACBiomeResolver.isMagneticOceanZone(CompatState.seed(), x, z, CompatState.overworldSampler())) {
            cir.setReturnValue(CompatConfig.magneticOceanCenterY + (cir.getReturnValue() / 4));
        }
    }
}
