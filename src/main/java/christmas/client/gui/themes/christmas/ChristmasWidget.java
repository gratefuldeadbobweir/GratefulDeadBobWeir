/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas;

import christmas.client.gui.renderer.GuiRenderer;
import christmas.client.gui.utils.BaseWidget;
import christmas.client.gui.widgets.WWidget;
import christmas.client.utils.render.color.Color;

public interface ChristmasWidget extends BaseWidget {
    default ChristmasGuiTheme theme() {
        return (ChristmasGuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, Color outlineColor, Color backgroundColor) {
        ChristmasGuiTheme theme = theme();
        double s = theme.scale(2);

        renderer.quad(widget.x + s, widget.y + s, widget.width - s * 2, widget.height - s * 2, backgroundColor);

        renderer.quad(widget.x, widget.y, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + widget.height - s, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + s, s, widget.height - s * 2, outlineColor);
        renderer.quad(widget.x + widget.width - s, widget.y + s, s, widget.height - s * 2, outlineColor);

        if (theme.festiveDetails.get() && widget.width > s * 4 && widget.height > s * 2) {
            double mark = Math.min(theme.scale(5), (widget.width - s * 2) / 2);
            double thickness = Math.min(theme.scale(1), s);
            renderer.quad(widget.x + s, widget.y, mark, thickness, theme.favoriteColor.get());
            renderer.quad(widget.x + widget.width - s - mark, widget.y, mark, thickness, theme.favoriteColor.get());
        }
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        ChristmasGuiTheme theme = theme();
        renderBackground(renderer, widget, theme.outlineColor.get(pressed, mouseOver), theme.backgroundColor.get(pressed, mouseOver));
        if (theme.festiveDetails.get() && (pressed || mouseOver) && widget.width > theme.scale(4) && widget.height > theme.scale(2)) {
            double inset = theme.scale(2);
            renderer.quad(widget.x + inset, widget.y + widget.height - theme.scale(1), widget.width - 2 * inset, theme.scale(1), theme.accentColor.get());
        }
    }
}
