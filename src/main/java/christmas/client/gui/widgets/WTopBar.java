/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.widgets;

import christmas.client.gui.renderer.GuiRenderer;
import christmas.client.gui.tabs.Tab;
import christmas.client.gui.tabs.TabScreen;
import christmas.client.gui.tabs.Tabs;
import christmas.client.gui.widgets.containers.WHorizontalList;
import christmas.client.gui.widgets.pressable.WPressable;
import christmas.client.utils.render.color.Color;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.sdl.SDLMouse;

import static christmas.client.ChristmasClient.mc;

public abstract class WTopBar extends WHorizontalList {
    protected abstract Color getButtonColor(boolean pressed, boolean hovered);

    protected abstract Color getNameColor();

    protected void renderButtonDecoration(GuiRenderer renderer, double x, double y, double width, double height, boolean selected, boolean hovered) {
    }

    public WTopBar() {
        spacing = 0;
    }

    @Override
    public void init() {
        for (Tab tab : Tabs.get()) {
            add(new WTopBarButton(tab));
        }
    }

    protected class WTopBarButton extends WPressable {
        private final Tab tab;

        public WTopBarButton(Tab tab) {
            this.tab = tab;
        }

        @Override
        protected void onCalculateSize() {
            double pad = pad();

            width = pad + theme.textWidth(tab.name) + pad;
            height = pad + theme.textHeight() + pad;
        }

        @Override
        protected void onPressed(int button) {
            Screen screen = mc.gui.screen();

            if (!(screen instanceof TabScreen tabScreen) || tabScreen.tab != tab) {
                double mouseX = mc.mouseHandler.xpos();
                double mouseY = mc.mouseHandler.ypos();

                tab.openScreen(theme);
                SDLMouse.SDL_WarpMouseInWindow(mc.getWindow().handle(), (float) mouseX, (float) mouseY);
            }
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double pad = pad();
            boolean selected = mc.gui.screen() instanceof TabScreen tabScreen && tabScreen.tab == tab;
            Color color = getButtonColor(pressed || selected, mouseOver);

            renderer.quad(x, y, width, height, color);
            renderButtonDecoration(renderer, x, y, width, height, selected, mouseOver);
            renderer.text(tab.name, x + pad, y + pad, getNameColor(), false);
        }
    }
}
