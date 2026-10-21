package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator;
import dev.forgedhorizons.actcompat.ACBiomeResolver;
import dev.forgedhorizons.actcompat.CompatConfig;
import dev.forgedhorizons.actcompat.CompatState;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quando o PROPRIO Alex's Caves escolhe o bioma (Magnetic e Forlorn aceitam qualquer continentalidade),
 * ele encerra o metodo antes do nosso codigo rodar. Por isso o filtro de oceano precisa entrar aqui,
 * na checagem do proprio AC: zona sob o oceano = condicao falha = AC nao escolhe.
 */
@Mixin(value = BiomeGenerationNoiseCondition.class, remap = false)
public abstract class BiomeGenerationNoiseConditionMixin {

    @Inject(
            method = "test(IIIFLnet/minecraft/world/level/biome/Climate$Sampler;Lnet/minecraft/resources/ResourceKey;Lcom/github/alexmodguy/alexscaves/server/misc/VoronoiGenerator$VoronoiInfo;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void actcompat$noOceanZones(int x, int y, int z, float unquantizedDepth, Climate.Sampler sampler,
                                        ResourceKey<Level> dimension, VoronoiGenerator.VoronoiInfo info,
                                        CallbackInfoReturnable<Boolean> cir) {
        long seed = CompatState.seed();
        if (seed == 0L || sampler == null) return;
        int blockX = QuartPos.toBlock(x);
        int blockZ = QuartPos.toBlock(z);
        ResourceKey<Biome> key = ACBiomeRarity.getACBiomeForPosition(seed, blockX, blockZ);
        if (key == null || key == ACBiomeRegistry.ABYSSAL_CHASM) return;
        float local = Climate.unquantizeCoord(sampler.sample(x, y, z).continentalness());
        if (local < CompatConfig.minLandContinentalness
                || !ACBiomeResolver.zoneAllowed(seed, key, blockX, blockZ, sampler, y)) {
            cir.setReturnValue(false);
        }
    }
}
