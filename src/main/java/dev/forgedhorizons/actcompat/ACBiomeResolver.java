package dev.forgedhorizons.actcompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator;
import dev.forgedhorizons.actcompat.mixin.ACBiomeRarityAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Decide se um ponto (coordenadas "quart", 4x4x4 blocos) vira bioma do Alex's Caves
 * ou, no anel em volta das Primordial Caves, Lush Caves.
 *
 * Fonte da verdade: o mapa Voronoi do proprio AC, o mesmo que as estruturas de caverna usam.
 */
public final class ACBiomeResolver {
    private ACBiomeResolver() {}

    /** Cache: centro da zona -> zona permitida (centro em terra firme). */
    private static final Map<Long, Boolean> ZONE_ON_LAND = new ConcurrentHashMap<>();

    static void clearCache() {
        ZONE_ON_LAND.clear();
    }

    @Nullable
    public static Holder<Biome> resolve(BiomeSource source, int quartX, int quartY, int quartZ,
                                        Climate.Sampler sampler, @Nullable Holder<Biome> current,
                                        Supplier<Holder<Biome>> vanilla) {
        if (!CompatConfig.biomeFallback) return null;
        if (!CompatState.isOverworld(source)) return null;

        long seed = CompatState.seed();
        if (seed == 0L) return null;

        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        ResourceKey<Biome> key = ACBiomeRarity.getACBiomeForPosition(seed, blockX, blockZ);

        if (current != null && isAlexsCavesBiome(current)) {
            // O proprio AC escolheu (Magnetic/Forlorn aceitam qualquer continentalidade, ate oceano).
            // Se a zona estiver debaixo do mar, devolvemos o bioma vanilla daquele ponto.
            if (key != null && key != ACBiomeRegistry.ABYSSAL_CHASM) {
                float local = Climate.unquantizeCoord(sampler.sample(quartX, quartY, quartZ).continentalness());
                if (local < CompatConfig.minLandContinentalness || !zoneAllowed(seed, key, blockX, blockZ, sampler, quartY)) {
                    return vanilla.get();
                }
            }
            return null;
        }

        if (key == null) {
            return lushNearPrimordial(seed, quartX, quartY, quartZ, sampler, current);
        }

        BiomeGenerationNoiseCondition condition = BiomeGenerationConfig.getBiomesSnapshot().get(key);
        if (condition == null || condition.isDisabledCompletely()) return null;

        Climate.TargetPoint point = sampler.sample(quartX, quartY, quartZ);
        float depth = Climate.unquantizeCoord(point.depth());
        if (depth < CompatConfig.minDepth || depth > CompatConfig.maxDepth) return null;

        float localContinentalness = Climate.unquantizeCoord(point.continentalness());
        if (key == ACBiomeRegistry.ABYSSAL_CHASM) {
            if (localContinentalness > CompatConfig.abyssalMaxContinentalness) return null;
        } else {
            // Nada de bioma de caverna colado no fundo do mar: o ponto precisa estar sob terra
            // e o centro da zona tambem (senao a caverna inteira inunda).
            if (localContinentalness < CompatConfig.minLandContinentalness) return null;
            if (!zoneAllowed(seed, key, blockX, blockZ, sampler, quartY)) return null;
        }

        return CompatState.holder(key);
    }

    /**
     * Zona de AC (exceto Abyssal) so vale se o CENTRO dela estiver em terra.
     * Mesmo criterio do AC original (ele testa o clima no centro da zona).
     */
    public static boolean zoneAllowed(long seed, ResourceKey<Biome> key, int blockX, int blockZ,
                                      Climate.Sampler sampler, int quartY) {
        if (key == ACBiomeRegistry.ABYSSAL_CHASM) return true;
        Vec3 center = ACBiomeRarity.getACBiomeCenterForPosition(seed, blockX, blockZ);
        if (center == null) return false;
        int cx = (int) Math.floor(center.x);
        int cz = (int) Math.floor(center.z);
        long cacheKey = seed ^ ((long) cx * 73856093L) ^ ((long) cz * 19349669L);
        Boolean cached = ZONE_ON_LAND.get(cacheKey);
        if (cached != null) return cached;
        Climate.TargetPoint p = sampler.sample(cx, quartY, cz);
        boolean ok = Climate.unquantizeCoord(p.continentalness()) >= CompatConfig.minCenterContinentalness;
        if (ZONE_ON_LAND.size() > 20000) ZONE_ON_LAND.clear();
        ZONE_ON_LAND.put(cacheKey, ok);
        return ok;
    }

