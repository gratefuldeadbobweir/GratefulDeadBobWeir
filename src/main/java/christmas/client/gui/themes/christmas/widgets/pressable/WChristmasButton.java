/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas.widgets.pressable;

import christmas.client.gui.renderer.GuiRenderer;
import christmas.client.gui.renderer.packer.GuiTexture;
import christmas.client.gui.themes.christmas.ChristmasGuiTheme;
import christmas.client.gui.themes.christmas.ChristmasWidget;
import christmas.client.gui.widgets.pressable.WButton;

public class WChristmasButton extends WButton implements ChristmasWidget {
    public WChristmasButton(String text, GuiTexture texture) {
        super(text, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        ChristmasGuiTheme theme = theme();
        double pad = pad();

        renderBackground(renderer, this, pressed, mouseOver);

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, theme.textColor.get(), false);
        }
        else {
            double ts = theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, theme.textColor.get());
        }
    }
}
