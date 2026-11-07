/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.screens.accounts;

import christmas.client.gui.GuiTheme;
import christmas.client.gui.WindowScreen;
import christmas.client.gui.widgets.containers.WHorizontalList;
import christmas.client.gui.widgets.pressable.WButton;
import christmas.client.systems.accounts.Account;
import christmas.client.systems.accounts.AccountType;
import christmas.client.systems.accounts.TokenAccount;
import christmas.client.utils.render.color.Color;

import static christmas.client.ChristmasClient.mc;

public class AccountInfoScreen extends WindowScreen {
    private final Account<?> account;

    public AccountInfoScreen(GuiTheme theme, Account<?> account) {
        super(theme, account.getUsername() + " details");
        this.account = account;
    }

    @Override
    public void initWidgets() {
        TokenAccount e = (TokenAccount) account;
        WHorizontalList l = add(theme.horizontalList()).expandX().widget();

        String tokenLabel = account.getType() + " token:";
        if (account.getType() == AccountType.Session) tokenLabel = "";

        WButton copy = theme.button("Copy");
        copy.action = () -> mc.keyboardHandler.setClipboard(e.getToken());

        l.add(theme.label(tokenLabel));
        l.add(theme.label(account.getType() == AccountType.Session ? "Click to copy Token" : e.getToken()).color(Color.GRAY)).pad(5);
        l.add(copy);
    }
}
