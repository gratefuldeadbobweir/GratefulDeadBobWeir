/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas.widgets.pressable;

import christmas.client.gui.themes.christmas.ChristmasWidget;
import christmas.client.gui.widgets.pressable.WFavorite;
import christmas.client.utils.render.color.Color;

public class WChristmasFavorite extends WFavorite implements ChristmasWidget {
    public WChristmasFavorite(boolean checked) {
        super(checked);
    }

    @Override
    protected Color getColor() {
        return theme().favoriteColor.get();
    }
}
