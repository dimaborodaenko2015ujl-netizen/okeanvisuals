package com.okean.visuals.mixin;

import com.okean.visuals.OkeanHud;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ClientWorld#getSkyColor returns an integer ARGB/RGB color in Minecraft 1.21.4.
 * Treating it as Vec3d caused ClassCastException when the sky theme was enabled.
 */
@Mixin(ClientWorld.class)
public abstract class OkeanSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void okean$skyTint(Vec3d cameraPos, float tickDelta,
                               CallbackInfoReturnable<Integer> cir) {
        if (OkeanHud.skyStyle == 0) return; // Keep vanilla untouched.

        int vanilla = cir.getReturnValue();
        int r = (vanilla >>> 16) & 0xFF;
        int g = (vanilla >>> 8) & 0xFF;
        int b = vanilla & 0xFF;

        // A gentle blue tint; preserve existing transparency/alpha.
        int red = Math.min(255, (int) Math.round(r * 0.72 + 8));
        int green = Math.min(255, (int) Math.round(g * 0.94 + 6));
        int blue = Math.min(255, (int) Math.round(b * 1.12 + 8));

        cir.setReturnValue((vanilla & 0xFF000000) | (red << 16) | (green << 8) | blue);
    }
}
