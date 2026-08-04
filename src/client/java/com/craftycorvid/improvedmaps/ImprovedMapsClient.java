package com.craftycorvid.improvedmaps;

import org.lwjgl.glfw.GLFW;
import com.craftycorvid.improvedmaps.ImprovedMapsNetworking.AtlasMapCenters;
import com.mojang.blaze3d.platform.InputConstants;
import eu.pb4.polymer.networking.api.client.PolymerClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.nbt.IntTag;

public class ImprovedMapsClient implements ClientModInitializer {
	public static final KeyMapping OPEN_ATLAS = new KeyMapping("key.improved-maps.open_atlas",
			InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M,
			KeyMapping.Category.register(ImprovedMaps.id("atlas")));

	@Override
	public void onInitializeClient() {
		PolymerClientNetworking.setClientMetadata(ImprovedMaps.HELLO_PACKET, IntTag.valueOf(1));

		KeyMappingHelper.registerKeyMapping(OPEN_ATLAS);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_ATLAS.consumeClick()) {
				if (client.gui.screen() == null && client.player != null
						&& MinimapHud.resolveAtlas(client.player) != null)
					client.setScreenAndShow(new AtlasScreen());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(AtlasMapCenters.TYPE,
				(payload, context) -> AtlasScreen.cacheCenters(payload.centers()));
		ClientPlayConnectionEvents.DISCONNECT
				.register((handler, client) -> AtlasScreen.forgetCenters());
		HudElementRegistry.addLast(ImprovedMaps.id("minimap"), MinimapHud::render);

		// Status effect icons share the minimap's top-right corner: slide them clear.
		// (Toasts get the same treatment in ToastManagerMixin.)
		HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS, vanilla -> (graphics, delta) -> {
			int inset = MinimapHud.rightInset();
			if (inset == 0) {
				vanilla.extractRenderState(graphics, delta);
				return;
			}
			graphics.pose().pushMatrix();
			graphics.pose().translate(-inset, 0f);
			vanilla.extractRenderState(graphics, delta);
			graphics.pose().popMatrix();
		});

		// Render an atlas's bundle tooltip with a capacity-scaled fullness bar.
		ClientTooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof AtlasTooltipData atlas) {
				ClientBundleTooltip tooltip = new ClientBundleTooltip(atlas.contents());
				((AtlasFullnessHolder) tooltip).improvedmaps$setFullness(atlas.fullness());
				return tooltip;
			}
			return null;
		});
	}
}
