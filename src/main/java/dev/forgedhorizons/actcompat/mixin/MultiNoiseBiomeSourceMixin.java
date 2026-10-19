package dev.forgedhorizons.actcompat.mixin;

import dev.forgedhorizons.actcompat.ACBiomeResolver;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O Alex's Caves injeta no HEAD deste metodo e, quando acerta, cancela (retorna o bioma dele).
 * Nos entramos no RETURN: so rodamos quando o AC NAO devolveu bioma, e ai aplicamos
 * a regra baseada no Voronoi, que nao depende do clima remodelado pelo Tectonic.
 */
@Mixin(value = MultiNoiseBiomeSource.class, priority = 1500)
public abstract class MultiNoiseBiomeSourceMixin {

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void actcompat$fallbackToAlexsCaves(int x, int y, int z, Climate.Sampler sampler,
                                                CallbackInfoReturnable<Holder<Biome>> cir) {
        Holder<Biome> replacement = ACBiomeResolver.resolve(
                (BiomeSource) (Object) this, x, y, z, sampler, cir.getReturnValue());
        if (replacement != null) {
            cir.setReturnValue(replacement);
        }
    }
}
