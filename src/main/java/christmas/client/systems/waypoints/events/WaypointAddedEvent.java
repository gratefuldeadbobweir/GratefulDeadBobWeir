/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.systems.waypoints.events;

import christmas.client.systems.waypoints.Waypoint;

public record WaypointAddedEvent(Waypoint waypoint) {
}
