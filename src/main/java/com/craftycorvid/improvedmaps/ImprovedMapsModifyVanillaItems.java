package com.craftycorvid.improvedmaps;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import com.google.common.collect.Lists;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;

public final class ImprovedMapsModifyVanillaItems {
    public static void initialize() {
        PolymerItemUtils.CONTEXT_ITEM_CHECK.register((itemStack, context) -> {
            return itemStack.is(Items.FILLED_MAP);
        });

        PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original, client, context) -> {
            // Add extra tooltip lines to filled maps
            if (original.is(Items.FILLED_MAP)) {
                ItemStack out = original.copy();
                List<Component> loreTexts = Lists.newArrayList();
                if (context.getPlayer() == null) {
                    return client;
                }
                ServerLevel world = context.getPlayer().level();
                MapItemSavedData mapState = MapItem.getSavedData(out, world);

                if (mapState != null) {
                    loreTexts.add(Component
                            .literal("Dimension " + ImprovedMapsUtils.formatDimensionString(
                                    mapState.dimension.location().toString()))
                            .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY)));
                    loreTexts.add(Component
                            .literal("Center " + mapState.centerX + ", " + mapState.centerZ)
                            .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY)));
                    loreTexts.add(Component
                            .literal("Scale "
                                    + ImprovedMapsUtils.scaleToString((int) mapState.scale))
                            .setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY)));

                    var loreComponent = new ItemLore(loreTexts);
                    out.applyComponents(DataComponentMap.builder()
                            .set(DataComponents.LORE, loreComponent).build());

                    return out;
                }
            }
            return client;
        });
    }

}
