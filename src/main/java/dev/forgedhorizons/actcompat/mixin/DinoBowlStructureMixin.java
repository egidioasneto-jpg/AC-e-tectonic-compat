package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.structure.DinoBowlStructure;
import dev.forgedhorizons.actcompat.CompatConfig;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Primordial Caves (Dino Bowl) um pouco maiores: largura x multiplicador, altura x metade do ganho. */
@Mixin(value = DinoBowlStructure.class, remap = false)
public abstract class DinoBowlStructureMixin {

    @Inject(method = "getWidthRadius", at = @At("RETURN"), cancellable = true)
    private void actcompat$wider(WorldgenRandom random, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) Math.round(cir.getReturnValue() * CompatConfig.primordialSizeMultiplier));
    }

    @Inject(method = "getHeightRadius", at = @At("RETURN"), cancellable = true)
    private void actcompat$taller(WorldgenRandom random, int seaLevel, CallbackInfoReturnable<Integer> cir) {
        double m = 1.0D + (CompatConfig.primordialSizeMultiplier - 1.0D) * 0.5D;
        cir.setReturnValue((int) Math.round(cir.getReturnValue() * m));
    }
}
