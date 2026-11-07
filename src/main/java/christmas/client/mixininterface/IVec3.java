/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixininterface;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

@SuppressWarnings("UnusedReturnValue")
public interface IVec3 {
    Vec3 christmas$set(double x, double y, double z);

    default Vec3 christmas$set(Vec3i vec) {
        return christmas$set(vec.getX(), vec.getY(), vec.getZ());
    }

    default Vec3 christmas$set(Vector3d vec) {
        return christmas$set(vec.x, vec.y, vec.z);
    }

    default Vec3 christmas$set(Vec3 pos) {
        return christmas$set(pos.x, pos.y, pos.z);
    }

    Vec3 christmas$setXZ(double x, double z);

    Vec3 christmas$setY(double y);
}
