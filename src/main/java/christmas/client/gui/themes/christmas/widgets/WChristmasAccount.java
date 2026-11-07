/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas.widgets;

import christmas.client.gui.WidgetScreen;
import christmas.client.gui.themes.christmas.ChristmasWidget;
import christmas.client.gui.widgets.WAccount;
import christmas.client.systems.accounts.Account;
import christmas.client.utils.render.color.Color;

public class WChristmasAccount extends WAccount implements ChristmasWidget {
    public WChristmasAccount(WidgetScreen screen, Account<?> account) {
        super(screen, account);
    }

    @Override
    protected Color loggedInColor() {
        return theme().loggedInColor.get();
    }

    @Override
    protected Color accountTypeColor() {
        return theme().textSecondaryColor.get();
    }
}
