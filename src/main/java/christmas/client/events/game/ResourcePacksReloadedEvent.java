/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.events.game;

public class ResourcePacksReloadedEvent {
    private static final ResourcePacksReloadedEvent INSTANCE = new ResourcePacksReloadedEvent();

    public static ResourcePacksReloadedEvent get() {
        return INSTANCE;
    }
}
