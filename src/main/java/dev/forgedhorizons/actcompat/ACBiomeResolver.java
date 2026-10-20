package dev.forgedhorizons.actcompat;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import javax.annotation.Nullable;

/**
 * Decide se um ponto (em coordenadas "quart", 4x4x4 blocos) deve ser um bioma do Alex's Caves.
 *
 * Fonte da verdade: o mapa Voronoi do proprio AC (ACBiomeRarity.getACBiomeForPosition).
 * E exatamente o mesmo mapa que as estruturas de caverna do AC (Ferrocave, DinoBowl, AcidPit...)
 * usam para decidir onde e qual tamanho esculpir. Assim bioma e formato da caverna ficam alinhados,
 * independente de como o Tectonic remodelou continentalidade/erosao.
 */
public final class ACBiomeResolver {
    private ACBiomeResolver() {}

    @Nullable
    public static Holder<Biome> resolve(BiomeSource source, int quartX, int quartY, int quartZ,
                                        Climate.Sampler sampler, @Nullable Holder<Biome> current) {
        if (!CompatConfig.biomeFallback) return null;
        if (!CompatState.isOverworld(source)) return null;
        if (current != null && isAlexsCavesBiome(current)) return null; // o AC ja resolveu sozinho

        long seed = CompatState.seed();
        if (seed == 0L) return null;

        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        ResourceKey<Biome> key = ACBiomeRarity.getACBiomeForPosition(seed, blockX, blockZ);
        if (key == null) return null;

        BiomeGenerationNoiseCondition condition = BiomeGenerationConfig.getBiomesSnapshot().get(key);
        if (condition == null || condition.isDisabledCompletely()) return null;

        Climate.TargetPoint point = sampler.sample(quartX, quartY, quartZ);
        float depth = Climate.unquantizeCoord(point.depth());
        if (depth < CompatConfig.minDepth || depth > CompatConfig.maxDepth) return null;

        if (key == ACBiomeRegistry.ABYSSAL_CHASM) {
            float continentalness = Climate.unquantizeCoord(point.continentalness());
            if (continentalness > CompatConfig.abyssalMaxContinentalness) return null;
        }

        return CompatState.holder(key);
    }

    public static boolean isAlexsCavesBiome(Holder<Biome> holder) {
        return holder.unwrapKey().map(k -> "alexscaves".equals(k.location().getNamespace())).orElse(false);
    }
}
