package com.ejs.endplus.features;

import java.util.List;
import java.util.Random;

import com.ejs.endplus.EndPlus;

import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class RuinedTowerGenerator {
	private static final Identifier EXIT_PORTAL = new Identifier(EndPlus.MOD_ID, "ruined_exit_portal");
	private static final Identifier RUINED_TOWER_1 = new Identifier(EndPlus.MOD_ID, "ruined_tower");
	private static final Identifier RUINED_TOWER_2 = new Identifier(EndPlus.MOD_ID, "ruined_tower_2");
	private static final Identifier RUINED_TOWER_3 = new Identifier(EndPlus.MOD_ID, "ruined_tower_3");
	private static final Identifier RUINED_TOWER_4 = new Identifier(EndPlus.MOD_ID, "ruined_tower_4");

	private static final Identifier[] TOWER_IDS = { RUINED_TOWER_1, RUINED_TOWER_2, RUINED_TOWER_3, RUINED_TOWER_4 };

	public static void addPieces(StructureManager manager, BlockPos pos, BlockRotation rotation,
			List<StructurePiece> pieces) {
		if (pos.getY() != 0) {
			pieces.add(new RuinedTowerPiece(manager, pos, EXIT_PORTAL, rotation));
			for (int i = 0; i < 4; i++) {
				int j = MathHelper
						.floor(42.0D * Math.cos(18*i));
				int k = MathHelper
						.floor(42.0D * Math.sin(18*i));
				BlockPos newpos = new BlockPos.Mutable(j+pos.getX(), pos.getY() - 1, k+pos.getZ());

				int rand = new Random().nextInt(4);
				pieces.add(new RuinedTowerPiece(manager, newpos, TOWER_IDS[rand], rotation));
			}
		}
	}
}
