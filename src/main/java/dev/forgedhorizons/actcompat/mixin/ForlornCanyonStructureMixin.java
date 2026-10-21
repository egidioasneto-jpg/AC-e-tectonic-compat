package dev.forgedhorizons.actcompat.mixin;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.structure.ForlornCanyonStructure;
import dev.forgedhorizons.actcompat.ACBiomeResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * O port so deixa o canion do Forlorn Hollows nascer a ate 3 chunks do CENTRO do bioma.
 * No original, cada chunk da grade dentro do bioma gera um canion, e eles se sobrepoem
 * ate esvaziar o bioma inteiro. Aqui voltamos a isso.
 */
@Mixin(value = ForlornCanyonStructure.class, remap = false)
public abstract class ForlornCanyonStructureMixin {

    @Inject(
            method = "findGenerationPoint(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void actcompat$generateAcrossWholeBiome(Structure.GenerationContext context,
                                                    CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        ResourceKey<Biome> biome = ACBiomeRarity.getACBiomeForPosition(context.seed(), x, z);
        if (biome == null || !biome.equals(ACBiomeRegistry.FORLORN_HOLLOWS)
                || !ACBiomeResolver.zoneAllowed(context.seed(), biome, x, z, context.randomState().sampler(), 0)) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        ForlornCanyonStructure self = (ForlornCanyonStructure) (Object) this;
        cir.setReturnValue(Optional.of(new Structure.GenerationStub(
                new BlockPos(x, ForlornCanyonStructure.BOWL_Y_CENTER, z),
                builder -> self.generatePieces(builder, context))));
    }
}
