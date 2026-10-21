package dev.forgedhorizons.actcompat;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function "ac_tectonic_compat:ac_zone":
 *   1.0 dentro de uma zona de caverna do Alex's Caves (na faixa Y configurada), 0.0 fora.
 *
 * Os worldgen_modifiers do Lithostitched (data/ac_tectonic_compat/lithostitched/worldgen_modifier)
 * usam isto para "desligar" as cavernas, rios subterraneos e tuneis de lava do Tectonic
 * dentro das zonas do AC. A estrutura do AC entao esculpe o formato original sem buracos extras.
 *
 * O Abyssal Chasm fica de fora: ele so existe sob oceano e o Ocean Trench ja corta fundo.
 */
public record ACZoneDensityFunction() implements DensityFunction.SimpleFunction {
    public static final ACZoneDensityFunction INSTANCE = new ACZoneDensityFunction();
    public static final KeyDispatchDataCodec<ACZoneDensityFunction> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

    @Override
    public double compute(FunctionContext context) {
        if (!CompatConfig.protectAcCaves) return 0.0D;
        int y = context.blockY();
        if (y < CompatConfig.zoneMinY || y > CompatConfig.zoneMaxY) return 0.0D;

        long seed = CompatState.seed();
        if (seed == 0L) return 0.0D;

        ResourceKey<Biome> key = ACBiomeRarity.getACBiomeForPosition(seed, context.blockX(), context.blockZ());
        if (key == null || key == ACBiomeRegistry.ABYSSAL_CHASM) return 0.0D;

        BiomeGenerationNoiseCondition condition = BiomeGenerationConfig.getBiomesSnapshot().get(key);
        if (condition == null || condition.isDisabledCompletely()) return 0.0D;
        // Zona vetada por estar sob o oceano: nao ha caverna do AC ali, entao nao bloqueia as do Tectonic.
        net.minecraft.world.level.biome.Climate.Sampler sampler = CompatState.overworldSampler();
        if (sampler != null && !ACBiomeResolver.zoneAllowed(seed, key, context.blockX(), context.blockZ(), sampler, 0)) {
            return 0.0D;
        }
        return 1.0D;
    }

    @Override
    public double minValue() {
        return 0.0D;
    }

    @Override
    public double maxValue() {
        return 1.0D;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
