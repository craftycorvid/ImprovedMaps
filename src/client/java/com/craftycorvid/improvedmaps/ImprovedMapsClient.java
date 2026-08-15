package com.craftycorvid.improvedmaps;

import org.lwjgl.glfw.GLFW;
import com.craftycorvid.improvedmaps.ImprovedMapsNetworking.AtlasMapCenters;
import com.craftycorvid.improvedmaps.ImprovedMapsNetworking.ClientReady;
import com.craftycorvid.improvedmaps.ImprovedMapsNetworking.MapBiomesPayload;
import com.mojang.blaze3d.platform.InputConstants;
import eu.pb4.polymer.core.api.client.PolymerClientUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class ImprovedMapsClient implements ClientModInitializer {
	public static final KeyMapping OPEN_ATLAS = new KeyMapping("key.improved-maps.open_atlas",
			InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M,
			KeyMapping.Category.register(ImprovedMaps.id("atlas")));

	@Override
	public void onInitializeClient() {
		// Any earlier and the server puts our item id inside the sync packet, which the client
		// cannot resolve until it has read it. Fires on the netty thread, before the player exists.
		PolymerClientUtils.ON_SYNC_FINISHED.register(() -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				if (client.getConnection() != null)
					ClientPlayNetworking.send(ClientReady.INSTANCE);
			});
		});

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
		ClientPlayNetworking.registerGlobalReceiver(MapBiomesPayload.TYPE,
				(payload, context) -> MapBiomeTints.accept(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			AtlasScreen.forgetCenters();
			MapBiomeTints.forget();
		});

		// Biome tints are read out of the pack's colormaps, so a pack swap invalidates both the
		// cached tints and every map texture already built from them.
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
				.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return ImprovedMaps.id("map_biome_tints");
					}

					@Override
					public void onResourceManagerReload(ResourceManager manager) {
						MapBiomeTints.resourcesReloaded();
					}
				});
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
