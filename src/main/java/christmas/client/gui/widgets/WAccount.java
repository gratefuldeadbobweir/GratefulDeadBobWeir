/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.widgets;

import christmas.client.gui.WidgetScreen;
import christmas.client.gui.screens.accounts.AccountInfoScreen;
import christmas.client.gui.widgets.containers.WHorizontalList;
import christmas.client.gui.widgets.pressable.WButton;
import christmas.client.gui.widgets.pressable.WConfirmedMinus;
import christmas.client.systems.accounts.Account;
import christmas.client.systems.accounts.Accounts;
import christmas.client.systems.accounts.TokenAccount;
import christmas.client.utils.network.ChristmasExecutor;
import christmas.client.utils.render.color.Color;

import static christmas.client.ChristmasClient.mc;

public abstract class WAccount extends WHorizontalList {
    public Runnable refreshScreenAction;
    private final WidgetScreen screen;
    private final Account<?> account;

    public WAccount(WidgetScreen screen, Account<?> account) {
        this.screen = screen;
        this.account = account;
    }

    protected abstract Color loggedInColor();

    protected abstract Color accountTypeColor();

    @Override
    public void init() {
        // Head
        add(theme.texture(32, 32, account.getCache().getHeadTexture().needsRotate() ? 90 : 0, account.getCache().getHeadTexture()));

        // Name
        WLabel name = add(theme.label(account.getUsername())).widget();
        if (mc.getUser().getName().equalsIgnoreCase(account.getUsername())) name.color = loggedInColor();

        // Type
        WLabel label = add(theme.label("(" + account.getType() + ")")).expandCellX().right().widget();
        label.color = accountTypeColor();

        // Info
        if (account instanceof TokenAccount) {
            WButton info = add(theme.button("Info")).widget();
            info.action = () -> mc.gui.setScreen(new AccountInfoScreen(theme, account));
        }

        // Login
        WButton login = add(theme.button("Login")).widget();
        login.action = () -> {
            login.minWidth = login.width;
            login.set("...");
            screen.locked = true;

            ChristmasExecutor.execute(() -> {
                if (account.fetchInfo() && account.login()) {
                    name.set(account.getUsername());

                    Accounts.get().save();

                    screen.taskAfterRender = refreshScreenAction;
                }

                login.minWidth = 0;
                login.set("Login");
                screen.locked = false;
            });
        };

        // Remove
        WConfirmedMinus remove = add(theme.confirmedMinus()).widget();
        remove.action = () -> {
            Accounts.get().remove(account);
            if (refreshScreenAction != null) refreshScreenAction.run();
        };
    }
}
