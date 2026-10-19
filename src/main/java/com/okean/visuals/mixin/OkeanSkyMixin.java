package com.okean.visuals.mixin;

import com.okean.visuals.OkeanHud;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Two optional sky color modes: vanilla and brighter ocean-blue tint.
// This adjusts sky color; it isn't a shader/resource-pack replacement.
@Mixin(ClientWorld.class)
public abstract class OkeanSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void okean$skyTint(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (OkeanHud.skyStyle == 0) return;
        Vec3d vanilla = cir.getReturnValue();
        if (vanilla == null) return;
        cir.setReturnValue(new Vec3d(
                Math.min(1.0, vanilla.x * 0.72 + 0.03),
                Math.min(1.0, vanilla.y * 0.94 + 0.025),
                Math.min(1.0, vanilla.z * 1.12 + 0.03)));
    }
}
