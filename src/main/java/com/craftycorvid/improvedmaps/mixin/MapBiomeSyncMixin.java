package com.craftycorvid.improvedmaps.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.craftycorvid.improvedmaps.ImprovedMapsNetworking;

@Mixin(MapItemSavedData.class)
public class MapBiomeSyncMixin {
    // The one choke point every map packet passes through - held maps and item frames alike, since
    // ServerEntity uses it for frames. Pushing the biomes from here means there is no request packet
    // to validate: by construction a player is only told a map's biomes when the server has already
    // decided to send them that map's pixels.
    @Inject(method = "getUpdatePacket", at = @At("RETURN"))
    private void improvedmaps$sendBiomes(MapId id, Player player,
            CallbackInfoReturnable<Packet<?>> cir) {
        if (cir.getReturnValue() != null && player instanceof ServerPlayer serverPlayer)
            ImprovedMapsNetworking.sendBiomes(serverPlayer, id);
    }
}
