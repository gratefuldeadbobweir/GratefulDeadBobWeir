/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixininterface;

import com.mojang.authlib.GameProfile;

public interface IGuiMessage {
    String christmas$getText();

    int christmas$getId();

    void christmas$setId(int id);

    GameProfile christmas$getSender();

    void christmas$setSender(GameProfile profile);
}
