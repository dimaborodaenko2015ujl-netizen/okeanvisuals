package com.okean.visuals.mixin;

import com.okean.visuals.OkeanHud;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides time-of-day ONLY for ClientWorld's local properties.
 * Does not alter server time, game rules or entity/server ticks.
 * Minecraft time: 06:00=0 ticks, 12:00=6000, 18:00=12000, 24:00=18000.
 */
@Mixin(ClientWorld.Properties.class)
public abstract class OkeanVisualTimeMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void okean$visualTime(CallbackInfoReturnable<Long> cir) {
        if (!OkeanHud.visualTimeEnabled) return;
        int hour = Math.max(1, Math.min(24, OkeanHud.visualTimeHour));
        // Visual-only 1..24h -> vanilla 0..23999 world ticks.
        long ticks = ((hour + 18) % 24) * 1000L;
        cir.setReturnValue(ticks);
    }
}
