/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas.widgets;

import christmas.client.gui.renderer.GuiRenderer;
import christmas.client.gui.themes.christmas.ChristmasWidget;
import christmas.client.gui.widgets.containers.WView;

public class WChristmasView extends WView implements ChristmasWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (canScroll && hasScrollBar) {
            renderer.quad(handleX(), handleY(), handleWidth(), handleHeight(), theme().scrollbarColor.get(focused, handleMouseOver));
            if (theme().festiveDetails.get()) {
                double line = Math.min(theme().scale(1), handleWidth());
                renderer.quad(handleX(), handleY(), line, handleHeight(), theme().favoriteColor.get());
            }
        }
    }
}
