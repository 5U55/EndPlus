package com.ejs.endplus;

import com.ejs.endplus.entity.EndermanBruteEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;

public class EndPlusClient implements ClientModInitializer{

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.INSTANCE.register(EndPlus.ENDERMAN_BRUTE, (dispatcher, client) -> {
			return new EndermanBruteEntityRenderer(dispatcher);
		});
		
	}

}
