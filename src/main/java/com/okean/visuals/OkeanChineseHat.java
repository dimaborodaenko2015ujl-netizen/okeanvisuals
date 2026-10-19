package com.okean.visuals;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Simple untextured conical Asian-style straw hat.
 * Purely cosmetic and client-side: only the local player sees it.
 */
public final class OkeanChineseHat {
    private OkeanChineseHat() {}

    public static void render(WorldRenderContext context) {
        if (!OkeanPlusMini.chineseHat || context.consumers() == null
                || context.matrixStack() == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.player.isInvisible()
                || client.options.getPerspective().isFirstPerson()) return;

        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        VertexConsumer quads = context.consumers().getBuffer(RenderLayer.getDebugQuads());

        matrices.push();
        matrices.translate(client.player.getX() - camera.x,
                client.player.getY() - camera.y + client.player.getHeight() + 0.025,
                client.player.getZ() - camera.z);

        // A low 32-piece cone, a visible wide brim and a subtle dark underside.
        final int segments = 32;
        final float r = 0.55f;
        final float centerY = 0.28f;
        int tint = OkeanPlusMini.hatColor();
        for (int i = 0; i < segments; ++i) {
            double a = 2.0 * Math.PI * i / segments;
            double b = 2.0 * Math.PI * (i + 1) / segments;
            float x1 = (float) Math.cos(a) * r;
            float z1 = (float) Math.sin(a) * r;
            float x2 = (float) Math.cos(b) * r;
            float z2 = (float) Math.sin(b) * r;
            int shaded = darken(tint, 1f - (i % 4) * 0.045f);
            int brim = darken(tint, 1.12f);
            int underside = darken(tint, 0.53f);
            quad(quads, matrices,
                    0, centerY, 0, x1, 0, z1,
                    x2, 0, z2, x2, 0, z2,
                    (shaded >>> 16) & 255, (shaded >>> 8) & 255, shaded & 255);
            // Duplicate reversed winding so the cone stays visible from both sides.
            quad(quads, matrices,
                    0, centerY, 0, x2, 0, z2,
                    x1, 0, z1, x1, 0, z1,
                    (shaded >>> 16) & 255, (shaded >>> 8) & 255, shaded & 255);
            // Flat rim: subtly turquoise edge on the hat.
            float innerR = 0.47f;
            float ix1 = (float)Math.cos(a) * innerR;
            float iz1 = (float)Math.sin(a) * innerR;
            float ix2 = (float)Math.cos(b) * innerR;
            float iz2 = (float)Math.sin(b) * innerR;
            quad(quads, matrices, ix1, 0.006f, iz1, x1, 0.006f, z1,
                    x2, 0.006f, z2, ix2, 0.006f, iz2,
                    (brim >>> 16) & 255, (brim >>> 8) & 255, brim & 255);
            // Shaded underside of the straw hat.
            quad(quads, matrices, 0, -0.005f, 0, x2, -0.005f, z2,
                    x1, -0.005f, z1, x1, -0.005f, z1,
                    (underside >>> 16) & 255, (underside >>> 8) & 255, underside & 255);
        }
        matrices.pop();
    }

    private static int darken(int color, float amount) {
        int r = Math.min(255, Math.max(0, Math.round(((color >>> 16) & 255) * amount)));
        int g = Math.min(255, Math.max(0, Math.round(((color >>> 8) & 255) * amount)));
        int b = Math.min(255, Math.max(0, Math.round((color & 255) * amount)));
        return (r << 16) | (g << 8) | b;
    }

    private static void quad(VertexConsumer vertices, MatrixStack matrices,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            int r, int g, int b) {
        vertices.vertex(matrices.peek(), ax, ay, az).color(r, g, b, 255);
        vertices.vertex(matrices.peek(), bx, by, bz).color(r, g, b, 255);
        vertices.vertex(matrices.peek(), cx, cy, cz).color(r, g, b, 255);
        vertices.vertex(matrices.peek(), dx, dy, dz).color(r, g, b, 255);
    }
}
