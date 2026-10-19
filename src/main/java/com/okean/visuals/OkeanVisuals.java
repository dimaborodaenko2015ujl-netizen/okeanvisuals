package com.okean.visuals;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class OkeanVisuals implements ClientModInitializer {

    public static boolean hudEnabled = true;
    public static boolean autoSprint = false;
    public static boolean fullbright = false;
    private boolean zoomEnabled = false;
    private int previousFov = 70;
    private boolean menuRequested = false;
    private boolean gammaOverridden = false;
    private double storedGamma = 0.5;
    private boolean appliedNightVision = false;
    public static boolean showRings = true;
    // First-person hand position and motion only; no shaders or item lighting effects.
    public static float itemX = 0.0f, itemY = 0.0f, itemZ = 0.0f;
    public static float itemScale = 1.0f;
    public static int handAnimation = 0; // 0=normal, 1=float, 2=sway


    @Override
    public void onInitializeClient() {

        System.out.println("🌊 Okean Visuals loaded!");
        OkeanMenu.loadSettings();

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

        KeyBinding hudKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.okeanvisuals.hud", InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_H, "category.okeanvisuals")
        );

        // HUD and world-space target indicators
        HudRenderCallback.EVENT.register(OkeanHud::render);
        WorldRenderEvents.AFTER_ENTITIES.register(this::renderTargetRings);
        WorldRenderEvents.AFTER_ENTITIES.register(OkeanChineseHat::render);

        // Команды
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> registerCommands(dispatcher)
        );

        // Игровой тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null || client.world == null) {
                appliedNightVision = false;
                restoreGamma(client);
                if (zoomEnabled) {
                    client.options.getFov().setValue(previousFov);
                    zoomEnabled = false;
                }
                return;
            }

            while (hudKey.wasPressed()) {
                hudEnabled = !hudEnabled;
                OkeanMenu.saveSettings();
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

            // Night Vision is actually visible with Minecraft 1.21.4's modern lightmap.
            // Only remove the effect if this mod itself added it.
            if (fullbright) {
                if (!client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    client.player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NIGHT_VISION, 300, 0, true, false, false));
                    appliedNightVision = true;
                }
            } else if (appliedNightVision) {
                client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                appliedNightVision = false;
            }
            if (gammaOverridden) restoreGamma(client);

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

        });
    }

    private void restoreGamma(MinecraftClient client) {
        if (gammaOverridden) {
            client.options.getGamma().setValue(storedGamma);
            gammaOverridden = false;
        }
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
                                            OkeanMenu.saveSettings();
                                            return 1;
                                        })
                        )

                        // /okean hud
                        .then(
                                ClientCommandManager.literal("hud")
                                        .executes(context -> {

                                            hudEnabled = !hudEnabled;
                                            OkeanMenu.saveSettings();
                                            return 1;
                                        })
                        )

                        // /okean bright
                        .then(
                                ClientCommandManager.literal("bright")
                                        .executes(context -> {

                                            fullbright = !fullbright;
                                            OkeanMenu.saveSettings();
                                            return 1;
                                        })
                        )

        );
    }

    // =========================
    // HUD
    // =========================

    private void renderTargetRings(WorldRenderContext context) {
        if (!showRings || context.consumers() == null || context.matrixStack() == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getPos();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
        for (LivingEntity entity : client.world.getEntitiesByClass(
                LivingEntity.class, client.player.getBoundingBox().expand(16.0),
                e -> e != client.player && e.isAlive()
                        && (e instanceof AnimalEntity || e instanceof PlayerEntity))) {
            double distanceSq = client.player.squaredDistanceTo(entity);
            if (distanceSq > 16.0 * 16.0) continue;
            boolean inReach = distanceSq <= 9.0 && client.player.canSee(entity);
            boolean ready = client.player.getAttackCooldownProgress(0.0f) >= 0.95f;
            // Green means the attack is in reach and ready; red means not yet.
            int cr = inReach && ready ? 60 : 255;
            int cg = inReach && ready ? 235 : 70;
            int cb = 75;
            matrices.push();
            matrices.translate(entity.getX() - camera.x, entity.getY() - camera.y + 0.05,
                    entity.getZ() - camera.z);
            float radius = Math.max(0.45f, entity.getWidth() * 0.7f);
            final int segments = 32;
            for (int i = 0; i < segments; i++) {
                double a = Math.PI * 2.0 * i / segments;
                double b = Math.PI * 2.0 * (i + 1) / segments;
                lines.vertex(matrices.peek(), (float) (Math.cos(a) * radius), 0.0f,
                                (float) (Math.sin(a) * radius))
                        .color(cr, cg, cb, 240).normal(matrices.peek(), 0.0f, 1.0f, 0.0f);
                lines.vertex(matrices.peek(), (float) (Math.cos(b) * radius), 0.0f,
                                (float) (Math.sin(b) * radius))
                        .color(cr, cg, cb, 240).normal(matrices.peek(), 0.0f, 1.0f, 0.0f);
            }
            matrices.pop();
        }
    }

    // =========================
    // МЕНЮ
    // =========================

    public static class OkeanMenu extends Screen {
        private enum Tab { MISC, HUB, SETTINGS }
        private enum Language { RUSSIAN, ENGLISH, UKRAINIAN }
        private enum Theme {
            OCEAN_BLUE, DARK_PURPLE, MIDNIGHT, EMERALD, SUNSET,
            SAKURA, FROST, NEON, AMBER, FOREST
        }

        private static Language language = Language.RUSSIAN;
        private static Theme theme = Theme.OCEAN_BLUE;
        private static int opacityIndex = 0;

        private Tab tab = Tab.MISC;
        private int px;
        private int py;
        private int pw;
        private int ph;
        private int handButtonX, handButtonY, handButtonW;

        public OkeanMenu() {
            super(Text.literal("Okean Visuals"));
        }

        public static String tr(String ru, String en, String uk) {
            return switch (language) {
                case RUSSIAN -> ru;
                case ENGLISH -> en;
                case UKRAINIAN -> uk;
            };
        }

        public static int accent() {
            return switch (theme) {
                case OCEAN_BLUE -> 0xFF40D7FF;
                case DARK_PURPLE -> 0xFFBB88FF;
                case MIDNIGHT -> 0xFF9AB4D6;
                case EMERALD -> 0xFF5BEAAB;
                case SUNSET -> 0xFFFFAA63;
                case SAKURA -> 0xFFFF94C8;
                case FROST -> 0xFF9FE6FF;
                case NEON -> 0xFFACFF6F;
                case AMBER -> 0xFFFFC157;
                case FOREST -> 0xFF73D88E;
            };
        }

        public static int panelColor() {
            int alpha = switch (opacityIndex) {
                case 1 -> 0xB8;
                case 2 -> 0xF5;
                default -> 0xDF;
            };
            int rgb = switch (theme) {
                case OCEAN_BLUE -> 0x0C1222;
                case DARK_PURPLE -> 0x180E29;
                case MIDNIGHT -> 0x0C1119;
                case EMERALD -> 0x09251A;
                case SUNSET -> 0x291524;
                case SAKURA -> 0x251426;
                case FROST -> 0x112330;
                case NEON -> 0x171A27;
                case AMBER -> 0x2A1B14;
                case FOREST -> 0x10261B;
            };
            return (alpha << 24) | rgb;
        }

        /** Named theme selector used by Plus Mini appearance presets. */
        public static void selectMiniTheme(String name) {
            try {
                theme = Theme.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                theme = Theme.OCEAN_BLUE;
            }
        }

        private static String themeName() {
            return switch (theme) {
                case OCEAN_BLUE -> "Ocean Blue";
                case DARK_PURPLE -> "Dark Purple";
                case MIDNIGHT -> "Midnight";
                case EMERALD -> "Emerald";
                case SUNSET -> "Sunset";
                case SAKURA -> "Sakura";
                case FROST -> "Frost";
                case NEON -> "Neon";
                case AMBER -> "Amber";
                case FOREST -> "Forest";
            };
        }

        private static Arm clientArm() {
            return MinecraftClient.getInstance().options.getMainArm().getValue();
        }

        private static String languageName() {
            return switch (language) {
                case RUSSIAN -> "Русский";
                case ENGLISH -> "English";
                case UKRAINIAN -> "Українська";
            };
        }

        // Keep UI preferences between Minecraft launches.
        private static final Path CONFIG_PATH =
                FabricLoader.getInstance().getConfigDir().resolve("okeanvisuals-ui.properties");

        private static void loadSettings() {
            if (!Files.exists(CONFIG_PATH)) return;
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
                try { language = Language.valueOf(props.getProperty("language", "RUSSIAN")); }
                catch (IllegalArgumentException ignored) { language = Language.RUSSIAN; }
                try { theme = Theme.valueOf(props.getProperty("theme", "OCEAN_BLUE")); }
                catch (IllegalArgumentException ignored) { theme = Theme.OCEAN_BLUE; }
                try {
                    opacityIndex = MathHelper.clamp(
                            Integer.parseInt(props.getProperty("opacity", "0")), 0, 2);
                } catch (NumberFormatException ignored) { opacityIndex = 0; }
                itemX = parseFloat(props, "itemX", 0.0f, -1.0f, 1.0f);
                itemY = parseFloat(props, "itemY", 0.0f, -1.0f, 1.0f);
                itemZ = parseFloat(props, "itemZ", 0.0f, -1.0f, 1.0f);
                itemScale = parseFloat(props, "itemScale", 1.0f, 0.5f, 2.0f);
                handAnimation = parseInt(props, "handAnimation", 0, 2);
                OkeanHud.x = parseInt(props, "hudX", 10, 20000);
                OkeanHud.y = parseInt(props, "hudY", 10, 20000);
                OkeanHud.scale = parseFloat(props, "hudScale", 1.0f, 0.75f, 1.75f);
                OkeanHud.showWasd = Boolean.parseBoolean(props.getProperty("wasd", "true"));
                OkeanHud.plusCrosshair = Boolean.parseBoolean(props.getProperty("crosshair", "false"));
                OkeanHud.visualTimeEnabled = Boolean.parseBoolean(
                        props.getProperty("visualTimeEnabled", "false"));
                OkeanHud.visualTimeHour = MathHelper.clamp(
                        parseInt(props, "visualTimeHour", 12, 24), 1, 24);
                hudEnabled = Boolean.parseBoolean(props.getProperty("hudEnabled", "true"));
                showRings = Boolean.parseBoolean(props.getProperty("showRings", "true"));
                autoSprint = Boolean.parseBoolean(props.getProperty("autoSprint", "false"));
                fullbright = Boolean.parseBoolean(props.getProperty("fullbright", "false"));
                OkeanPlusMini.chineseHat = Boolean.parseBoolean(props.getProperty("miniHat", "false"));
                OkeanPlusMini.gradientHud = Boolean.parseBoolean(props.getProperty("miniGradient", "false"));
                OkeanPlusMini.hudTransparency = parseInt(props, "miniHudOpacity", 0, 3);
                OkeanPlusMini.roundedHud = Boolean.parseBoolean(props.getProperty("miniRounded", "false"));
                OkeanPlusMini.colorfulWasd = Boolean.parseBoolean(props.getProperty("miniWasd", "false"));
                OkeanPlusMini.animatedUi = Boolean.parseBoolean(props.getProperty("miniAnimatedUi", "true"));
                OkeanPlusMini.hudColor = parseInt(props, "miniHudColor", 0, 10);
                OkeanPlusMini.hatColor = parseInt(props, "miniHatColor", 0, 5);
                OkeanPlusMini.hudStyle = parseInt(props, "miniHudStyle", 0, 2);
                OkeanPlusMini.rainbowHud = Boolean.parseBoolean(props.getProperty("miniRgbHud", "false"));
                OkeanPlusMini.animatedHud = Boolean.parseBoolean(props.getProperty("miniAnimatedHud", "true"));
            } catch (IOException e) {
                System.err.println("Okean Visuals: could not load settings: " + e.getMessage());
            }
        }

        private static float parseFloat(Properties props, String name, float fallback, float min, float max) {
            try { return MathHelper.clamp(Float.parseFloat(props.getProperty(name)), min, max); }
            catch (RuntimeException ignored) { return fallback; }
        }

        private static int parseInt(Properties props, String name, int fallback, int max) {
            try { return MathHelper.clamp(Integer.parseInt(props.getProperty(name)), 0, max); }
            catch (RuntimeException ignored) { return fallback; }
        }

        public static void saveSettings() {
            Properties props = new Properties();
            props.setProperty("language", language.name());
            props.setProperty("theme", theme.name());
            props.setProperty("opacity", Integer.toString(opacityIndex));
            props.setProperty("itemX", Float.toString(itemX));
            props.setProperty("itemY", Float.toString(itemY));
            props.setProperty("itemZ", Float.toString(itemZ));
            props.setProperty("itemScale", Float.toString(itemScale));
            props.setProperty("handAnimation", Integer.toString(handAnimation));
            props.setProperty("hudX", Integer.toString(OkeanHud.x));
            props.setProperty("hudY", Integer.toString(OkeanHud.y));
            props.setProperty("hudScale", Float.toString(OkeanHud.scale));
            props.setProperty("wasd", Boolean.toString(OkeanHud.showWasd));
            props.setProperty("crosshair", Boolean.toString(OkeanHud.plusCrosshair));
            props.setProperty("visualTimeEnabled", Boolean.toString(OkeanHud.visualTimeEnabled));
            props.setProperty("visualTimeHour", Integer.toString(OkeanHud.visualTimeHour));
            props.setProperty("hudEnabled", Boolean.toString(hudEnabled));
            props.setProperty("showRings", Boolean.toString(showRings));
            props.setProperty("autoSprint", Boolean.toString(autoSprint));
            props.setProperty("fullbright", Boolean.toString(fullbright));
            props.setProperty("miniHat", Boolean.toString(OkeanPlusMini.chineseHat));
            props.setProperty("miniGradient", Boolean.toString(OkeanPlusMini.gradientHud));
            props.setProperty("miniHudOpacity", Integer.toString(OkeanPlusMini.hudTransparency));
            props.setProperty("miniRounded", Boolean.toString(OkeanPlusMini.roundedHud));
            props.setProperty("miniWasd", Boolean.toString(OkeanPlusMini.colorfulWasd));
            props.setProperty("miniAnimatedUi", Boolean.toString(OkeanPlusMini.animatedUi));
            props.setProperty("miniHudColor", Integer.toString(OkeanPlusMini.hudColor));
            props.setProperty("miniHatColor", Integer.toString(OkeanPlusMini.hatColor));
            props.setProperty("miniHudStyle", Integer.toString(OkeanPlusMini.hudStyle));
            props.setProperty("miniRgbHud", Boolean.toString(OkeanPlusMini.rainbowHud));
            props.setProperty("miniAnimatedHud", Boolean.toString(OkeanPlusMini.animatedHud));
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                    props.store(out, "Okean Visuals UI settings");
                }
            } catch (IOException e) {
                System.err.println("Okean Visuals: could not save settings: " + e.getMessage());
            }
        }

        private static String transparencyName() {
            return switch (opacityIndex) {
                case 1 -> "72%";
                case 2 -> "96%";
                default -> "87%";
            };
        }

        private void addAction(String label, int x, int y, int w, int h,
                               ButtonWidget.PressAction action) {
            addDrawableChild(OkeanButton.of(label, x, y, w, h, action));
        }

        private void addPlanned(String label, int x, int y, int w, int h) {
            OkeanButton button = OkeanButton.of(
                    label + "  [" + tr("скоро", "soon", "скоро") + "]",
                    x, y, w, h, ignored -> {});
            button.active = false;
            addDrawableChild(button);
        }

        @Override
        protected void init() {
            pw = Math.min(440, Math.max(220, width - 24));
            ph = Math.min(315, Math.max(215, height - 24));
            px = (width - pw) / 2;
            py = (height - ph) / 2;

            int pad = 18;
            int gap = 6;
            int tabW = (pw - 2 * pad - 2 * gap) / 3;
            int tabY = py + 37;

            addAction(tr("Разное", "Misc", "Різне"), px + pad, tabY, tabW, 23,
                    b -> { tab = Tab.MISC; clearAndInit(); });
            addAction(tr("Хаб", "Hub", "Хаб"), px + pad + tabW + gap, tabY, tabW, 23,
                    b -> { tab = Tab.HUB; clearAndInit(); });
            addAction(tr("Настройки", "Settings", "Налаштування"),
                    px + pad + 2 * (tabW + gap), tabY, tabW, 23,
                    b -> { tab = Tab.SETTINGS; clearAndInit(); });

            int rowX = px + 35;
            int rowW = pw - 70;
            int rowY = tabY + 37;
            int rowGap = 26;
            handButtonX = rowX; handButtonY = rowY; handButtonW = rowW;

            if (tab == Tab.MISC) {
                Arm selectedArm = clientArm();
                addAction(tr("Расположение рук (ПКМ)", "Hand positions (right-click)",
                                "Розташування рук (ПКМ)"),
                        rowX, rowY, rowW, 22, b -> {
                            MinecraftClient.getInstance().setScreen(new HandEditorScreen(this));
                        });
                addAction("Fullbright: " + (fullbright ? "ON" : "OFF"),
                        rowX, rowY + rowGap, rowW, 22, b -> {
                            fullbright = !fullbright;
                            saveSettings();
                            clearAndInit();
                        });
                addAction("AutoSprint: " + (autoSprint ? "ON" : "OFF"),
                        rowX, rowY + 2 * rowGap, rowW, 22, b -> {
                            autoSprint = !autoSprint;
                            saveSettings();
                            clearAndInit();
                        });
                addAction(tr("Круги целей: ", "Target rings: ", "Кола цілей: ")
                                + (showRings ? "ON" : "OFF"),
                        rowX, rowY + 3 * rowGap, rowW, 22, b -> {
                            showRings = !showRings;
                            saveSettings();
                            clearAndInit();
                        });
            } else if (tab == Tab.SETTINGS) {
                addAction(tr("Язык: ", "Language: ", "Мова: ") + languageName(),
                        rowX, rowY, rowW, 22, b -> {
                            language = Language.values()[(language.ordinal() + 1)
                                    % Language.values().length];
                            saveSettings();
                            clearAndInit();
                        });
                addAction(tr("Стиль: ", "Theme: ", "Стиль: ") + themeName(),
                        rowX, rowY + rowGap, rowW, 22, b -> {
                            theme = Theme.values()[(theme.ordinal() + 1)
                                    % Theme.values().length];
                            saveSettings();
                            clearAndInit();
                        });
                addAction(tr("Непрозрачность: ", "Opacity: ", "Непрозорість: ")
                                + transparencyName(),
                        rowX, rowY + 2 * rowGap, rowW, 22, b -> {
                            opacityIndex = (opacityIndex + 1) % 3;
                            saveSettings();
                            clearAndInit();
                        });
                addAction(tr("Горячие клавиши", "Keybinds", "Гарячі клавіші"),
                        rowX, rowY + 3 * rowGap, rowW, 22, b -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.setScreen(new ControlsOptionsScreen(this, client.options));
                        });
                addAction(tr("Настройки HUD", "HUD settings", "Налаштування HUD"),
                        rowX, rowY + 4 * rowGap, rowW, 22, b -> {
                            MinecraftClient.getInstance().setScreen(
                                    new OkeanHud.VisualSettingsScreen(this));
                        });
                addAction(tr("Plus Mini: эффекты", "Plus Mini visuals", "Plus Mini: ефекти"),
                        rowX, rowY + 5 * rowGap, rowW, 22, b -> {
                            MinecraftClient.getInstance().setScreen(
                                    new OkeanPlusMini.SettingsScreen(this));
                        });
            }

            addAction(tr("Закрыть", "Close", "Закрити"),
                    px + (pw - 135) / 2, py + ph - 32, 135, 23,
                    b -> close());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (tab == Tab.MISC && button == 1
                    && mouseX >= handButtonX && mouseX <= handButtonX + handButtonW
                    && mouseY >= handButtonY && mouseY <= handButtonY + 22) {
                MinecraftClient.getInstance().setScreen(new HandEditorScreen(this));
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0xA0000614);
            ctx.fill(px, py, px + pw, py + ph, panelColor());
            ctx.drawBorder(px, py, pw, ph, accent());
            ctx.fill(px + 1, py + 1, px + pw - 1, py + 4, accent());

            ctx.drawText(textRenderer, "🌊 OKEAN VISUALS",
                    px + 18, py + 16, accent(), true);

            int pad = 18;
            int gap = 6;
            int tabW = (pw - 2 * pad - 2 * gap) / 3;
            int selection = switch (tab) {
                case MISC -> 0;
                case HUB -> 1;
                case SETTINGS -> 2;
            };
            int activeX = px + pad + selection * (tabW + gap);
            ctx.fill(activeX, py + 63, activeX + tabW, py + 65, accent());

            if (tab == Tab.HUB) {
                int tx = px + 33;
                int ty = py + 83;
                ctx.drawText(textRenderer, tr("Главная страница Okean Visuals",
                        "Okean Visuals main page", "Головна сторінка Okean Visuals"),
                        tx, ty, accent(), true);
                ctx.drawText(textRenderer, tr("Версия: 1.5.0 Plus Mini Test",
                        "Version: 1.5.0 Plus Mini Test", "Версія: 1.5.0 Plus Mini Test"),
                        tx, ty + 24, 0xFFFFFFFF, true);
                ctx.drawText(textRenderer, tr("Plus Mini: RGB, шляпа, пресеты",
                        "Plus Mini: RGB, hat, presets", "Plus Mini: RGB, капелюх, пресети"),
                        tx, ty + 46, 0xFFB8D6EB, true);
                ctx.drawText(textRenderer, tr("Помощь: Right Shift — открыть меню",
                        "Help: Right Shift — open menu", "Допомога: Right Shift — відкрити меню"),
                        tx, ty + 68, 0xFFB8D6EB, true);
                ctx.drawText(textRenderer, tr("Plus Mini: тестовая сборка",
                        "Plus Mini: testing build", "Plus Mini: тестова збірка"),
                        tx, ty + 90, 0xFFB8D6EB, true);
            } else if (tab == Tab.SETTINGS) {
                ctx.drawText(textRenderer, tr("Нажми на кнопку, чтобы сменить настройку",
                        "Click a button to change a setting",
                        "Натисни кнопку, щоб змінити налаштування"),
                        px + 30, py + ph - 55, 0xFFB8D6EB, true);
            }

            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }

    // Small separate first-person held-item editor.
    public static class HandEditorScreen extends Screen {
        private final Screen parent;
        private int x, y, w, h;

        public HandEditorScreen(Screen parent) {
            super(Text.literal("Okean Visuals - Hand Editor"));
            this.parent = parent;
        }

        private void button(String label, int bx, int by, int bw, Runnable action) {
            addDrawableChild(OkeanButton.of(label, bx, by, bw, 20, b -> {
                action.run();
                OkeanMenu.saveSettings();
                clearAndInit();
            }));
        }

        private void step(int row, String label, Runnable minus, Runnable plus) {
            int by = y + 39 + row * 27;
            button("-", x + w - 96, by, 39, minus);
            button("+", x + w - 50, by, 39, plus);
        }

        @Override
        protected void init() {
            w = Math.min(360, width - 24);
            h = Math.min(250, height - 20);
            x = (width - w) / 2;
            y = (height - h) / 2;
            step(0, "X", () -> itemX = clamp(itemX - 0.05f, -1, 1),
                    () -> itemX = clamp(itemX + 0.05f, -1, 1));
            step(1, "Y", () -> itemY = clamp(itemY - 0.05f, -1, 1),
                    () -> itemY = clamp(itemY + 0.05f, -1, 1));
            step(2, "Z", () -> itemZ = clamp(itemZ - 0.05f, -1, 1),
                    () -> itemZ = clamp(itemZ + 0.05f, -1, 1));
            step(3, "Size", () -> itemScale = clamp(itemScale - 0.05f, 0.5f, 2.0f),
                    () -> itemScale = clamp(itemScale + 0.05f, 0.5f, 2.0f));
            step(4, "Animation", () -> handAnimation = (handAnimation + 2) % 3,
                    () -> handAnimation = (handAnimation + 1) % 3);
            button(OkeanMenu.tr("Сбросить", "Reset", "Скинути"),
                    x + 20, y + h - 33, 104, () -> {
                        itemX = 0; itemY = 0; itemZ = 0;
                        itemScale = 1; handAnimation = 0;
                    });
            addDrawableChild(OkeanButton.of(OkeanMenu.tr("Готово", "Done", "Готово"),
                    x + w - 128, y + h - 33, 104, 20, b -> close()));
        }

        private static float clamp(float value, float min, float max) {
            return Math.round(MathHelper.clamp(value, min, max) * 20f) / 20f;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
            handAnimation = (handAnimation + (vertical > 0 ? 2 : 1)) % 3;
            OkeanMenu.saveSettings();
            clearAndInit();
            return true;
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            ctx.fill(0, 0, width, height, 0xA0000614);
            ctx.fill(x, y, x + w, y + h, OkeanMenu.panelColor());
            ctx.drawBorder(x, y, w, h, OkeanMenu.accent());
            ctx.drawText(textRenderer, "ITEM EDITOR", x + 16, y + 14, OkeanMenu.accent(), true);
            String[] rows = {
                String.format(java.util.Locale.ROOT, "X: %.2f", itemX),
                String.format(java.util.Locale.ROOT, "Y: %.2f", itemY),
                String.format(java.util.Locale.ROOT, "Z: %.2f", itemZ),
                String.format(java.util.Locale.ROOT, "Size: %.2f", itemScale),
                "Motion: " + switch (handAnimation) {
                    case 1 -> "Float"; case 2 -> "Sway"; default -> "Normal"; }
            };
            for (int i = 0; i < rows.length; i++) {
                ctx.drawText(textRenderer, rows[i], x + 16, y + 45 + i * 27,
                        0xFFB8D6EB, true);
            }
            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
            MinecraftClient.getInstance().setScreen(parent);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
