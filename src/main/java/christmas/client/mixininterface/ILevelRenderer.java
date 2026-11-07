/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface ILevelRenderer {
    void christmas$pushEntityOutlineFramebuffer(RenderTarget framebuffer);

    void christmas$popEntityOutlineFramebuffer();
}
