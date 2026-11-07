/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.systems.modules.movement;

import christmas.client.events.packets.PacketEvent;
import christmas.client.events.world.TickEvent;
import christmas.client.settings.BoolSetting;
import christmas.client.settings.EnumSetting;
import christmas.client.settings.Setting;
import christmas.client.settings.SettingGroup;
import christmas.client.systems.modules.Categories;
import christmas.client.systems.modules.Module;
import christmas.client.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.UseEffects;

public class Sprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Mode {
        Strict,
        Rage
    }

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("sprint-mode")
        .description("What mode of sprinting.")
        .defaultValue(Mode.Strict)
        .build()
    );

    private final Setting<Boolean> keepSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-sprint")
        .description("Whether to keep sprinting after attacking.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> unsprintOnHit = sgGeneral.add(new BoolSetting.Builder()
        .name("unsprint-on-hit")
        .description("Whether to stop sprinting before attacking, to ensure you get crits and sweep attacks.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> unsprintInWater = sgGeneral.add(new BoolSetting.Builder()
        .name("unsprint-in-water")
        .description("Whether to stop sprinting when in water.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Rage)
        .build()
    );

    private final Setting<Boolean> permaSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint-while-stationary")
        .description("Sprint even when not moving.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Rage)
        .build()
    );

    private Packet<?> pendingAttack;
    private LocalPlayer attackPlayer;

    public Sprint() {
        super(Categories.Movement, "sprint", "Automatically sprints.");
    }

    @Override
    public void onDeactivate() {
        pendingAttack = null;
        attackPlayer = null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTickMovement(TickEvent.Post event) {
        if (mc.player == null) return;
        boolean sprint = shouldSprint();
        if (mc.player.isSprinting() != sprint) mc.player.setSprinting(sprint);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundAttackPacket)) return;
        pendingAttack = null;
        attackPlayer = null;
        if (event.isCancelled() || !unsprintOnHit.get() || mc.player == null || mc.getConnection() == null
            || event.connection != mc.getConnection().getConnection() || !mc.player.isSprinting()) return;

        pendingAttack = event.packet;
        attackPlayer = mc.player;
        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        mc.player.setSprinting(false);
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        if (event.packet != pendingAttack || pendingAttack == null) return;
        LocalPlayer player = attackPlayer;
        pendingAttack = null;
        attackPlayer = null;
        if (!keepSprint.get() || player == null || player != mc.player || mc.getConnection() == null
            || event.connection != mc.getConnection().getConnection() || !shouldSprint() || player.isSprinting()) return;

        mc.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        player.setSprinting(true);
    }

    public boolean shouldSprint() {
        LocalPlayer player = mc.player;
        if (player == null) return false;

        GUIMove guiMove = Modules.get().get(GUIMove.class);
        boolean screenAllowed = mc.gui.screen() == null || (guiMove.isActive() && guiMove.sprint.get() && !guiMove.skip());
        Entity vehicle = player.getVehicle();
        boolean vehicleCanSprint = vehicle != null && vehicle.canSprint() && vehicle.isLocalInstanceAuthoritative();
        boolean itemSlows = player.isUsingItem() && !Modules.get().get(NoSlow.class).items()
            && !player.getUseItem().getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).canSprint();
        SprintPolicy.State state = new SprintPolicy.State(isActive(), mode.get() == Mode.Rage, screenAllowed,
            player.input.hasForwardImpulse(), player.zza, player.xxa, permaSprint.get(), unsprintInWater.get(),
            player.isInWater(), player.isUnderWater(), player.isInShallowWater(), player.getAbilities().flying,
            player.isMobilityRestricted(), player.isPassenger(), vehicleCanSprint,
            player.getFoodData().hasEnoughFood() || player.getAbilities().mayfly,
            player.horizontalCollision, player.minorHorizontalCollision, itemSlows,
            player.isFallFlying(), player.isMovingSlowly());
        return SprintPolicy.shouldSprint(state);
    }

    public boolean rageSprint() {
        return isActive() && mode.get() == Mode.Rage;
    }

    public boolean unsprintInWater() {
        return rageSprint() && unsprintInWater.get();
    }

    public boolean stopSprinting() {
        return !isActive() || !keepSprint.get();
    }
}
