/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.systems.hud;

import christmas.client.events.christmas.CustomFontChangedEvent;
import christmas.client.events.render.Render2DEvent;
import christmas.client.events.world.TickEvent;
import christmas.client.gui.WidgetScreen;
import christmas.client.settings.*;
import christmas.client.systems.System;
import christmas.client.systems.Systems;
import christmas.client.systems.hud.elements.*;
import christmas.client.systems.hud.elements.keyboard.KeyboardHud;
import christmas.client.systems.hud.screens.HudEditorScreen;
import christmas.client.utils.Utils;
import christmas.client.utils.misc.Keybind;
import christmas.client.utils.misc.NbtUtils;
import christmas.client.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.NonNull;

import java.util.*;

import static christmas.client.ChristmasClient.mc;

public class Hud extends System<Hud> implements Iterable<HudElement> {
    public static final HudGroup GROUP = new HudGroup("Christmas");

    public boolean active;
    public Settings settings = new Settings();

    public final Map<String, HudElementInfo<?>> infos = new TreeMap<>();
    private final List<HudElement> elements = new ArrayList<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEditor = settings.createGroup("Editor");
    private final SettingGroup sgKeybind = settings.createGroup("Bind");

    // General

    private final Setting<Boolean> customFont = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-font")
        .description("Text will use custom font.")
        .defaultValue(true)
        .onChanged(_ -> {
            for (HudElement element : elements) element.onFontChanged();
        })
        .build()
    );

    private final Setting<Boolean> hideInMenus = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-in-menus")
        .description("Hides the christmas hud when in inventory screens or game menus.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> textScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Scale of text if not overridden by the element.")
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    public final Setting<List<SettingColor>> textColors = sgGeneral.add(new ColorListSetting.Builder()
        .name("text-colors")
        .description("Colors used for the Text element.")
        .defaultValue(List.of(new SettingColor(250, 247, 237), new SettingColor(240, 198, 92), new SettingColor(62, 190, 112), new SettingColor(228, 65, 78)))
        .build()
    );

    // Editor

    public final Setting<Integer> border = sgEditor.add(new IntSetting.Builder()
        .name("border")
        .description("Space around the edges of the screen.")
        .defaultValue(4)
        .sliderMax(20)
        .build()
    );

    public final Setting<Integer> snappingRange = sgEditor.add(new IntSetting.Builder()
        .name("snapping-range")
        .description("Snapping range in editor.")
        .defaultValue(10)
        .sliderMax(20)
        .build()
    );

    // Keybindings
    @SuppressWarnings("unused")
    private final Setting<Keybind> keybind = sgKeybind.add(new KeybindSetting.Builder()
        .name("bind")
        .defaultValue(Keybind.none())
        .action(() -> active = !active)
        .build()
    );

    private boolean resetToDefaultElements;

    public Hud() {
        super("hud");
    }

    public static Hud get() {
        return Systems.get(Hud.class);
    }

    @Override
    public void init() {
        settings.registerColorSettings(null);

        register(ChristmasTextHud.INFO);
        register(FestiveBannerHud.INFO);
        register(ItemHud.INFO);
        register(InventoryHud.INFO);
        register(CompassHud.INFO);
        register(ArmorHud.INFO);
        register(HoleHud.INFO);
        register(PlayerModelHud.INFO);
        register(ActiveModulesHud.INFO);
        register(LagNotifierHud.INFO);
        register(PlayerRadarHud.INFO);
        register(ModuleInfosHud.INFO);
        register(PotionTimersHud.INFO);
        register(CombatHud.INFO);
        register(MapHud.INFO);
        register(KeyboardHud.INFO);

        // Default config
        if (isFirstInit) resetToDefaultElements();
    }

    public void register(HudElementInfo<?> info) {
        infos.put(info.name, info);
    }

    private void add(HudElement element, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        element.box.setPos(x, y);

        if (xAnchor == null || yAnchor == null) element.box.updateAnchors();
        else {
            element.box.xAnchor = xAnchor;
            element.box.yAnchor = yAnchor;
        }

        element.settings.registerColorSettings(null);

        elements.add(element);
    }

    public void add(HudElementInfo<?> info, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        add(info.create(), x, y, xAnchor, yAnchor);
    }

    public void add(HudElementInfo<?> info, int x, int y) {
        add(info, x, y, null, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void add(HudElementInfo.@NonNull Preset preset, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
        HudElement element = preset.info.create();
        preset.callback.accept(element);
        add(element, x, y, xAnchor, yAnchor);
    }

    public void add(HudElementInfo<?>.@NonNull Preset preset, int x, int y) {
        add(preset, x, y, null, null);
    }

    void remove(HudElement element) {
        element.settings.unregisterColorSettings();
        elements.remove(element);
    }

    public void clear() {
        elements.clear();
    }

    public void resetToDefaultElements() {
        resetToDefaultElements = true;
    }

    private void resetToDefaultElementsImpl() {
        elements.clear();

        int h = (int) Math.ceil(HudRenderer.INSTANCE.textHeight(true));

        // Top Left
        add(FestiveBannerHud.INFO, 4, 4, XAnchor.Left, YAnchor.Top);
        add(ChristmasTextHud.FPS, 4, 18 + h, XAnchor.Left, YAnchor.Top);
        add(ChristmasTextHud.TPS, 4, 18 + h * 2, XAnchor.Left, YAnchor.Top);
        add(ChristmasTextHud.PING, 4, 18 + h * 3, XAnchor.Left, YAnchor.Top);
        add(ChristmasTextHud.SPEED, 4, 18 + h * 4, XAnchor.Left, YAnchor.Top);

        // Top Right
        add(ActiveModulesHud.INFO, -4, 4, XAnchor.Right, YAnchor.Top);

        // Bottom Right
        add(ChristmasTextHud.POSITION, -4, -4, XAnchor.Right, YAnchor.Bottom);
        add(ChristmasTextHud.OPPOSITE_POSITION, -4, -4 - h, XAnchor.Right, YAnchor.Bottom);
        add(ChristmasTextHud.ROTATION, -4, -4 - h * 2, XAnchor.Right, YAnchor.Bottom);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Utils.isLoading()) return;

        if (resetToDefaultElements) {
            resetToDefaultElementsImpl();
            resetToDefaultElements = false;
        }

        if (!(active || HudEditorScreen.isOpen())) return;

        for (HudElement element : elements) {
            if (element.isActive() || element.isInEditor()) {
                element.tick(HudRenderer.INSTANCE);
            }
        }
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        if (Utils.isLoading()) return;

        if (!(active || HudEditorScreen.isOpen()) || shouldHideHud()) return;
        if ((mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden || mc.debugEntries.isOverlayVisible()) && !HudEditorScreen.isOpen())
            return;

        HudRenderer.INSTANCE.begin(event.graphics);

        for (HudElement element : elements) {
            element.updatePos();

            if (element.isActive() || element.isInEditor()) {
                element.render(HudRenderer.INSTANCE);
            }
        }

        HudRenderer.INSTANCE.end();
    }

    private boolean shouldHideHud() {
        return hideInMenus.get() && mc.gui.screen() != null && !(mc.gui.screen() instanceof WidgetScreen);
    }

    @EventHandler
    private void onCustomFontChanged(CustomFontChangedEvent event) {
        if (customFont.get()) {
            for (HudElement element : elements) element.onFontChanged();
        }
    }

    public boolean hasCustomFont() {
        return customFont.get();
    }

    public double getTextScale() {
        return textScale.get();
    }

    @NonNull
    @Override
    public Iterator<HudElement> iterator() {
        return elements.iterator();
    }

    // Serialization

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putInt("__version__", 1);

        tag.putBoolean("active", active);
        tag.put("settings", settings.toTag());
        tag.put("elements", NbtUtils.listToTag(elements));

        return tag;
    }

    @Override
    public Hud fromTag(CompoundTag tag) {
        if (!tag.contains("__version__")) {
            resetToDefaultElements();
            return this;
        }

        tag.getBoolean("active").ifPresent(active1 -> active = active1);
        settings.fromTag(tag.getCompoundOrEmpty("settings"));

        // Elements
        elements.clear();

        for (Tag e : tag.getListOrEmpty("elements")) {
            CompoundTag c = (CompoundTag) e;
            if (c.getString("name").isEmpty()) continue;

            HudElementInfo<?> info = infos.get(c.getString("name").get());
            if (info != null) {
                HudElement element = info.create();
                element.fromTag(c);
                element.settings.registerColorSettings(null);
                elements.add(element);
            }
        }

        return this;
    }
}
