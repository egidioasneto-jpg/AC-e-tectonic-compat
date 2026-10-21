package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Leitura dos campos internos do mapa Voronoi do AC (para achar o anel em volta das zonas). */
@Mixin(value = ACBiomeRarity.class, remap = false)
public interface ACBiomeRarityAccessor {
    @Accessor("VORONOI_GENERATOR")
    static VoronoiGenerator actcompat$getVoronoi() { throw new AssertionError(); }

    @Accessor("NOISE_X")
    static PerlinSimplexNoise actcompat$getNoiseX() { throw new AssertionError(); }

    @Accessor("NOISE_Z")
    static PerlinSimplexNoise actcompat$getNoiseZ() { throw new AssertionError(); }

    @Accessor("biomeSize")
    static double actcompat$getBiomeSize() { throw new AssertionError(); }

    @Accessor("seperationDistance")
    static double actcompat$getSeparation() { throw new AssertionError(); }
}
