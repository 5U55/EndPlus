package com.ejs.endplus.features;

import com.mojang.serialization.Codec;

import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

public class RuinedTower extends StructureFeature<DefaultFeatureConfig>{

	public RuinedTower(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public StructureStartFactory<DefaultFeatureConfig> getStructureStartFactory() {
		return Start::new;
	}

	public static class Start extends StructureStart<DefaultFeatureConfig> {
		public Start(StructureFeature<DefaultFeatureConfig> feature, int chunkX, int chunkZ, BlockBox box, int references, long seed) {
			super(feature, chunkX, chunkZ, box, references, seed);
		}

		@Override
		public void init(DynamicRegistryManager registryManager, ChunkGenerator chunkGenerator,
				StructureManager manager, int chunkX, int chunkZ, Biome biome, DefaultFeatureConfig config) {
			int x = chunkX * 16;
			int z = chunkZ * 16;
			int y = chunkGenerator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG);
		/*	int islandEndPtX = 0;
			while(chunkGenerator.getHeight(islandEndPtX, z, Heightmap.Type.WORLD_SURFACE_WG)>0) {
				islandEndPtX++;
			}
			int islandEndPtX2 = 0;
			while(chunkGenerator.getHeight(islandEndPtX2, z, Heightmap.Type.WORLD_SURFACE_WG)>0) {
				islandEndPtX2--;
			}
			int islandEndPtZ = 0;
			while(chunkGenerator.getHeight(x, islandEndPtZ, Heightmap.Type.WORLD_SURFACE_WG)>0) {
				islandEndPtZ++;
			}
			int islandEndPtZ2 = 0;
			while(chunkGenerator.getHeight(x, islandEndPtZ2, Heightmap.Type.WORLD_SURFACE_WG)>0) {
				islandEndPtZ2--;
			}
			x=(islandEndPtX +islandEndPtX2)/2;
			z=(islandEndPtZ +islandEndPtZ2)/2;*/
			BlockPos pos = new BlockPos(x, y, z);
			BlockRotation rotation = BlockRotation.random(this.random);
			RuinedTowerGenerator.addPieces(manager, pos, rotation, this.children);
			this.setBoundingBoxFromChildren();
		}
	}
}
