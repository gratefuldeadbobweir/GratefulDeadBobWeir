/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixininterface;

public interface IServerboundMovePlayerPacket {
    int christmas$getTag();

    void christmas$setTag(int tag);
}
