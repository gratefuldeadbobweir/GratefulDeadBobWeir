/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BarrierVision extends Module {
    private static final List<Block> TARGETS = List.of(Blocks.BARRIER, Blocks.LIGHT, Blocks.STRUCTURE_VOID);
    private static final int MAX_PARTICLES = 5000;
    private static final int SCAN_INTERVAL = 40;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The block search range (in blocks).")
        .defaultValue(24)
        .min(1)
        .sliderRange(5, 48)
        .build()
    );

    private int timer = 0;
    private int spawned = 0;

    public BarrierVision() {
        super(Categories.Render, "barrier-vision", "Makes invisible visible.");
    }

    @Override
    public void onActivate() {
        // Forces a scan on the first tick after enabling
        timer = SCAN_INTERVAL;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null) return;
        if (++timer < SCAN_INTERVAL) return;
        timer = 0;
        spawned = 0;

        int r = range.get();

        BlockIterator.register(r, r, (pos, state) -> {
            if (!TARGETS.contains(state.getBlock())) return;

            mc.level.addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                0, 0, 0
            );

            if (++spawned >= MAX_PARTICLES) BlockIterator.disableCurrent();
        });
    }
}
