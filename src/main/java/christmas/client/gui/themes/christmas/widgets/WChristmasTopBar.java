/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas.widgets;

import christmas.client.gui.renderer.GuiRenderer;
import christmas.client.gui.themes.christmas.ChristmasWidget;
import christmas.client.gui.widgets.WTopBar;
import christmas.client.utils.render.color.Color;

public class WChristmasTopBar extends WTopBar implements ChristmasWidget {
    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        return theme().backgroundColor.get(pressed, hovered);
    }

    @Override
    protected Color getNameColor() {
        return theme().textColor.get();
    }

    @Override
    protected void renderButtonDecoration(GuiRenderer renderer, double x, double y, double width, double height, boolean selected, boolean hovered) {
        if (!theme().festiveDetails.get() || width <= 0 || height <= 0) return;
        double line = Math.min(theme().scale(1), height / 3);
        renderer.quad(x, y, width, line, selected ? theme().favoriteColor.get() : theme().separatorEdges.get());
        if (selected || hovered) renderer.quad(x, y + height - line, width, line, theme().accentColor.get());
    }
}
