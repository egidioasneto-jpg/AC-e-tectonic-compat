package dev.forgedhorizons.actcompat;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/ac_tectonic_compat-common.toml
 * Valores copiados para campos estaticos volateis, porque sao lidos no caminho quente da geracao.
 */
public final class CompatConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue BIOME_FALLBACK = B
            .comment("Coloca os biomas do Alex's Caves onde o mapa Voronoi do proprio Alex's Caves manda,",
                     "mesmo quando as faixas de clima (continentalidade/temperatura) falham por causa do Tectonic.")
            .define("biome_fallback", true);

    private static final ModConfigSpec.DoubleValue MIN_DEPTH = B
            .comment("Profundidade minima (ruido 'depth') para o bioma aparecer. 0.05 = padrao do Alex's Caves (logo abaixo da superficie).")
            .defineInRange("min_depth", 0.05D, -2.0D, 2.0D);

    private static final ModConfigSpec.DoubleValue MAX_DEPTH = B
            .comment("Profundidade maxima. Tectonic limita 'depth' a 1.3, entao 2.0 nao corta nada.")
            .defineInRange("max_depth", 2.0D, -2.0D, 4.0D);

    private static final ModConfigSpec.DoubleValue ABYSSAL_MAX_CONTINENTALNESS = B
            .comment("O Abyssal Chasm so aparece sob oceano. Continentalidade local precisa ser menor que isto.",
                     "Alex's Caves usa -0.5; -0.19 e o limite de oceano vanilla (mais permissivo para os oceanos do Tectonic).")
            .defineInRange("abyssal_max_continentalness", -0.19D, -1.2D, 1.2D);

    private static final ModConfigSpec.DoubleValue MIN_LAND_CONTINENTALNESS = B
            .comment("Biomas do AC (exceto Abyssal Chasm) nao aparecem onde a continentalidade local e menor que isto.",
                     "-0.11 = fim da costa no vanilla (abaixo disso e oceano). Evita caverna colada no fundo do mar.")
            .defineInRange("min_land_continentalness", -0.11D, -1.2D, 1.2D);

    private static final ModConfigSpec.DoubleValue MIN_CENTER_CONTINENTALNESS = B
            .comment("O CENTRO da zona do AC precisa ter pelo menos esta continentalidade (terra firme).",
                     "Valores maiores = zonas mais para dentro do continente, longe do mar. O AC original usa 0.4-0.5 para Primordial/Toxic/Candy.")
            .defineInRange("min_center_continentalness", 0.05D, -1.2D, 1.2D);

    private static final ModConfigSpec.BooleanValue LUSH_NEAR_PRIMORDIAL = B
            .comment("Aumenta a chance de Lush Caves num anel em volta das Primordial Caves (nunca dentro delas).")
            .define("lush_near_primordial", true);

    private static final ModConfigSpec.IntValue LUSH_RING_WIDTH = B
            .comment("Largura (blocos) do anel em volta da Primordial onde as Lush Caves ficam mais comuns.")
            .defineInRange("lush_ring_width", 160, 16, 1024);

    private static final ModConfigSpec.DoubleValue LUSH_CHANCE = B
            .comment("Chance maxima (colado na Primordial) de um ponto do anel virar Lush Caves. Cai ate 0 na borda do anel.")
            .defineInRange("lush_chance", 0.45D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue LUSH_MIN_DEPTH = B
            .comment("Profundidade minima para as Lush extras (0.2 ~ 25 blocos abaixo da superficie).")
            .defineInRange("lush_min_depth", 0.2D, 0.0D, 2.0D);

    private static final ModConfigSpec.IntValue LUSH_MAX_Y = B
            .defineInRange("lush_max_y", 48, -64, 512);

    private static final ModConfigSpec.DoubleValue PRIMORDIAL_SIZE = B
            .comment("Multiplicador do tamanho da caverna das Primordial Caves (largura; a altura cresce metade disso). 1.0 = original.")
            .defineInRange("primordial_size_multiplier", 1.2D, 0.5D, 2.0D);

    private static final ModConfigSpec.DoubleValue CANDY_SIZE = B
            .comment("Multiplicador do tamanho da caverna da Candy Cavity (largura; a altura cresce metade disso). 1.0 = original.")
            .defineInRange("candy_size_multiplier", 1.2D, 0.5D, 2.0D);

    private static final ModConfigSpec.DoubleValue MAGNETIC_OPENNESS = B
            .comment("Quao 'aberta' fica a Magnetic Cave: 0 = original (muitas cascas e bolas de pedra no ar), 1 = quase sem.")
            .defineInRange("magnetic_openness", 0.6D, 0.0D, 1.0D);

    private static final ModConfigSpec.BooleanValue PROTECT_AC_CAVES = B
            .comment("Bloqueia cavernas (cheese/noodle), rios subterraneos e tuneis de lava do Tectonic dentro das zonas",
                     "do Alex's Caves, para que o formato das cavernas do AC nao seja cortado.")
            .define("protect_ac_caves", true);

    private static final ModConfigSpec.IntValue ZONE_MIN_Y = B
            .comment("Faixa vertical (Y) onde a protecao vale.")
            .defineInRange("zone_min_y", -64, -2048, 2048);

    private static final ModConfigSpec.IntValue ZONE_MAX_Y = B
            .defineInRange("zone_max_y", 72, -2048, 2048);

    public static final ModConfigSpec SPEC = B.build();

    public static volatile boolean biomeFallback = true;
    public static volatile float minDepth = 0.05F;
    public static volatile float maxDepth = 2.0F;
    public static volatile float abyssalMaxContinentalness = -0.19F;
    public static volatile float minLandContinentalness = -0.11F;
    public static volatile float minCenterContinentalness = 0.05F;
    public static volatile boolean lushNearPrimordial = true;
    public static volatile int lushRingWidth = 160;
    public static volatile double lushChance = 0.45D;
    public static volatile double lushMinDepth = 0.2D;
    public static volatile int lushMaxY = 48;
    public static volatile double primordialSizeMultiplier = 1.2D;
    public static volatile double candySizeMultiplier = 1.2D;
    public static volatile double magneticOpenness = 0.6D;
    public static volatile boolean protectAcCaves = true;
    public static volatile int zoneMinY = -64;
    public static volatile int zoneMaxY = 72;

    private CompatConfig() {}

    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    private static void bake() {
        biomeFallback = BIOME_FALLBACK.get();
        minDepth = MIN_DEPTH.get().floatValue();
        maxDepth = MAX_DEPTH.get().floatValue();
        abyssalMaxContinentalness = ABYSSAL_MAX_CONTINENTALNESS.get().floatValue();
        minLandContinentalness = MIN_LAND_CONTINENTALNESS.get().floatValue();
        minCenterContinentalness = MIN_CENTER_CONTINENTALNESS.get().floatValue();
        lushNearPrimordial = LUSH_NEAR_PRIMORDIAL.get();
        lushRingWidth = LUSH_RING_WIDTH.get();
        lushChance = LUSH_CHANCE.get();
        lushMinDepth = LUSH_MIN_DEPTH.get();
        lushMaxY = LUSH_MAX_Y.get();
        primordialSizeMultiplier = PRIMORDIAL_SIZE.get();
        candySizeMultiplier = CANDY_SIZE.get();
        magneticOpenness = MAGNETIC_OPENNESS.get();
        ACBiomeResolver.clearCache();
        protectAcCaves = PROTECT_AC_CAVES.get();
        zoneMinY = ZONE_MIN_Y.get();
        zoneMaxY = ZONE_MAX_Y.get();
    }
}
