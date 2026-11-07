/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin;

import christmas.client.systems.config.Config;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(SplashManager.class)
public abstract class SplashManagerMixin {
    @Unique
    private boolean override = true;
    @Unique
    private static final Random random = new Random();
    @Unique
    private final List<String> christmasSplashes = getChristmasSplashes();

    @Inject(method = "getSplash", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfoReturnable<SplashRenderer> cir) {
        if (Config.get() == null || !Config.get().titleScreenSplashes.get()) return;

        if (override)
            cir.setReturnValue(new SplashRenderer(Component.literal(christmasSplashes.get(random.nextInt(christmasSplashes.size())))));
        override = !override;
    }

    @Unique
    private static List<String> getChristmasSplashes() {
        return List.of(
            "Christmas on Crack!",
            "Star Christmas Client on GitHub!",
            "Based utility mod.",
            "§6MineGame159 §fbased god",
            "Merry Christmas!",
            "§4Christmas on Crack!",
            "§6Christmas on Crack!"
        );
    }

}
