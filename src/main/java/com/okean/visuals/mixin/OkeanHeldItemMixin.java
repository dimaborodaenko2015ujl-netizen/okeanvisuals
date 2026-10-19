package com.okean.visuals.mixin;

import com.okean.visuals.OkeanVisuals;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Transform first-person hands/items, not the world. Free effect: Glow only. */
@Mixin(HeldItemRenderer.class)
public abstract class OkeanHeldItemMixin {
    private static final String ITEM_RENDER =
            "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;"
            + "Lnet/minecraft/item/ModelTransformationMode;Z"
            + "Lnet/minecraft/client/util/math/MatrixStack;"
            + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V";

    // Apply to the parent first-person render matrix, before Vanilla pushes its own.
    // This reliably moves the actual first-person hand as well as its held item.
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void okean$firstPersonMotion(AbstractClientPlayerEntity player, float tickDelta,
            float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider providers, int light, CallbackInfo ci) {
        matrices.push();
        if (stack.isEmpty()) return;
        float seconds = System.currentTimeMillis() / 1000.0f;
        float x = OkeanVisuals.itemX;
        float y = OkeanVisuals.itemY;
        float z = OkeanVisuals.itemZ;

        if (OkeanVisuals.handAnimation == 1) { // Float up/down.
            y += (float)Math.sin(seconds * 2.3f) * 0.12f;
        } else if (OkeanVisuals.handAnimation == 2) { // Sway side to side.
            x += (float)Math.sin(seconds * 2.3f) * 0.12f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    (float)Math.sin(seconds * 2.3f) * 6.0f));
        }
        matrices.translate(x, y, z);
        matrices.scale(OkeanVisuals.itemScale, OkeanVisuals.itemScale, OkeanVisuals.itemScale);
    }

    // Each hand must restore the caller's matrix, or the second hand accumulates offsets.
    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void okean$restoreFirstPersonMatrix(AbstractClientPlayerEntity player, float tickDelta,
            float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider providers, int light, CallbackInfo ci) {
        matrices.pop();
    }

    // Full-bright lighting is the only Free item effect; no global world shaders.
    @ModifyVariable(method = ITEM_RENDER, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int okean$glowHeldItem(int light) {
        return OkeanVisuals.itemEffect == 1
                ? LightmapTextureManager.MAX_LIGHT_COORDINATE
                : light;
    }
}
