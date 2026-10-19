package com.okean.visuals.mixin;

import com.okean.visuals.OkeanVisuals;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class OkeanHeldItemMixin {
    private static final String METHOD =
            "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;"
            + "Lnet/minecraft/item/ModelTransformationMode;Z"
            + "Lnet/minecraft/client/util/math/MatrixStack;"
            + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V";

    @Inject(method = METHOD, at = @At("HEAD"))
    private void okean$transformHeldItem(LivingEntity entity, ItemStack stack,
            ModelTransformationMode mode, boolean leftHanded, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (mode != ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
                && mode != ModelTransformationMode.FIRST_PERSON_LEFT_HAND) return;
        float time = System.currentTimeMillis() / 230.0f;
        float x = OkeanVisuals.itemX;
        float y = OkeanVisuals.itemY;
        float z = OkeanVisuals.itemZ;
        if (OkeanVisuals.handAnimation == 1) {
            y += (float)Math.sin(time) * 0.065f;  // Float
        } else if (OkeanVisuals.handAnimation == 2) {
            x += (float)Math.sin(time) * 0.065f;  // Sway
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    (float)Math.sin(time) * 3.5f));
        }
        matrices.translate(x, y, z);
        float scale = OkeanVisuals.itemScale;
        if (OkeanVisuals.itemEffect == 2) {
            scale *= 1.0f + (float)Math.sin(time * 1.7f) * 0.09f; // Pulse
        }
        matrices.scale(scale, scale, scale);
    }

    // Vanilla full-light mapping affects only the first-person item, not terrain or entities.
    @ModifyVariable(method = METHOD, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int okean$glowHeldItem(int light) {
        if (OkeanVisuals.itemEffect == 1 || OkeanVisuals.itemEffect == 2)
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        return light;
    }
}
