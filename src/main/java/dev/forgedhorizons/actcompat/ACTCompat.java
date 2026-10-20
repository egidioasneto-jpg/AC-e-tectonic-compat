package dev.forgedhorizons.actcompat;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ACTCompat.MODID)
public class ACTCompat {
    public static final String MODID = "ac_tectonic_compat";
    public static final Logger LOGGER = LoggerFactory.getLogger("AC-Tectonic-Compat");

    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MODID);

    static {
        // Usado pelos worldgen_modifiers do Lithostitched como "ac_tectonic_compat:ac_zone"
        DENSITY_FUNCTION_TYPES.register("ac_zone", () -> ACZoneDensityFunction.CODEC.codec());
    }

    public ACTCompat(IEventBus modBus, ModContainer container) {
        DENSITY_FUNCTION_TYPES.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, CompatConfig.SPEC);
        modBus.addListener(CompatConfig::onLoad);
        modBus.addListener(CompatConfig::onReload);
        NeoForge.EVENT_BUS.register(new ServerEvents());
    }
}
