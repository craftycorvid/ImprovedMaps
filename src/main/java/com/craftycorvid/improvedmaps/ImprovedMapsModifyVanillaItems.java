package com.craftycorvid.improvedmaps;

import java.util.List;
import com.google.common.collect.Lists;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public final class ImprovedMapsModifyVanillaItems {
    public static void initialize() {
        PolymerItemUtils.ITEM_CHECK.register((itemStack) -> {
            return itemStack.isOf(Items.FILLED_MAP);
        });

        PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, context) -> {
            // Add extra tooltip lines to filled maps
            if (original.isOf(Items.FILLED_MAP)) {
                ItemStack out = original.copy();
                List<Text> loreTexts = Lists.newArrayList();
                World world = context.getPlayer().getWorld();
                MapState mapState = FilledMapItem.getMapState(out, world);

                loreTexts.add(Text
                        .literal("Dimension " + ImprovedMapsUtils
                                .formatDimensionString(mapState.dimension.getValue().toString()))
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
                loreTexts.add(Text.literal("Center " + mapState.centerX + ", " + mapState.centerZ)
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
                loreTexts.add(Text
                        .literal("Scale " + ImprovedMapsUtils.scaleToString((int) mapState.scale))
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));

                var loreComponent = new LoreComponent(loreTexts);
                out.applyComponentsFrom(
                        ComponentMap.builder().add(DataComponentTypes.LORE, loreComponent).build());

                return out;
            }
            return client;
        });
    }

}
