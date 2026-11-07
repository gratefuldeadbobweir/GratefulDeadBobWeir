/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import christmas.client.commands.Command;
import christmas.client.renderer.Fonts;
import christmas.client.systems.Systems;
import christmas.client.systems.friends.Friend;
import christmas.client.systems.friends.Friends;
import christmas.client.utils.network.ChristmasExecutor;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super("reload", "Reloads many systems.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            warning("Reloading systems, this may take a while.");

            Systems.load();
            Fonts.refresh();
            ChristmasExecutor.execute(() -> Friends.get().forEach(Friend::updateInfo));

            return SINGLE_SUCCESS;
        });
    }
}
