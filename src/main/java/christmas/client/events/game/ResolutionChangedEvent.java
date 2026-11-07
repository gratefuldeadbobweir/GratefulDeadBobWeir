/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.events.game;

public class ResolutionChangedEvent {
    private static final ResolutionChangedEvent INSTANCE = new ResolutionChangedEvent();

    public static ResolutionChangedEvent get() {
        return INSTANCE;
    }
}
