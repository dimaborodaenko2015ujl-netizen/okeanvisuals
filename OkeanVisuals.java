package com.okean.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class OkeanVisuals implements ClientModInitializer {
    public static boolean hudEnabled = true;
    public static boolean autoSprint = false;
    public static boolean fullbright = false;
    public static boolean aimbot = false;
    private boolean zoomEnabled = false;
    private float previousFov = 70.0f;

    @Override
    public void onInitializeClient() {
        System.out.println("Okean Visuals loaded!");

        KeyBinding menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.okeanvisuals.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.okeanvisuals"));
        KeyBinding zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.okeanvisuals.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C,
                "category.okeanvisuals"));

        HudRenderCallback.EVENT.register(this::renderHUD);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            if (autoSprint && client.player.forwardSpeed > 0 && !client.player.isSneaking()) {
                client.player.setSprinting(true);
            }

            client.options.getGamma().setValue(fullbright ? 100.0 : 0.5);

            while (menuKey.wasPressed()) {
                client.setScreen(new OkeanMenu());
            }

            if (zoomKey.isPressed()) {
                if (!zoomEnabled) {
                    previousFov = client.options.getFov().getValue();
                    zoomEnabled = true;
                }
                client.options.getFov().setValue(Math.max(10, previousFov * 0.35));
            } else if (zoomEnabled) {
                client.options.getFov().setValue(previousFov);
                zoomEnabled = false;
            }

            if (aimbot) {
                LivingEntity target = findTarget(client);
                if (target != null) aimAt(client, target);
            }
        });
    }

    private void renderHUD(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!hudEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = 10, y = 10, w = 180, h = 100;
        ctx.fill(x, y, x + w, y + h, 0xB4000014);
        ctx.drawBorder(x, y, w, h, 0xFF00D4FF);

        int tx = x + 8, ty = y + 10;
        ctx.drawText(client.textRenderer, "OKEAN VISUALS", tx, ty, 0xFF00D4FF, true);
        ty += 16;
        ctx.drawText(client.textRenderer,
                String.format("XYZ: %.1f / %.1f / %.1f", client.player.getX(), client.player.getY(), client.player.getZ()),
                tx, ty, 0xFF88CCFF, true);
        ty += 12;
        ctx.drawText(client.textRenderer, "FPS: " + client.getCurrentFps(), tx, ty, 0xFFFFFFFF, true);
        ty += 12;
        ctx.drawText(client.textRenderer, "Sprint: " + (autoSprint ? "ON" : "OFF"), tx, ty,
                autoSprint ? 0xFF44FF44 : 0xFFFF4444, true);
        ty += 12;
        ctx.drawText(client.textRenderer, "Aimbot: " + (aimbot ? "ON" : "OFF"), tx, ty,
                aimbot ? 0xFF44FF44 : 0xFFFF4444, true);
    }

    private LivingEntity findTarget(MinecraftClient client) {
        LivingEntity best = null;
        double bestDist = 30.0;
        for (LivingEntity entity : client.world.getEntitiesByClass(
                LivingEntity.class, client.player.getBoundingBox().expand(30),
                e -> e != client.player && e.isAlive())) {
            double distance = client.player.distanceTo(entity);
            if (distance < bestDist) {
                bestDist = distance;
                best = entity;
            }
        }
        return best;
    }

    private void aimAt(MinecraftClient client, LivingEntity target) {
        Vec3d diff = target.getPos().subtract(client.player.getEyePos());
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(
                Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
        client.player.setYaw(yaw);
        client.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    public static class OkeanMenu extends Screen {
        public OkeanMenu() {
            super(Text.literal("Okean Visuals"));
        }

        @Override
        protected void init() {
            int x = width / 2 - 80;
            int y = height / 2 - 90;
            int w = 160, h = 25, s = 8;

            addDrawableChild(ButtonWidget.builder(
                    Text.literal((autoSprint ? "[ON] " : "[OFF] ") + "Автоспринт"),
                    b -> { autoSprint = !autoSprint; clearAndInit(); })
                    .dimensions(x, y, w, h).build());

            y += h + s;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal((fullbright ? "[ON] " : "[OFF] ") + "Fullbright"),
                    b -> { fullbright = !fullbright; clearAndInit(); })
                    .dimensions(x, y, w, h).build());

            y += h + s;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal((aimbot ? "[ON] " : "[OFF] ") + "Aimbot"),
                    b -> { aimbot = !aimbot; clearAndInit(); })
                    .dimensions(x, y, w, h).build());

            y += h + s + 10;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Закрыть"), b -> close())
                    .dimensions(x, y, w, h).build());
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0x96000014);
            int mx = width / 2 - 200, my = height / 2 - 125;
            ctx.fill(mx, my, mx + 400, my + 250, 0xE60A1428);
            ctx.drawBorder(mx, my, 400, 250, 0xFF00D4FF);
            ctx.drawText(textRenderer, "OKEAN VISUALS", mx + 20, my + 12, 0xFF00D4FF, true);
            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override public boolean shouldPause() { return false; }
    }
}
