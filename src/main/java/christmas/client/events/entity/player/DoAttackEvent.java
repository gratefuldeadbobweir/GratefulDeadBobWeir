/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.events.entity.player;

import christmas.client.events.Cancellable;

public class DoAttackEvent extends Cancellable {
    private static final DoAttackEvent INSTANCE = new DoAttackEvent();

    public static DoAttackEvent get() {
        return INSTANCE;
    }
}
