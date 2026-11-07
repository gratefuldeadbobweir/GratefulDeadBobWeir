/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
    @Accessor("serverChunkRadius")
    int christmas$getServerChunkRadius();

    @Accessor("signedMessageEncoder")
    SignedMessageChain.Encoder christmas$getSignedMessageEncoder();

    @Accessor("lastSeenMessages")
    LastSeenMessagesTracker christmas$getLastSeenMessages();

    @Accessor("registryAccess")
    RegistryAccess.Frozen christmas$getRegistryAccess();

    @Accessor("enabledFeatures")
    FeatureFlagSet christmas$getEnabledFeatures();

    @Accessor("COMMAND_NODE_BUILDER")
    static ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> christmas$getCommandNodeFactory() {
        return null;
    }
}
