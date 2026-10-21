package dev.forgedhorizons.actcompat;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServerEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        long seed = server.getWorldData().worldGenOptions().seed();
        CompatState.reset();
        CompatState.init(seed, server.registryAccess().registryOrThrow(Registries.BIOME));

        LevelStem overworld = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
        if (overworld != null) {
            BiomeSource source = overworld.generator().getBiomeSource();
            CompatState.registerOverworldSource(source);
            ACTCompat.LOGGER.info("Overworld BiomeSource registrado ({}), seed {}", source.getClass().getSimpleName(), seed);
        } else {
            ACTCompat.LOGGER.warn("Nao encontrei o LevelStem do Overworld; o fallback de biomas ficara inativo.");
        }
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        CompatState.registerOverworldSource(source); // caso algum mod tenha trocado o gerador
        CompatState.setOverworldSampler(level.getChunkSource().randomState().sampler());
        boolean biomesOk = CompatState.sourceHasAllAcBiomes(source);
        boolean structuresOk = hasAlexsCavesStructureSets(level);
        if (biomesOk && structuresOk) {
            ACTCompat.LOGGER.info("OK: biomas e estruturas de caverna do Alex's Caves ativos no Overworld.");
        } else {
            ACTCompat.LOGGER.warn("Problema: biomas AC em possibleBiomes={} | estruturas AC no StructureState={}. "
                    + "Se estruturas=false, as cavernas do AC nao serao esculpidas neste mundo.", biomesOk, structuresOk);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        CompatState.reset();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    // ---------------------------------------------------------------- comandos

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("actcompat")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("why")
                        .then(Commands.argument("biome", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(ctx -> why(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "biome")))))
                .then(Commands.literal("find")
                        .executes(ctx -> find(ctx.getSource(), 4000))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(256, 30000))
                                .executes(ctx -> find(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    /** Mostra tudo que decide se este ponto vira bioma do AC. */
    private static int info(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        long seed = CompatState.seed();
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();

        int qx = QuartPos.fromBlock(pos.getX()), qy = QuartPos.fromBlock(pos.getY()), qz = QuartPos.fromBlock(pos.getZ());
        Climate.TargetPoint p = sampler.sample(qx, qy, qz);
        ResourceKey<Biome> voronoi = seed == 0 ? null : ACBiomeRarity.getACBiomeForPosition(seed, pos.getX(), pos.getZ());
        Holder<Biome> biomeHere = source.getNoiseBiome(qx, qy, qz, sampler);

        send(src, "§6[AC x Tectonic] §f" + pos.toShortString());
        send(src, " seed conhecido: " + (seed != 0) + " | BiomeSource do Overworld registrado: " + CompatState.isOverworld(source));
        send(src, " biomas AC em possibleBiomes: " + CompatState.sourceHasAllAcBiomes(source)
                + " | estruturas AC ativas: " + hasAlexsCavesStructureSets(level));
        send(src, " Voronoi do AC neste X/Z: " + (voronoi == null ? "nenhum" : voronoi.location()));
        send(src, String.format(" clima: cont=%.3f eros=%.3f depth=%.3f temp=%.3f",
                Climate.unquantizeCoord(p.continentalness()), Climate.unquantizeCoord(p.erosion()),
                Climate.unquantizeCoord(p.depth()), Climate.unquantizeCoord(p.temperature())));
        send(src, String.format(" correcoes rodando -> Forlorn: %d inicios / %d blocos com formato original | Magnetic aberta: %d",
                CompatStats.FORLORN_STARTS.get(), CompatStats.FORLORN_ORIGINAL_SHAPE.get(), CompatStats.MAGNETIC_OPEN_SHAPE.get()));
        send(src, " getNoiseBiome aqui: " + biomeHere.unwrapKey().map(k -> k.location().toString()).orElse("?"));
        return 1;
    }

    /** Procura no mapa Voronoi (sem gerar chunks) o ponto mais proximo de cada bioma do AC. */
    /**
     * Diagnostico: para um bioma do AC (ex.: magnetic_caves), conta as zonas num raio de 6000 blocos
     * e mostra por que cada uma foi aceita ou descartada.
     */
    private static int why(CommandSourceStack src, String biomeName) {
        long seed = CompatState.seed();
        if (seed == 0) {
            send(src, "§cSeed ainda nao inicializado.");
            return 0;
        }
        Climate.Sampler sampler = src.getLevel().getChunkSource().randomState().sampler();
        BlockPos origin = BlockPos.containing(src.getPosition());
        int radius = 6000, step = 48;
        java.util.Set<Long> seen = new java.util.HashSet<>();
        int zones = 0, oceanCenter = 0, oceanEdge = 0, ok = 0;
        BlockPos nearestOk = null;
        long nearestD2 = Long.MAX_VALUE;
        boolean edgeWas = CompatConfig.zoneEdgeCheck;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                ResourceKey<Biome> key = ACBiomeRarity.getACBiomeForPosition(seed, x, z);
                if (key == null || !key.location().getPath().equals(biomeName)) continue;
                net.minecraft.world.phys.Vec3 c = ACBiomeRarity.getACBiomeCenterForPosition(seed, x, z);
                if (c == null) continue;
                long id = ((long) Math.floor(c.x) << 32) ^ ((long) Math.floor(c.z) & 0xffffffffL);
                if (!seen.add(id)) continue;
                zones++;
                int cbx = (int) Math.floor(c.x) * 4, cbz = (int) Math.floor(c.z) * 4;
                float cc = Climate.unquantizeCoord(sampler.sample((int) Math.floor(c.x), 0, (int) Math.floor(c.z)).continentalness());
                if (cc < CompatConfig.minCenterContinentalness) { oceanCenter++; continue; }
                ACBiomeResolver.clearCache();
                if (!ACBiomeResolver.zoneAllowed(seed, key, cbx, cbz, sampler, 0)) { oceanEdge++; continue; }
                ok++;
                long d2 = (long) (cbx - origin.getX()) * (cbx - origin.getX()) + (long) (cbz - origin.getZ()) * (cbz - origin.getZ());
                if (d2 < nearestD2) { nearestD2 = d2; nearestOk = new BlockPos(cbx, 0, cbz); }
            }
        }
        for (var e : com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig.getBiomesSnapshot().entrySet()) {
            if (e.getKey().location().getPath().equals(biomeName)) {
                send(src, " config do AC: desativado=" + e.getValue().isDisabledCompletely()
                        + " | distancia do spawn=" + e.getValue().getDistanceFromSpawn()
                        + " | rarity_offset=" + e.getValue().getRarityOffset());
            }
        }
        if (biomeName.equals("magnetic_caves") && CompatConfig.magneticAllowOcean) {
            send(src, " (magnetic_allow_ocean=true: zonas sob o mar NAO sao descartadas; viram Magnetic profunda abaixo de Y "
                    + CompatConfig.magneticOceanMaxY + ")");
        }
        send(src, "§6[why " + biomeName + "] raio 6000: zonas=" + zones + " | centro no mar=" + oceanCenter
                + " | borda no mar=" + oceanEdge + " (edge_check=" + edgeWas + ") | aceitas=" + ok);
        if (nearestOk != null) {
            send(src, " mais proxima aceita: " + nearestOk.getX() + " ~ " + nearestOk.getZ() + " (va para Y ~0)");
        }
        return ok;
    }

    private static int find(CommandSourceStack src, int radius) {
        long seed = CompatState.seed();
        if (seed == 0) {
            send(src, "§cSeed ainda nao inicializado.");
            return 0;
        }
        BlockPos origin = BlockPos.containing(src.getPosition());
        Climate.Sampler sampler = src.getLevel().getChunkSource().randomState().sampler();
        Map<ResourceKey<Biome>, BlockPos> nearest = new LinkedHashMap<>();
        Map<ResourceKey<Biome>, Long> best = new LinkedHashMap<>();
        int step = 32;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                ResourceKey<Biome> key = ACBiomeRarity.getACBiomeForPosition(seed, x, z);
                if (key == null) continue;
                if (!ACBiomeResolver.zoneAllowed(seed, key, x, z, sampler, QuartPos.fromBlock(0))
                        && !ACBiomeResolver.isMagneticOceanZone(seed, x, z, sampler)) continue; // zona sob o oceano (Magnetic profunda conta)
                long d2 = (long) dx * dx + (long) dz * dz;
                Long prev = best.get(key);
                if (prev == null || d2 < prev) {
                    best.put(key, d2);
                    nearest.put(key, new BlockPos(x, 0, z));
                }
            }
        }
        if (nearest.isEmpty()) {
            send(src, "§eNenhuma zona do Alex's Caves num raio de " + radius + " blocos.");
            return 0;
        }
        send(src, "§6Zonas do Alex's Caves mais proximas (Y ~0, va para baixo; zonas sob o oceano sao ignoradas):");
        nearest.forEach((key, p) -> send(src, String.format(" %s -> %d ~ %d  (%d blocos)",
                key.location().getPath(), p.getX(), p.getZ(), (int) Math.sqrt(best.get(key)))));
        return nearest.size();
    }

    private static boolean hasAlexsCavesStructureSets(ServerLevel level) {
        List<Holder<StructureSet>> sets = level.getChunkSource().getGeneratorState().possibleStructureSets();
        for (Holder<StructureSet> set : sets) {
            if (set.unwrapKey().map(k -> "alexscaves".equals(k.location().getNamespace())).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    private static void send(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg), false);
    }
}
