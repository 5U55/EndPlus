package com.ejs.endplus.features;

import java.util.List;
import java.util.Random;

import com.ejs.endplus.EndPlus;

import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RuinedTowerGenerator {
	private static final Identifier RUINED_TOWER_1 = new Identifier(EndPlus.MOD_ID, "ruined_tower");
	private static final Identifier RUINED_TOWER_2 = new Identifier(EndPlus.MOD_ID, "ruined_tower_2");
	private static final Identifier RUINED_TOWER_3 = new Identifier(EndPlus.MOD_ID, "ruined_tower_3");
	private static final Identifier RUINED_TOWER_4 = new Identifier(EndPlus.MOD_ID, "ruined_tower_4");

	private static final Identifier[] TOWER_IDS = { RUINED_TOWER_1, RUINED_TOWER_2, RUINED_TOWER_3, RUINED_TOWER_4 };

	public static void addPieces(StructureManager manager, BlockPos pos, BlockRotation rotation,
			List<StructurePiece> pieces) {
		if (pos.getY() != 0) {

			/*
			 * for(int i=0; i<4; i++) { int j = MathHelper.floor(2.0D * Math.cos(2.0D *
			 * (-3.141592653589793D + 0.3141592653589793D * (double)i))); int k =
			 * MathHelper.floor(2.0D * Math.sin(2.0D * (-3.141592653589793D +
			 * 0.3141592653589793D * (double)i))); BlockPos newpos= new BlockPos.Mutable(j,
			 * pos.getY()-1, k);
			 */
			int rand = new Random().nextInt(4);
			pieces.add(new RuinedTowerPiece(manager, pos, TOWER_IDS[rand], rotation));
			// }
		}
	}
}
