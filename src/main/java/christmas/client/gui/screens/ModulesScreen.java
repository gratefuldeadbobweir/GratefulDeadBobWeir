/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.screens;

import com.mojang.datafixers.util.Pair;
import christmas.client.gui.GuiTheme;
import christmas.client.gui.tabs.TabScreen;
import christmas.client.gui.tabs.Tabs;
import christmas.client.gui.utils.Cell;
import christmas.client.gui.widgets.containers.WContainer;
import christmas.client.gui.widgets.containers.WSection;
import christmas.client.gui.widgets.containers.WVerticalList;
import christmas.client.gui.widgets.containers.WWindow;
import christmas.client.gui.widgets.input.WTextBox;
import christmas.client.systems.config.Config;
import christmas.client.systems.modules.Category;
import christmas.client.systems.modules.Module;
import christmas.client.systems.modules.Modules;
import christmas.client.utils.misc.NbtUtils;
import christmas.client.utils.misc.input.Input;
import christmas.client.utils.render.DisplayItemUtils;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static christmas.client.utils.Utils.getWindowHeight;
import static christmas.client.utils.Utils.getWindowWidth;

public class ModulesScreen extends TabScreen {
    private WCategoryController controller;
    private WWindow searchWindow;
    private WTextBox searchTextBox;

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        controller = add(new WCategoryController()).top().widget();

