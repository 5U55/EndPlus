package com.ejs.endplus.registry;

import com.ejs.endplus.EndPlus;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {
	public static final SpawnEggItem TEST = new SpawnEggItem(EndPlus.ENDERMAN_BRUTE, 27923100, 27910024, new Item.Settings().group(ItemGroup.MISC));
	
	public static void registerItems() {
		Registry.register(Registry.ITEM, new Identifier(EndPlus.MOD_ID, "ender_brute_spawn_egg"), TEST);
	}
}