    /**
     * Anel em volta das Primordial Caves: aumenta a CHANCE de Lush Caves, em manchas.
     * So roda fora de qualquer zona do AC (key == null), entao nunca ocupa espaco das Primordial.
     */
    @Nullable
    private static Holder<Biome> lushNearPrimordial(long seed, int quartX, int quartY, int quartZ,
                                                    Climate.Sampler sampler, @Nullable Holder<Biome> current) {
        if (!CompatConfig.lushNearPrimordial || CompatConfig.lushChance <= 0) return null;
        Holder<Biome> lush = CompatState.lushCaves();
        if (lush == null) return null;
        if (current != null && (current.is(Biomes.DEEP_DARK) || current.is(Biomes.LUSH_CAVES))) return null;

        int blockY = QuartPos.toBlock(quartY);
        if (blockY > CompatConfig.lushMaxY) return null;

        double separation = ACBiomeRarityAccessor.actcompat$getSeparation();
        double biomeSize = ACBiomeRarityAccessor.actcompat$getBiomeSize();
        if (separation <= 0 || biomeSize <= 0) return null;

        // Mesma conta do ACBiomeRarity.getRareBiomeInfoForQuad, mas sem descartar quem esta FORA do bioma.
        double sampleX = quartX / separation;
        double sampleZ = quartZ / separation;
        double randomness = AlexsCaves.COMMON_CONFIG.caveBiomeWidthRandomness.get();
        double offX = randomness * ACBiomeRarityAccessor.actcompat$getNoiseX().getValue(sampleX, sampleZ, false);
        double offZ = randomness * ACBiomeRarityAccessor.actcompat$getNoiseZ().getValue(sampleX, sampleZ, false);
        VoronoiGenerator.VoronoiInfo info = ACBiomeRarityAccessor.actcompat$getVoronoi().get2(sampleX + offX, sampleZ + offZ, seed);

        double distQuart = info.distance() * separation;
        double ringQuart = CompatConfig.lushRingWidth / 4.0D;
        if (distQuart <= biomeSize || distQuart >= biomeSize + ringQuart) return null;

        int offset = ACBiomeRarity.getRareBiomeOffsetId(info);
        ResourceKey<Biome> neighbour = null;
        for (Map.Entry<ResourceKey<Biome>, BiomeGenerationNoiseCondition> e : BiomeGenerationConfig.getBiomesSnapshot().entrySet()) {
            if (e.getValue().getRarityOffset() == offset) {
                neighbour = e.getKey();
                break;
            }
        }
        if (neighbour != ACBiomeRegistry.PRIMORDIAL_CAVES) return null;

        Climate.TargetPoint point = sampler.sample(quartX, quartY, quartZ);
        if (Climate.unquantizeCoord(point.depth()) < CompatConfig.lushMinDepth) return null;
        if (Climate.unquantizeCoord(point.continentalness()) < CompatConfig.minLandContinentalness) return null;

        // Quanto mais perto da Primordial, maior a chance. Manchas grandes de ruido.
        double closeness = 1.0D - (distQuart - biomeSize) / ringQuart;
        double chance = CompatConfig.lushChance * closeness;
        float noise = ACMath.sampleNoise3D(QuartPos.toBlock(quartX), blockY, QuartPos.toBlock(quartZ), 48F);
        double roll = (noise + 1.0D) * 0.5D;
        return roll < chance ? lush : null;
    }

    public static boolean isAlexsCavesBiome(Holder<Biome> holder) {
        return holder.unwrapKey().map(k -> "alexscaves".equals(k.location().getNamespace())).orElse(false);
    }
}
