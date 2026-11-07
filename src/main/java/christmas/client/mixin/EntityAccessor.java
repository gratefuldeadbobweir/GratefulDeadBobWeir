/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("wasTouchingWater")
    void christmas$setInWater(boolean touchingWater);

    @Accessor("id")
    int christmas$getId();

    @Accessor("id")
    void christmas$setId(int id);
}
