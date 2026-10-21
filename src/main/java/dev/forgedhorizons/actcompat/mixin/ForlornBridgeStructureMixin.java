package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.structure.ForlornBridgeStructure;
import dev.forgedhorizons.actcompat.ACBiomeResolver;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** A ponte do Forlorn ignora a checagem de bioma do jogo; aqui ela respeita o filtro de oceano. */
@Mixin(value = ForlornBridgeStructure.class, remap = false)
public abstract class ForlornBridgeStructureMixin {

    @Inject(
            method = "findGenerationPoint(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void actcompat$skipOceanZones(Structure.GenerationContext context,
                                          CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        ResourceKey<Biome> biome = ACBiomeRarity.getACBiomeForPosition(context.seed(), x, z);
        if (biome != null && !ACBiomeResolver.zoneAllowed(context.seed(), biome, x, z, context.randomState().sampler(), 0)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
