package com.ejs.endplus.features;

import java.util.List;

import com.ejs.endplus.EndPlus;

import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class EndTempleGenerator {
	private static final Identifier END_TEMPLE = new Identifier(EndPlus.MOD_ID, "end_temple");
	
	public static void addPieces(StructureManager manager, BlockPos pos, BlockRotation rotation, List<StructurePiece> pieces) {
		if(pos.getY()!=0 && pos.getY()>90 && pos.getZ()>90) {
		pieces.add(new EndTemplePiece(manager, pos, END_TEMPLE, rotation));
		}
	}
}	
