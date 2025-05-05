package com.craftycorvid.improvedmaps.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.craftycorvid.improvedmaps.ImprovedMapsUtils;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Mixin(FilledMapItem.class)
public class FilledMapItemMixin extends Item {
    public FilledMapItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "createMap", at = @At("TAIL"))
    private static void addLoreOnCreateMap(ServerWorld world, int x, int z, byte scale,
            boolean showIcons, boolean unlimitedTracking, CallbackInfoReturnable<ItemStack> ci,
            @Local ItemStack stack, @Local MapIdComponent mapIdComponent) {
        var mapState = world.getMapState(mapIdComponent);
        if (mapState != null) {
            addLore(mapState, stack);
        }
    }

    @Inject(method = "scale", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;set(Lnet/minecraft/component/ComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static void addLoreOnScale(ItemStack map, ServerWorld world, CallbackInfo ci,
            @Local MapIdComponent mapIdComponent) {
        var mapState = world.getMapState(mapIdComponent);
        if (mapState != null) {
            addLore(mapState, map);
        }
    }

    @Inject(method = "copyMap", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;set(Lnet/minecraft/component/ComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static void addLoreOnCopy(ItemStack map, ServerWorld world, CallbackInfo ci,
            @Local MapIdComponent mapIdComponent) {
        var mapState = world.getMapState(mapIdComponent);
        if (mapState != null) {
            addLore(mapState, map);
        }
    }

    private static void addLore(MapState mapState, ItemStack stack) {
        List<Text> loreTexts = Lists.newArrayList();

        loreTexts.add(Text
                .literal("Dimension " + ImprovedMapsUtils
                        .formatDimensionString(mapState.dimension.getValue().toString()))
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
        loreTexts.add(Text.literal("Center " + mapState.centerX + ", " + mapState.centerZ)
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));
        loreTexts.add(Text.literal("Scale " + ImprovedMapsUtils.scaleToString((int) mapState.scale))
                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GRAY)));

        var loreComponent = new LoreComponent(loreTexts);
        stack.applyComponentsFrom(
                ComponentMap.builder().add(DataComponentTypes.LORE, loreComponent).build());
    }
}
