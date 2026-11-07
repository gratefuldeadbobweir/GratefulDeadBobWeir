/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas.widgets;

import christmas.client.gui.renderer.GuiRenderer;
import christmas.client.gui.themes.christmas.ChristmasGuiTheme;
import christmas.client.gui.themes.christmas.ChristmasWidget;
import christmas.client.gui.widgets.WVerticalSeparator;
import christmas.client.utils.render.color.Color;

public class WChristmasVerticalSeparator extends WVerticalSeparator implements ChristmasWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        ChristmasGuiTheme theme = theme();
        Color colorEdges = theme.separatorEdges.get();
        Color colorCenter = theme.separatorCenter.get();

        double s = theme.scale(1);
        double offsetX = Math.round(width / 2.0);

        renderer.quad(x + offsetX, y, s, height / 2, colorEdges, colorEdges, colorCenter, colorCenter);
        renderer.quad(x + offsetX, y + height / 2, s, height / 2, colorCenter, colorCenter, colorEdges, colorEdges);
    }
}
