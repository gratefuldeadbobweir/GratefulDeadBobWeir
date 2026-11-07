/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin.iris;

import christmas.client.systems.modules.Modules;
import christmas.client.systems.modules.render.Fullbright;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public abstract class IrisCommonUniformsMixin {
    @Inject(method = "getNightVision", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fullbright$nightVision(CallbackInfoReturnable<Float> cir) {
        if (Modules.get().get(Fullbright.class).getGamma()) cir.setReturnValue(1.0f);
    }
}
