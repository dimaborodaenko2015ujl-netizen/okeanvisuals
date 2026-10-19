package com.okean.visuals.mixin;

import com.okean.visuals.OkeanHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Records *actual mouse press events*, so fast CPS isn't capped at 20 game ticks.
@Mixin(Mouse.class)
public abstract class OkeanMouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void okean$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen == null
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            OkeanHud.recordLeftClick();
        }
    }
}
