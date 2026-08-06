package com.craftycorvid.improvedmaps;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.craftycorvid.improvedmaps.config.ModConfig.MinimapCorner;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_CONFIG;

public final class ImprovedMapsConfigScreen {
    public static Screen create(Screen parent) {
        Option<Boolean> minimapEnabled = Option.<Boolean>createBuilder()
                .name(Component.literal("Enable minimap"))
                .description(describe("Draws the Atlas's selected map as an overlay on the HUD."))
                .binding(true, () -> MOD_CONFIG.client_minimapEnabled,
                        v -> MOD_CONFIG.client_minimapEnabled = v)
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<MinimapCorner> minimapCorner = Option.<MinimapCorner>createBuilder()
                .name(Component.literal("Corner"))
                .description(describe("Screen corner the minimap is anchored to."))
                .binding(MinimapCorner.TOP_RIGHT, () -> MOD_CONFIG.client_minimapCorner,
                        v -> MOD_CONFIG.client_minimapCorner = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MinimapCorner.class))
                .build();

        Option<Integer> minimapSize = Option.<Integer>createBuilder()
                .name(Component.literal("Size"))
                .description(describe("Width and height of the minimap in screen pixels."))
                .binding(96, () -> MOD_CONFIG.client_minimapSize,
                        v -> MOD_CONFIG.client_minimapSize = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(16, 512).step(8))
                .build();

        Option<Boolean> showBiomeMapColors = Option.<Boolean>createBuilder()
                .name(Component.literal("Show biome map colours"))
                .description(describe(
                        "Tints map grass, leaves and water per biome, where the server has cached"
                                + " the biome data."))
                .binding(true, () -> MOD_CONFIG.client_showBiomeMapColors,
                        v -> MOD_CONFIG.client_showBiomeMapColors = v)
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<Integer> atlasMapCapacity = Option.<Integer>createBuilder()
                .name(Component.literal("Atlas map capacity"))
                .description(describe("Maximum number of filled maps an Atlas can hold."))
                .binding(512, () -> MOD_CONFIG.server_atlasMapCapacity,
                        v -> MOD_CONFIG.server_atlasMapCapacity = v)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(1))
                .build();

        Option<Boolean> updateWhenStowed = Option.<Boolean>createBuilder()
                .name(Component.literal("Update atlas when not in hand"))
                .description(describe("Keeps an Atlas's maps filling in while it is stowed."))
                .binding(true, () -> MOD_CONFIG.server_updateAtlasWhenNotInHand,
                        v -> MOD_CONFIG.server_updateAtlasWhenNotInHand = v)
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<Boolean> cacheBiomeMapColors = Option.<Boolean>createBuilder()
                .name(Component.literal("Cache biome map colours"))
                .description(describe(
                        "Stores the biome behind each map pixel and sends it to clients that can"
                                + " draw it."))
                .binding(true, () -> MOD_CONFIG.server_cacheBiomeMapColors,
                        v -> MOD_CONFIG.server_cacheBiomeMapColors = v)
                .controller(BooleanControllerBuilder::create)
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Improved Maps"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Client"))
                        .tooltip(Component.literal(
                                "The client_ settings, which apply wherever you play."))
                        .option(minimapEnabled)
                        .option(minimapCorner)
                        .option(minimapSize)
                        .option(showBiomeMapColors)
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Server"))
                        .tooltip(Component.literal(
                                "The server_ settings, which apply only in singleplayer or when you"
                                        + " host; on a server the server's own config is used."))
                        .option(atlasMapCapacity)
                        .option(updateWhenStowed)
                        .option(cacheBiomeMapColors)
                        .build())
                // Refresh after the bindings are written, not from an option listener - a listener
                // fires on the pending value, before MOD_CONFIG has been updated to match.
                .save(() -> {
                    MOD_CONFIG.saveConfig();
                    MapBiomeTints.refresh();
                })
                .build()
                .generateScreen(parent);
    }

    private static OptionDescription describe(String text) {
        return OptionDescription.of(Component.literal(text));
    }
}
