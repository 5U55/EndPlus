package com.ejs.endplus;

import com.ejs.endplus.entity.EndermanBruteEntity;
import com.ejs.endplus.features.DragonSkeleton;
import com.ejs.endplus.features.DragonSkeletonPiece;
import com.ejs.endplus.features.EndTemple;
import com.ejs.endplus.features.EndTemplePiece;
import com.ejs.endplus.registry.ModItems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.TheEndBiomes;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.structure.v1.FabricStructureBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.surfacebuilder.ConfiguredSurfaceBuilder;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import net.minecraft.world.gen.surfacebuilder.TernarySurfaceConfig;

@SuppressWarnings("deprecation")
public class EndPlus implements ModInitializer{

	public static final String MOD_ID = "endplus";
	
	public static final ConfiguredSurfaceBuilder<TernarySurfaceConfig> END_DECAY_BUILDER = SurfaceBuilder.DEFAULT
			.withConfig(new TernarySurfaceConfig(
					Blocks.BLACKSTONE.getDefaultState(),
					Blocks.POLISHED_BLACKSTONE.getDefaultState(),
					Blocks.GRAVEL.getDefaultState()
					));
	public static final Biome END_DECAY = createEndDecay();
	
	private static Biome createEndDecay() {
		SpawnSettings.Builder spawnSettings = new SpawnSettings.Builder();
		DefaultBiomeFeatures.addBats(spawnSettings);
		DefaultBiomeFeatures.addEndMobs(spawnSettings);
		
		GenerationSettings.Builder generationSettings = new GenerationSettings.Builder();
		generationSettings.surfaceBuilder(END_DECAY_BUILDER);
		DefaultBiomeFeatures.addDesertDeadBushes(generationSettings);
		
		return (new Biome.Builder())
				.precipitation(Biome.Precipitation.NONE)
				.category(Biome.Category.THEEND)
				.depth(0.125F)
				.scale(0.5F)
				.temperature(0.4F)
				.downfall(0.4F)
				.effects((new BiomeEffects.Builder())
						.waterColor(0xC833FF)
						.waterFogColor(0x42246D)
						.fogColor(0xC833FF)
						.skyColor(0x000000)
						.build())
				.spawnSettings(spawnSettings.build())
				.generationSettings(generationSettings.build())
				.build();
	}
	
	public static final RegistryKey<Biome> END_DECAY_KEY = RegistryKey.of(Registry.BIOME_KEY, new Identifier(MOD_ID, "end_decay"));
	
	public static final StructurePieceType DRAGON_SKELETON = DragonSkeletonPiece::new;
	private static final StructureFeature<DefaultFeatureConfig> DRAGON_SKELETON_STRUCTURE  = new DragonSkeleton(DefaultFeatureConfig.CODEC);
	private static final ConfiguredStructureFeature<?, ?> DRAGON_SKELETON_CONFIGURED = DRAGON_SKELETON_STRUCTURE.configure(DefaultFeatureConfig.DEFAULT);
	
	public static final StructurePieceType END_TEMPLE = EndTemplePiece::new;
	private static final StructureFeature<DefaultFeatureConfig> END_TEMPLE_STRUCTURE  = new EndTemple(DefaultFeatureConfig.CODEC);
	private static final ConfiguredStructureFeature<?, ?> END_TEMPLE_CONFIGURED = END_TEMPLE_STRUCTURE.configure(DefaultFeatureConfig.DEFAULT);

	public static final EntityType<EndermanBruteEntity> ENDERMAN_BRUTE = Registry.register(Registry.ENTITY_TYPE, 
			new Identifier(MOD_ID, "enderman_brute"), 
			FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, EndermanBruteEntity::new).dimensions(EntityDimensions.fixed(1f, 3f)).build());
	
	@Override
	public void onInitialize() {
		ModItems.registerItems();
		
		Registry.register(BuiltinRegistries.CONFIGURED_SURFACE_BUILDER, new Identifier(MOD_ID, "end_decay_surface_builder"), END_DECAY_BUILDER);
		Registry.register(BuiltinRegistries.BIOME, END_DECAY_KEY.getValue(), END_DECAY);
		
		TheEndBiomes.addHighlandsBiome(END_DECAY_KEY, 2D);
		TheEndBiomes.addSmallIslandsBiome(END_DECAY_KEY, 1D);
		
		Registry.register(Registry.STRUCTURE_PIECE, new Identifier(MOD_ID, "dragon_skeleton_piece"), DRAGON_SKELETON);
		FabricStructureBuilder.create(new Identifier(MOD_ID, "dragon_skeleton"), DRAGON_SKELETON_STRUCTURE)
			.step(GenerationStep.Feature.SURFACE_STRUCTURES)
			.defaultConfig(32, 8, 12345)
			.adjustsSurface()
			.register();
		
		Registry.register(Registry.STRUCTURE_PIECE, new Identifier(MOD_ID, "end_temple_piece"), END_TEMPLE);
		FabricStructureBuilder.create(new Identifier(MOD_ID, "end_temple"), END_TEMPLE_STRUCTURE)
			.step(GenerationStep.Feature.SURFACE_STRUCTURES)
			.defaultConfig(100, 8, 23457)
			.adjustsSurface()
			.register();
		
		RegistryKey<ConfiguredStructureFeature<?, ?>> endTempleConfigured = RegistryKey.of(Registry.CONFIGURED_STRUCTURE_FEATURE_WORLDGEN, new Identifier(MOD_ID, "end_temple"));
		BuiltinRegistries.add(BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE, endTempleConfigured.getValue() , END_TEMPLE_CONFIGURED);
		BiomeModifications.addStructure(BiomeSelectors.foundInTheEnd(), endTempleConfigured);
		
		RegistryKey<ConfiguredStructureFeature<?, ?>> dragonSkeletonConfigured = RegistryKey.of(Registry.CONFIGURED_STRUCTURE_FEATURE_WORLDGEN, new Identifier(MOD_ID, "dragon_skeleton"));
		BuiltinRegistries.add(BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE, dragonSkeletonConfigured.getValue() , DRAGON_SKELETON_CONFIGURED);
		BiomeModifications.addStructure(BiomeSelectors.foundInTheEnd(), dragonSkeletonConfigured);
	
		FabricDefaultAttributeRegistry.register(ENDERMAN_BRUTE, EndermanBruteEntity.createEndermanAttributes());
	}

}