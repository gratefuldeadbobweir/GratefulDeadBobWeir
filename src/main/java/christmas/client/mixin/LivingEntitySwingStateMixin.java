/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import christmas.client.systems.modules.Modules;
import christmas.client.systems.modules.render.HandView;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static christmas.client.ChristmasClient.mc;

@Mixin(LivingEntity.SwingState.class)
public abstract class LivingEntitySwingStateMixin {
    @ModifyExpressionValue(method = "getAnimation", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity$SwingState;animation:F", opcode = Opcodes.GETFIELD))
    private float getHandSwingDuration(float original) {
        if ((Object) this != mc.player) return original;

        return Modules.get().get(HandView.class).isActive() && mc.options.getCameraType().isFirstPerson() ? Modules.get().get(HandView.class).swingSpeed.get().floatValue() : original;
    }
}