        // Help
        if (theme.modulesHelpText()) {
            WVerticalList help = add(theme.verticalList()).pad(4).bottom().widget();
            help.add(theme.label("Left click - Toggle module"));
            help.add(theme.label("Right click - Open module settings"));
        }
    }

    @Override
    protected void init() {
        super.init();
        controller.refresh();
    }

    // Category

    protected WWindow createCategory(WContainer c, Category category, List<Module> moduleList) {
        WWindow w = theme.window(category.name);
        w.id = category.name;
        w.padding = 0;
        w.spacing = 0;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(category.icon.get())).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = true;
        w.view.spacing = 0;

        for (Module module : moduleList) {
            w.add(theme.module(module)).expandX();
        }

        return w;
    }

    // Search

    protected void createSearchW(WContainer w, String text) {
        if (!text.isEmpty()) {
            // Titles
            List<Pair<Module, String>> modules = Modules.get().searchTitles(text);

            if (!modules.isEmpty()) {
                WSection section = w.add(theme.section("Modules")).expandX().widget();
                section.spacing = 0;

                int count = 0;
                for (Pair<Module, String> p : modules) {
                    if (count >= Config.get().moduleSearchCount.get() || count >= modules.size()) break;
                    section.add(theme.module(p.getFirst(), p.getSecond())).expandX();
                    count++;
                }
            }

            // Settings
            Set<Module> settings = Modules.get().searchSettingTitles(text);

            if (!settings.isEmpty()) {
                WSection section = w.add(theme.section("Settings")).expandX().widget();
                section.spacing = 0;

                int count = 0;
                for (Module module : settings) {
                    if (count >= Config.get().moduleSearchCount.get() || count >= settings.size()) break;
                    section.add(theme.module(module)).expandX();
                    count++;
                }
            }
        }
    }

    protected WWindow createSearch(WContainer c) {
        WWindow w = theme.window("Search");
        w.id = "search";
        searchWindow = w;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(DisplayItemUtils.toStack(Items.COMPASS))).pad(2);
        }

        c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = true;
        w.view.maxHeight -= 20;

        WVerticalList l = theme.verticalList();

        WTextBox text = w.add(theme.textBox("")).minWidth(140).expandX().widget();
        text.setFocused(true);
        searchTextBox = text;
        text.action = () -> {
            l.clear();
            createSearchW(l, text.get());
        };

        w.add(l).expandX();
        createSearchW(l, text.get());

        return w;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (locked) return false;

        boolean ctrl = SystemUtils.IS_OS_MAC
            ? Input.modsMatch(input.modifiers(), MOD_SUPER)
            : Input.modsMatch(input.modifiers(), MOD_CONTROL);

        if (ctrl && input.key() == KEY_F) {
            if (searchWindow != null) searchWindow.setExpanded(true);
            if (searchTextBox != null) {
                searchTextBox.setFocused(true);
                searchTextBox.setCursorMax();
            }

            return true;
        }

        return super.keyPressed(input);
    }

    // Favorites

    protected Cell<WWindow> createFavorites(WContainer c) {
        boolean hasFavorites = Modules.get().getAll().stream().anyMatch(module -> module.favorite);
        if (!hasFavorites) return null;

        WWindow w = theme.window("Favorites");
        w.id = "favorites";
        w.padding = 0;
        w.spacing = 0;

        if (theme.categoryIcons()) {
            w.beforeHeaderInit = wContainer -> wContainer.add(theme.item(DisplayItemUtils.toStack(Items.NETHER_STAR))).pad(2);
        }

        Cell<WWindow> cell = c.add(w);
        w.view.scrollOnlyWhenMouseOver = true;
        w.view.hasScrollBar = true;
        w.view.spacing = 0;

        createFavoritesW(w);
        return cell;
    }

    protected boolean createFavoritesW(WWindow w) {
        List<Module> modules = new ArrayList<>();

        for (Module module : Modules.get().getAll()) {
            if (module.favorite) {
                modules.add(module);
            }
        }

        modules.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name));

        for (Module module : modules) {
            w.add(theme.module(module)).expandX();
        }

        return !modules.isEmpty();
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    @Override
    public void reload() {
    }

    // Stuff

    protected class WCategoryController extends WContainer {
        public final List<WWindow> windows = new ArrayList<>();
        private Cell<WWindow> favorites;
        private final Map<WWindow, Double> defaultViewHeights = new IdentityHashMap<>();

        @Override
        public void init() {
            List<Module> moduleList = new ArrayList<>();
            for (Category category : Modules.loopCategories()) {
                for (Module module : Modules.get().getGroup(category)) {
                    if (!Config.get().hiddenModules.get().contains(module)) {
                        moduleList.add(module);
                    }
                }

                // Ensure empty categories are not shown
                if (!moduleList.isEmpty()) {
                    windows.add(createCategory(this, category, moduleList));
                    moduleList.clear();
                }
            }

            windows.add(createSearch(this));

            refresh();
        }

        protected void refresh() {
            if (favorites == null) {
                favorites = createFavorites(this);
                if (favorites != null) windows.add(favorites.widget());
            } else {
                favorites.widget().clear();

                if (!createFavoritesW(favorites.widget())) {
                    remove(favorites);
                    windows.remove(favorites.widget());
                    defaultViewHeights.remove(favorites.widget());
                    favorites = null;
                }
            }
        }

        @Override
        protected void onCalculateSize() {
            double pad = theme.scale(4);
            double availableWidth = Math.max(1, getWindowWidth() - 2 * pad);
            for (Cell<?> cell : cells) {
                WWindow window = (WWindow) cell.widget();
                defaultViewHeights.putIfAbsent(window, window.view.maxHeight);
                window.view.maxHeight = defaultViewHeights.get(window);
                window.calculateSize();
            }

            // Scrollbars can increase a window's width; recalculate rows until stable.
            int rows = 1;
            for (int pass = 0; pass <= cells.size(); pass++) {
                int neededRows = 1;
                double rowWidth = 0;
                for (Cell<?> cell : cells) {
                    double width = cell.widget().width;
                    if (rowWidth > 0 && rowWidth + pad + width > availableWidth) {
                        neededRows++;
                        rowWidth = 0;
                    }
                    rowWidth += (rowWidth > 0 ? pad : 0) + width;
                }
                if (pass > 0 && neededRows == rows) break;
                rows = neededRows;
                double top = theme.scale(40);
                double bottom = theme.modulesHelpText() ? theme.scale(38) : pad;
                double rowHeight = Math.max(1, (getWindowHeight() - top - bottom - pad * (rows - 1)) / rows);
                for (Cell<?> cell : cells) {
                    WWindow window = (WWindow) cell.widget();
                    double frameHeight = window.height - window.view.height;
                    window.view.maxHeight = Math.min(defaultViewHeights.get(window), Math.max(1, rowHeight - frameHeight));
                    window.calculateSize();
                }
            }
            super.onCalculateSize();
        }

        @Override
        protected void onCalculateWidgetPositions() {
            double pad = theme.scale(4);
            double startX = this.x + pad;
            double right = getWindowWidth() - pad;
            double x = startX;
            double y = this.y + theme.scale(40);
            double rowHeight = 0;

            for (Cell<?> cell : cells) {
                double width = cell.widget().width;
                double height = cell.widget().height;
                if (x > startX && x + width > right) {
                    x = startX;
                    y += rowHeight + pad;
                    rowHeight = 0;
                }

                cell.x = x;
                cell.y = y;
                cell.width = width;
                cell.height = height;
                cell.alignWidget();
                x += width + pad;
                rowHeight = Math.max(rowHeight, height);
            }
        }
    }
}
