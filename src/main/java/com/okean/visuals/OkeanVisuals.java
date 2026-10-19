package com.okean.visuals;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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
    private int previousFov = 70;
    private boolean menuRequested = false;
    private boolean gammaOverrideActive = false;
    private double originalGamma = 0.5;

    @Override
    public void onInitializeClient() {

        System.out.println("🌊 Okean Visuals loaded!");

        // Клавиша меню — Right Shift
        KeyBinding menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.okeanvisuals.menu",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_SHIFT,
                        "category.okeanvisuals"
                )
        );

        // Клавиша зума — C
        KeyBinding zoomKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.okeanvisuals.zoom",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_C,
                        "category.okeanvisuals"
                )
        );

        // Toggle HUD with H
        KeyBinding hudKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.okeanvisuals.hud",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_H,
                        "category.okeanvisuals"
                )
        );

        // HUD
        HudRenderCallback.EVENT.register(this::renderHUD);

        // Команды
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> registerCommands(dispatcher)
        );

        // Игровой тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null || client.world == null) {
                if (zoomEnabled) {
                    client.options.getFov().setValue(previousFov);
                    zoomEnabled = false;
                }
                restoreGamma(client);
                return;
            }

            while (hudKey.wasPressed()) {
                hudEnabled = !hudEnabled;
            }

            if (menuRequested) {
                menuRequested = false;
                client.setScreen(new OkeanMenu());
            }

            // Автоспринт
            if (autoSprint
                    && client.player.forwardSpeed > 0
                    && !client.player.isSneaking()) {

                client.player.setSprinting(true);
            }

            // Preserve the user's original brightness when Fullbright is disabled.
            if (fullbright && !gammaOverrideActive) {
                originalGamma = client.options.getGamma().getValue();
                client.options.getGamma().setValue(100.0);
                gammaOverrideActive = true;
            } else if (!fullbright) {
                restoreGamma(client);
            }

            // Открытие меню через Right Shift
            while (menuKey.wasPressed()) {
                client.setScreen(new OkeanMenu());
            }

            // Zoom
            if (zoomKey.isPressed() && client.currentScreen == null) {

                if (!zoomEnabled) {
                    previousFov = client.options.getFov().getValue();
                    zoomEnabled = true;
                }

                client.options.getFov().setValue(
                        MathHelper.clamp((int) Math.round(previousFov * 0.35), 30, 110)
                );

            } else if (zoomEnabled) {

                client.options.getFov().setValue(previousFov);
                zoomEnabled = false;
            }

            // Aimbot
            if (aimbot) {

                LivingEntity target = findTarget(client);

                if (target != null) {
                    aimAt(client, target);
                }
            }
        });
    }

    // =========================
    // КОМАНДЫ
    // =========================

    private void registerCommands(
            CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher) {

        dispatcher.register(
                ClientCommandManager.literal("okean")
                        .executes(context -> {
                            menuRequested = true;
                            return 1;
                        })

                        // /okean menu
                        .then(
                                ClientCommandManager.literal("menu")
                                        .executes(context -> {

                                            menuRequested = true;

                                            return 1;
                                        })
                        )

                        // /okean sprint
                        .then(
                                ClientCommandManager.literal("sprint")
                                        .executes(context -> {

                                            autoSprint = !autoSprint;

                                            return 1;
                                        })
                        )

                        // /okean hud
                        .then(
                                ClientCommandManager.literal("hud")
                                        .executes(context -> {

                                            hudEnabled = !hudEnabled;

                                            return 1;
                                        })
                        )

                        // /okean bright
                        .then(
                                ClientCommandManager.literal("bright")
                                        .executes(context -> {

                                            fullbright = !fullbright;

                                            return 1;
                                        })
                        )

                        // /okean aimbot
                        .then(
                                ClientCommandManager.literal("aimbot")
                                        .executes(context -> {

                                            aimbot = !aimbot;

                                            return 1;
                                        })
                        )
        );
    }

    // =========================
    // HUD
    // =========================

    private void restoreGamma(MinecraftClient client) {
        if (gammaOverrideActive) {
            client.options.getGamma().setValue(originalGamma);
            gammaOverrideActive = false;
        }
    }

    // =========================
    // HUD
    // =========================

    private void renderHUD(
            DrawContext ctx,
            net.minecraft.client.render.RenderTickCounter tickCounter) {

        if (!hudEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        int x = 10;
        int y = 10;
        int w = 190;
        int h = 136;
        int tx = x + 8;
        int ty = y + 10;

        ctx.fill(x, y, x + w, y + h, 0xB4000014);
        ctx.drawBorder(x, y, w, h, 0xFF00D4FF);

        ctx.drawText(client.textRenderer, "OKEAN VISUALS 1.1", tx, ty, 0xFF00D4FF, true);
        ty += 16;

        ctx.drawText(
                client.textRenderer,
                String.format(java.util.Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
                        client.player.getX(), client.player.getY(), client.player.getZ()),
                tx, ty, 0xFF88CCFF, true);
        ty += 12;

        ctx.drawText(client.textRenderer, "FPS: " + client.getCurrentFps(),
                tx, ty, 0xFFFFFFFF, true);
        ty += 12;

        double blocksPerSecond = client.player.getVelocity().horizontalLength() * 20.0;
        ctx.drawText(
                client.textRenderer,
                String.format(java.util.Locale.ROOT, "Speed: %.1f blocks/s", blocksPerSecond),
                tx, ty, 0xFFFFFFFF, true);
        ty += 12;

        ctx.drawText(
                client.textRenderer,
                "Facing: " + client.player.getHorizontalFacing().asString().toUpperCase(java.util.Locale.ROOT),
                tx, ty, 0xFF88CCFF, true);
        ty += 12;

        ctx.drawText(client.textRenderer,
                "Sprint: " + (autoSprint ? "ON" : "OFF"),
                tx, ty, autoSprint ? 0xFF44FF44 : 0xFFFF4444, true);
        ty += 12;

        ctx.drawText(client.textRenderer,
                "Fullbright: " + (fullbright ? "ON" : "OFF"),
                tx, ty, fullbright ? 0xFF44FF44 : 0xFFFF4444, true);
        ty += 12;

        ctx.drawText(client.textRenderer,
                "Zoom (C): " + (zoomEnabled ? "ON" : "OFF"),
                tx, ty, zoomEnabled ? 0xFF44FF44 : 0xFFFF4444, true);
        ty += 12;

        ctx.drawText(client.textRenderer,
                "Aimbot: " + (aimbot ? "ON" : "OFF"),
                tx, ty, aimbot ? 0xFF44FF44 : 0xFFFF4444, true);
    }

    // =========================
    // AIMBOT
    // =========================

    private LivingEntity findTarget(MinecraftClient client) {

        LivingEntity best = null;
        double bestDist = 30.0;

        for (LivingEntity entity :
                client.world.getEntitiesByClass(
                        LivingEntity.class,
                        client.player.getBoundingBox().expand(30),
                        e -> e != client.player && e.isAlive()
                )) {

            double distance =
                    client.player.distanceTo(entity);

            if (distance < bestDist) {

                bestDist = distance;
                best = entity;
            }
        }

        return best;
    }

    private void aimAt(
            MinecraftClient client,
            LivingEntity target) {

        Vec3d diff =
                target.getPos()
                        .subtract(client.player.getEyePos());

        float yaw =
                (float) Math.toDegrees(
                        Math.atan2(diff.z, diff.x)
                ) - 90.0f;

        float pitch =
                (float) -Math.toDegrees(
                        Math.atan2(
                                diff.y,
                                Math.sqrt(
                                        diff.x * diff.x
                                                + diff.z * diff.z
                                )
                        )
                );

        client.player.setYaw(yaw);

        client.player.setPitch(
                MathHelper.clamp(
                        pitch,
                        -90.0f,
                        90.0f
                )
        );
    }

    // =========================
    // МЕНЮ
    // =========================

    public static class OkeanMenu extends Screen {

        public OkeanMenu() {
            super(Text.literal("Okean Visuals"));
        }

        @Override
        protected void init() {

            int x = width / 2 - 80;
            int y = height / 2 - 90;

            int w = 160;
            int h = 25;
            int s = 8;

            // Show/hide the HUD
            addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal((hudEnabled ? "[ON] " : "[OFF] ") + "HUD (H)"),
                            button -> {
                                hudEnabled = !hudEnabled;
                                clearAndInit();
                            }
                    )
                    .dimensions(x, y, w, h)
                    .build()
            );

            y += h + s;

            // Автоспринт
            addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal(
                                    (autoSprint ? "[ON] " : "[OFF] ")
                                            + "Автоспринт"
                            ),
                            button -> {

                                autoSprint = !autoSprint;

                                clearAndInit();
                            }
                    )
                    .dimensions(x, y, w, h)
                    .build()
            );

            y += h + s;

            // Fullbright
            addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal(
                                    (fullbright ? "[ON] " : "[OFF] ")
                                            + "Fullbright"
                            ),
                            button -> {

                                fullbright = !fullbright;

                                clearAndInit();
                            }
                    )
                    .dimensions(x, y, w, h)
                    .build()
            );

            y += h + s;

            // Aimbot
            addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal(
                                    (aimbot ? "[ON] " : "[OFF] ")
                                            + "Aimbot"
                            ),
                            button -> {

                                aimbot = !aimbot;

                                clearAndInit();
                            }
                    )
                    .dimensions(x, y, w, h)
                    .build()
            );

            y += h + s + 10;

            // Закрыть
            addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal("Закрыть"),
                            button -> close()
                    )
                    .dimensions(x, y, w, h)
                    .build()
            );
        }

        @Override
        public void render(
                DrawContext ctx,
                int mouseX,
                int mouseY,
                float delta) {

            // Затемнение
            ctx.fill(
                    0,
                    0,
                    width,
                    height,
                    0x96000014
            );

            // Окно
            int mx = width / 2 - 200;
            int my = height / 2 - 125;

            ctx.fill(
                    mx,
                    my,
                    mx + 400,
                    my + 250,
                    0xE60A1428
            );

            // Рамка
            ctx.drawBorder(
                    mx,
                    my,
                    400,
                    250,
                    0xFF00D4FF
            );

            // Заголовок
            ctx.drawText(
                    textRenderer,
                    "OKEAN VISUALS",
                    mx + 20,
                    my + 12,
                    0xFF00D4FF,
                    true
            );

            super.render(
                    ctx,
                    mouseX,
                    mouseY,
                    delta
            );
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
