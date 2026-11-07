/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas.widgets;

import christmas.client.gui.renderer.GuiRenderer;
import christmas.client.gui.themes.christmas.ChristmasWidget;
import christmas.client.gui.widgets.WWidget;
import christmas.client.gui.widgets.containers.WWindow;
import christmas.client.utils.render.color.Color;

public class WChristmasWindow extends WWindow implements ChristmasWidget {
    private static final Color SNOW = new Color(250, 247, 237, 220);
    private static final Color RED_LIGHT = new Color(228, 65, 78);
    private static final Color GREEN_LIGHT = new Color(62, 190, 112);
    private static final Color GOLD_LIGHT = new Color(240, 198, 92);
    private static final Color[] LIGHTS = { GOLD_LIGHT, GREEN_LIGHT, RED_LIGHT };
    public WChristmasWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WChristmasHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            renderer.quad(x, y + header.height, width, height - header.height, theme().backgroundColor.get());
            if (theme().festiveDetails.get() && width > 0 && height > header.height) {
                double line = Math.min(theme().scale(1), Math.min(width, height - header.height));
                renderer.quad(x, y + header.height, line, height - header.height, theme().separatorEdges.get());
                renderer.quad(x + width - line, y + header.height, line, height - header.height, theme().separatorEdges.get());
                renderer.quad(x, y + height - line, width, line, theme().favoriteColor.get());
            }
        }
    }

    private class WChristmasHeader extends WHeader {
        public WChristmasHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.quad(this, theme().accentColor.get());
            if (theme().festiveHeaders.get() && width > 0 && height > 0) {
                double line = Math.min(theme().scale(1), height / 3);
                renderer.quad(x, y, width, line, SNOW);
                double spacing = theme().scale(12);
                double light = Math.min(theme().scale(2), height / 3);
                for (int i = 0; (i + 1) * spacing < width; i++) {
                    renderer.quad(x + (i + 1) * spacing, y + height - light, Math.min(light, width - (i + 1) * spacing), light, LIGHTS[i % LIGHTS.length]);
                }
            }
        }
    }
}
