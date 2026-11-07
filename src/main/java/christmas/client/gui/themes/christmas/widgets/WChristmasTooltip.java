/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas.widgets;

import christmas.client.gui.renderer.GuiRenderer;
import christmas.client.gui.themes.christmas.ChristmasWidget;
import christmas.client.gui.widgets.WTooltip;
import christmas.client.renderer.text.TextRenderer;
import christmas.client.renderer.text.VanillaTextRenderer;

public class WChristmasTooltip extends WTooltip implements ChristmasWidget {
    public WChristmasTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (TextRenderer.get() == VanillaTextRenderer.INSTANCE) {
            renderer.fill(this, theme().backgroundColor.get());
        } else {
            renderer.quad(this, theme().backgroundColor.get());
        }
        if (theme().festiveDetails.get() && width > 0 && height > 0) {
            double line = Math.min(theme().scale(1), height);
            renderer.quad(x, y + height - line, width, line, theme().favoriteColor.get());
        }
    }
}
