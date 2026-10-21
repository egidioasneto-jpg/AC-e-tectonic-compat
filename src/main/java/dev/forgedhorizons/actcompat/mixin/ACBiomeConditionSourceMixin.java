package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.level.surface.ACSurfaceRuleConditionRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Pedras dos biomas (radrock, galena, guanostone, limestone, bolo...).
 *
 * O port troca a regra original "isBiome(...)" por uma checagem no mapa Voronoi que
 * PARA no Y 50. Tudo acima de Y 50 fica pedra comum, e as cavernas do AC sobem bem acima disso
 * (a parte de cima dos salões aparecia com pedra vanilla, minérios e geodos).
 *
 * Como este mod já faz o bioma real ser o do AC, voltamos à regra do Alex's Caves original:
 * converte a pedra onde o BIOMA é o do AC, em toda a altura do bioma.
 */
@Mixin(value = ACSurfaceRuleConditionRegistry.ACBiomeConditionSource.class, remap = false)
public abstract class ACBiomeConditionSourceMixin {

    @Inject(
            method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void actcompat$useRealBiome(SurfaceRules.Context context,
                                        CallbackInfoReturnable<SurfaceRules.Condition> cir) {
        int offset = ((ACSurfaceRuleConditionRegistry.ACBiomeConditionSource) (Object) this).rarityOffset();
        for (Map.Entry<ResourceKey<Biome>, BiomeGenerationNoiseCondition> e : BiomeGenerationConfig.getBiomesSnapshot().entrySet()) {
            if (e.getValue().getRarityOffset() == offset) {
                cir.setReturnValue(SurfaceRules.isBiome(e.getKey()).apply(context));
                return;
            }
        }
        // config ainda nao carregada: deixa o comportamento do port
    }
}
