package dev.forgedhorizons.actcompat;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.BiomeSourceAccessor;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado por servidor: seed do mundo, quais BiomeSource sao do Overworld
 * e os Holders dos biomas do Alex's Caves.
 *
 * Diferente do Alex's Caves, nao dependemos de "qual dimensao foi amostrada por ultimo"
 * (um estatico global que pode estar errado em outra thread): marcamos a instancia
 * exata do BiomeSource do Overworld.
 */
public final class CompatState {
    private static volatile long seed = 0L;
    private static final Set<BiomeSource> OVERWORLD_SOURCES =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final Map<ResourceKey<Biome>, Holder<Biome>> AC_HOLDERS = new ConcurrentHashMap<>();
    private static volatile Holder<Biome> lushCaves = null;
    private static volatile net.minecraft.world.level.biome.Climate.Sampler overworldSampler = null;

    public static net.minecraft.world.level.biome.Climate.Sampler overworldSampler() {
        return overworldSampler;
    }

    static void setOverworldSampler(net.minecraft.world.level.biome.Climate.Sampler sampler) {
        overworldSampler = sampler;
    }

    private CompatState() {}

    public static long seed() {
        return seed;
    }

    public static boolean isOverworld(BiomeSource source) {
        return OVERWORLD_SOURCES.contains(source);
    }

    public static Holder<Biome> lushCaves() {
        return lushCaves;
    }

    public static Holder<Biome> holder(ResourceKey<Biome> key) {
        return AC_HOLDERS.get(key);
    }

    static void init(long worldSeed, Registry<Biome> biomes) {
        seed = worldSeed;
        AC_HOLDERS.clear();
        lushCaves = biomes.getHolder(net.minecraft.world.level.biome.Biomes.LUSH_CAVES).orElse(null);
        ACBiomeResolver.clearCache();
        for (ResourceKey<Biome> key : ACBiomeRegistry.ALEXS_CAVES_BIOMES) {
            biomes.getHolder(key).ifPresent(h -> AC_HOLDERS.put(key, h));
        }
    }

    /**
     * Marca o BiomeSource como Overworld e garante que os biomas do AC estejam em possibleBiomes().
     * Sem isso, o ChunkGeneratorStructureState descarta as estruturas que esculpem as cavernas do AC
     * e o /locate biome nem procura por eles.
     */
    static void registerOverworldSource(BiomeSource source) {
        OVERWORLD_SOURCES.add(source);
        if (source instanceof BiomeSourceAccessor accessor && !AC_HOLDERS.isEmpty()) {
            accessor.expandBiomesWith(ImmutableSet.copyOf(AC_HOLDERS.values()));
        }
    }

    static boolean sourceHasAllAcBiomes(BiomeSource source) {
        Set<Holder<Biome>> possible = source.possibleBiomes();
        for (Holder<Biome> h : AC_HOLDERS.values()) {
            if (!possible.contains(h)) return false;
        }
        return !AC_HOLDERS.isEmpty();
    }

    static void reset() {
        seed = 0L;
        lushCaves = null;
        overworldSampler = null;
        ACBiomeResolver.clearCache();
        OVERWORLD_SOURCES.clear();
        AC_HOLDERS.clear();
    }
}
