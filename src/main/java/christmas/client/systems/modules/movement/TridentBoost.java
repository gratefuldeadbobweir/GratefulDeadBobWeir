/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.systems.modules.movement;

import christmas.client.settings.BoolSetting;
import christmas.client.settings.DoubleSetting;
import christmas.client.settings.Setting;
import christmas.client.settings.SettingGroup;
import christmas.client.systems.modules.Categories;
import christmas.client.systems.modules.Module;

public class TridentBoost extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("boost")
        .description("How much your velocity is multiplied by when using riptide.")
        .defaultValue(2)
        .min(0.1)
        .sliderMin(1)
        .build()
    );

    private final Setting<Boolean> allowOutOfWater = sgGeneral.add(new BoolSetting.Builder()
        .name("out-of-water")
        .description("Whether riptide should work out of water")
        .defaultValue(true)
        .build()
    );

    public TridentBoost() {
        super(Categories.Movement, "trident-boost", "Boosts you when using riptide with a trident.");
    }

    public double getMultiplier() {
        return isActive() ? multiplier.get() : 1;
    }

    public boolean allowOutOfWater() {
        return isActive() && allowOutOfWater.get();
    }
}
