/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin;

import christmas.client.systems.modules.Modules;
import christmas.client.systems.modules.movement.NoSlow;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static christmas.client.ChristmasClient.mc;

@Mixin(SlimeBlock.class)
public abstract class SlimeBlockMixin {
    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
    private void onStepOn(Level level, BlockPos pos, BlockState onState, Entity entity, CallbackInfo ci) {
        if (Modules.get().get(NoSlow.class).slimeBlock() && entity == mc.player) ci.cancel();
    }
}
