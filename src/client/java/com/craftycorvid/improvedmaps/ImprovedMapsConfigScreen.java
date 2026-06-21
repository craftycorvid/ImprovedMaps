package com.craftycorvid.improvedmaps;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
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
                .binding(false, () -> MOD_CONFIG.minimapEnabled, v -> MOD_CONFIG.minimapEnabled = v)
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<MinimapCorner> minimapCorner = Option.<MinimapCorner>createBuilder()
                .name(Component.literal("Corner"))
                .binding(MinimapCorner.TOP_RIGHT, () -> MOD_CONFIG.minimapCorner, v -> MOD_CONFIG.minimapCorner = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MinimapCorner.class))
                .build();

        Option<Integer> minimapSize = Option.<Integer>createBuilder()
                .name(Component.literal("Size"))
                .binding(128, () -> MOD_CONFIG.minimapSize, v -> MOD_CONFIG.minimapSize = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(16, 512).step(8))
                .build();

        Option<Integer> atlasMapCapacity = Option.<Integer>createBuilder()
                .name(Component.literal("Atlas map capacity"))
                .binding(512, () -> MOD_CONFIG.atlasMapCapacity, v -> MOD_CONFIG.atlasMapCapacity = v)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(1))
                .build();

        Option<Boolean> updateWhenStowed = Option.<Boolean>createBuilder()
                .name(Component.literal("Update atlas when not in hand"))
                .binding(true, () -> MOD_CONFIG.updateAtlasWhenNotInHand, v -> MOD_CONFIG.updateAtlasWhenNotInHand = v)
                .controller(BooleanControllerBuilder::create)
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Improved Maps"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Minimap"))
                        .option(minimapEnabled)
                        .option(minimapCorner)
                        .option(minimapSize)
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Atlas"))
                        .tooltip(Component.literal(
                                "Gameplay settings. They apply only in singleplayer or when you host;"
                                        + " on a server the server's own config is used."))
                        .option(atlasMapCapacity)
                        .option(updateWhenStowed)
                        .build())
                .save(MOD_CONFIG::saveConfig)
                .build()
                .generateScreen(parent);
    }
}
