/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import christmas.client.ChristmasClient;
import christmas.client.events.christmas.CharTypedEvent;
import christmas.client.events.christmas.KeyInputEvent;
import christmas.client.gui.GuiKeyEvents;
import christmas.client.gui.WidgetScreen;
import christmas.client.utils.Utils;
import christmas.client.utils.misc.input.Input;
import christmas.client.utils.misc.input.KeyAction;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        int modifiers = event.modifiers();
        if (event.key() != InputConstants.UNKNOWN.getValue()) {
            // on Linux/X11 the modifier is not active when the key is pressed and still active when the key is released
            // https://github.com/glfw/glfw/issues/1630
            if (action == InputConstants.PRESS) {
                modifiers |= Input.getModifier(event.key());
            } else if (action == InputConstants.RELEASE) {
                modifiers &= ~Input.getModifier(event.key());
            }

            if (minecraft.gui.screen() instanceof WidgetScreen widgetScreen && action == InputConstants.REPEAT) {
                widgetScreen.keyRepeated(new KeyEvent(event.key(), event.keycode(), modifiers));
            }

            if (GuiKeyEvents.canUseKeys) {
                Input.setKeyState(event.key(), action != InputConstants.RELEASE);
                if (ChristmasClient.EVENT_BUS.post(KeyInputEvent.get(new KeyEvent(event.key(), event.keycode(), modifiers), KeyAction.get(action))).isCancelled())
                    ci.cancel();
            }
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onChar(long handle, CharacterEvent event, CallbackInfo ci) {
        if (Utils.canUpdate() && !minecraft.isPaused() && (minecraft.gui.screen() == null || minecraft.gui.screen() instanceof WidgetScreen)) {
            if (ChristmasClient.EVENT_BUS.post(CharTypedEvent.get((char) event.codepoint())).isCancelled()) ci.cancel();
        }
    }
}
