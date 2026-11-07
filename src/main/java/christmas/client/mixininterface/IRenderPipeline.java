/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixininterface;

public interface IRenderPipeline {
    void christmas$setLineSmooth(boolean lineSmooth);

    boolean christmas$getLineSmooth();
}
