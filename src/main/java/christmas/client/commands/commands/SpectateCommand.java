/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import christmas.client.ChristmasClient;
import christmas.client.commands.Command;
import christmas.client.commands.arguments.PlayerArgumentType;
import christmas.client.events.christmas.KeyInputEvent;
import christmas.client.events.christmas.MouseClickEvent;
import christmas.client.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;

public class SpectateCommand extends Command {

    private final StaticListener shiftListener = new StaticListener();

    public SpectateCommand() {
        super("spectate", "Allows you to spectate nearby players");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(literal("reset").executes(_ -> {
            mc.setCameraEntity(mc.player);
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
            mc.setCameraEntity(PlayerArgumentType.get(context));
            mc.player.sendSystemMessage(Component.literal("Sneak to un-spectate."));
            ChristmasClient.EVENT_BUS.subscribe(shiftListener);
            return SINGLE_SUCCESS;
        }));
    }

    private static class StaticListener {
        @EventHandler
        private void onKey(KeyInputEvent event) {
            if (Input.isPressed(mc.options.keyShift)) {
                mc.setCameraEntity(mc.player);
                event.cancel();
                ChristmasClient.EVENT_BUS.unsubscribe(this);
            }
        }

        @EventHandler
        private void onMouse(MouseClickEvent event) {
            if (Input.isPressed(mc.options.keyShift)) {
                mc.setCameraEntity(mc.player);
                event.cancel();
                ChristmasClient.EVENT_BUS.unsubscribe(this);
            }
        }
    }
}
