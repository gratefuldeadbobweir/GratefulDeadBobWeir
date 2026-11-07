/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.gui.themes.christmas;

import christmas.client.gui.DefaultSettingsWidgetFactory;
import christmas.client.gui.GuiTheme;
import christmas.client.gui.WidgetScreen;
import christmas.client.gui.renderer.packer.GuiTexture;
import christmas.client.gui.themes.christmas.widgets.*;
import christmas.client.gui.themes.christmas.widgets.input.WChristmasDropdown;
import christmas.client.gui.themes.christmas.widgets.input.WChristmasSlider;
import christmas.client.gui.themes.christmas.widgets.input.WChristmasTextBox;
import christmas.client.gui.themes.christmas.widgets.pressable.*;
import christmas.client.gui.utils.AlignmentX;
import christmas.client.gui.utils.CharFilter;
import christmas.client.gui.widgets.*;
import christmas.client.gui.widgets.containers.WSection;
import christmas.client.gui.widgets.containers.WView;
import christmas.client.gui.widgets.containers.WWindow;
import christmas.client.gui.widgets.input.WDropdown;
import christmas.client.gui.widgets.input.WSlider;
import christmas.client.gui.widgets.input.WTextBox;
import christmas.client.gui.widgets.pressable.*;
import christmas.client.renderer.text.TextRenderer;
import christmas.client.settings.*;
import christmas.client.systems.accounts.Account;
import christmas.client.systems.modules.Module;
import christmas.client.utils.render.color.Color;
import christmas.client.utils.render.color.SettingColor;
import org.apache.commons.lang3.SystemUtils;

import static christmas.client.ChristmasClient.mc;

public class ChristmasGuiTheme extends GuiTheme {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final SettingGroup sgTextColors = settings.createGroup("Text");
    private final SettingGroup sgBackgroundColors = settings.createGroup("Background");
    private final SettingGroup sgOutline = settings.createGroup("Outline");
    private final SettingGroup sgSeparator = settings.createGroup("Separator");
    private final SettingGroup sgScrollbar = settings.createGroup("Scrollbar");
    private final SettingGroup sgSlider = settings.createGroup("Slider");
    private final SettingGroup sgStarscript = settings.createGroup("Starscript");

    // General

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the GUI.")
        .defaultValue(1)
        .min(0.75)
        .sliderRange(0.75, 4)
        .onSliderRelease()
        .onChanged(_ -> {
            if (mc.gui.screen() instanceof WidgetScreen widgetScreen) widgetScreen.invalidate();
        })
        .build()
    );

    public final Setting<AlignmentX> moduleAlignment = sgGeneral.add(new EnumSetting.Builder<AlignmentX>()
        .name("module-alignment")
        .description("How module titles are aligned.")
        .defaultValue(AlignmentX.Center)
        .build()
    );

    public final Setting<Boolean> categoryIcons = sgGeneral.add(new BoolSetting.Builder()
        .name("category-icons")
        .description("Adds item icons to module categories.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> modulesHelpText = sgGeneral.add(new BoolSetting.Builder()
        .name("modules-help-text")
        .description("Toggle help text in the modules screen.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> hideHUD = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-HUD")
        .description("Hide HUD when in GUI.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> festiveHeaders = sgGeneral.add(new BoolSetting.Builder()
        .name("festive-headers")
        .description("Decorate window headers with snow and holiday lights.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> festiveDetails = sgGeneral.add(new BoolSetting.Builder()
        .name("festive-details")
        .description("Show holiday accents on menu controls and panels.")
        .defaultValue(true)
        .build()
    );

    // Colors

    public final Setting<SettingColor> accentColor = color("accent", "Main color of the GUI.", new SettingColor(196, 43, 60));
    public final Setting<SettingColor> checkboxColor = color("checkbox", "Color of checkbox.", new SettingColor(196, 43, 60));
    public final Setting<SettingColor> plusColor = color("plus", "Color of plus button.", new SettingColor(62, 190, 112));
    public final Setting<SettingColor> minusColor = color("minus", "Color of minus button.", new SettingColor(228, 65, 78));
    public final Setting<SettingColor> favoriteColor = color("favorite", "Color of checked favorite button.", new SettingColor(240, 198, 92));

    // Text

    public final Setting<SettingColor> textColor = color(sgTextColors, "text", "Color of text.", new SettingColor(250, 247, 237));
    public final Setting<SettingColor> textSecondaryColor = color(sgTextColors, "text-secondary-text", "Color of secondary text.", new SettingColor(150, 150, 150));
    public final Setting<SettingColor> textHighlightColor = color(sgTextColors, "text-highlight", "Color of text highlighting.", new SettingColor(62, 150, 100, 100));
    public final Setting<SettingColor> titleTextColor = color(sgTextColors, "title-text", "Color of title text.", new SettingColor(250, 247, 237));
    public final Setting<SettingColor> loggedInColor = color(sgTextColors, "logged-in-text", "Color of logged in account name.", new SettingColor(62, 190, 112));
    public final Setting<SettingColor> placeholderColor = color(sgTextColors, "placeholder", "Color of placeholder text.", new SettingColor(255, 255, 255, 20));

    // Background

    public final ThreeStateColorSetting backgroundColor = new ThreeStateColorSetting(
        sgBackgroundColors,
        "background",
        new SettingColor(12, 27, 22, 225),
        new SettingColor(19, 42, 32, 230),
        new SettingColor(29, 58, 42, 235)
    );

    public final Setting<SettingColor> moduleBackground = color(sgBackgroundColors, "module-background", "Color of module background when active.", new SettingColor(28, 61, 42));

    // Outline

    public final ThreeStateColorSetting outlineColor = new ThreeStateColorSetting(
        sgOutline,
        "outline",
        new SettingColor(8, 18, 14),
        new SettingColor(14, 30, 22),
        new SettingColor(21, 43, 31)
    );

    // Separator

    public final Setting<SettingColor> separatorText = color(sgSeparator, "separator-text", "Color of separator text", new SettingColor(250, 247, 237));
    public final Setting<SettingColor> separatorCenter = color(sgSeparator, "separator-center", "Center color of separators.", new SettingColor(250, 247, 237));
    public final Setting<SettingColor> separatorEdges = color(sgSeparator, "separator-edges", "Color of separator edges.", new SettingColor(240, 198, 92, 150));

    // Scrollbar

    public final ThreeStateColorSetting scrollbarColor = new ThreeStateColorSetting(
        sgScrollbar,
        "Scrollbar",
        new SettingColor(19, 42, 32, 230),
        new SettingColor(29, 58, 42, 235),
        new SettingColor(39, 74, 52, 240)
    );

    // Slider

    public final ThreeStateColorSetting sliderHandle = new ThreeStateColorSetting(
        sgSlider,
        "slider-handle",
        new SettingColor(170, 32, 49),
        new SettingColor(196, 43, 60),
        new SettingColor(222, 66, 79)
    );

    public final Setting<SettingColor> sliderLeft = color(sgSlider, "slider-left", "Color of slider left part.", new SettingColor(44, 137, 82));
    public final Setting<SettingColor> sliderRight = color(sgSlider, "slider-right", "Color of slider right part.", new SettingColor(28, 61, 42));

    // Starscript

    private final Setting<SettingColor> starscriptText = color(sgStarscript, "starscript-text", "Color of text in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptBraces = color(sgStarscript, "starscript-braces", "Color of braces in Starscript code.", new SettingColor(150, 150, 150));
    private final Setting<SettingColor> starscriptParenthesis = color(sgStarscript, "starscript-parenthesis", "Color of parenthesis in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptDots = color(sgStarscript, "starscript-dots", "Color of dots in starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptCommas = color(sgStarscript, "starscript-commas", "Color of commas in starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptOperators = color(sgStarscript, "starscript-operators", "Color of operators in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptStrings = color(sgStarscript, "starscript-strings", "Color of strings in Starscript code.", new SettingColor(106, 135, 89));
    private final Setting<SettingColor> starscriptNumbers = color(sgStarscript, "starscript-numbers", "Color of numbers in Starscript code.", new SettingColor(104, 141, 187));
    private final Setting<SettingColor> starscriptKeywords = color(sgStarscript, "starscript-keywords", "Color of keywords in Starscript code.", new SettingColor(204, 120, 50));
    private final Setting<SettingColor> starscriptAccessedObjects = color(sgStarscript, "starscript-accessed-objects", "Color of accessed objects (before a dot) in Starscript code.", new SettingColor(152, 118, 170));

    public ChristmasGuiTheme() {
        super("Christmas");

        settingsFactory = new DefaultSettingsWidgetFactory(this);
    }

    private Setting<SettingColor> color(SettingGroup group, String name, String description, SettingColor color) {
        return group.add(new ColorSetting.Builder()
            .name(name + "-color")
            .description(description)
            .defaultValue(color)
            .build());
    }

    private Setting<SettingColor> color(String name, String description, SettingColor color) {
        return color(sgColors, name, description, color);
    }

    // Widgets

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WChristmasWindow(icon, title));
    }

    @Override
    public WLabel label(String text, boolean title, double maxWidth) {
        if (maxWidth == 0 && !text.contains("\n")) return w(new WChristmasLabel(text, title));
        return w(new WChristmasMultiLabel(text, title, maxWidth));
    }

    @Override
    public WHorizontalSeparator horizontalSeparator(String text) {
        return w(new WChristmasHorizontalSeparator(text));
    }

    @Override
    public WVerticalSeparator verticalSeparator() {
        return w(new WChristmasVerticalSeparator());
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WChristmasButton(text, texture));
    }

    @Override
    protected WConfirmedButton confirmedButton(String text, String confirmText, GuiTexture texture) {
        return w(new WChristmasConfirmedButton(text, confirmText, texture));
    }

    @Override
    public WMinus minus() {
        return w(new WChristmasMinus());
    }

    @Override
    public WConfirmedMinus confirmedMinus() {
        return w(new WChristmasConfirmedMinus());
    }

    @Override
    public WPlus plus() {
        return w(new WChristmasPlus());
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WChristmasCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WChristmasSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WChristmasTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WChristmasDropdown<>(values, value));
    }

    @Override
    public WTriangle triangle() {
        return w(new WChristmasTriangle());
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WChristmasTooltip(text));
    }

    @Override
    public WView view() {
        return w(new WChristmasView());
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WChristmasSection(title, expanded, headerWidget));
    }

    @Override
    public WAccount account(WidgetScreen screen, Account<?> account) {
        return w(new WChristmasAccount(screen, account));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WChristmasModule(module, title));
    }

    @Override
    public WQuad quad(Color color) {
        return w(new WChristmasQuad(color));
    }

    @Override
    public WTopBar topBar() {
        return w(new WChristmasTopBar());
    }

    @Override
    public WFavorite favorite(boolean checked) {
        return w(new WChristmasFavorite(checked));
    }

    // Colors

    @Override
    public Color textColor() {
        return textColor.get();
    }

    @Override
    public Color textSecondaryColor() {
        return textSecondaryColor.get();
    }

    //     Starscript

    @Override
    public Color starscriptTextColor() {
        return starscriptText.get();
    }

    @Override
    public Color starscriptBraceColor() {
        return starscriptBraces.get();
    }

    @Override
    public Color starscriptParenthesisColor() {
        return starscriptParenthesis.get();
    }

    @Override
    public Color starscriptDotColor() {
        return starscriptDots.get();
    }

    @Override
    public Color starscriptCommaColor() {
        return starscriptCommas.get();
    }

    @Override
    public Color starscriptOperatorColor() {
        return starscriptOperators.get();
    }

    @Override
    public Color starscriptStringColor() {
        return starscriptStrings.get();
    }

    @Override
    public Color starscriptNumberColor() {
        return starscriptNumbers.get();
    }

    @Override
    public Color starscriptKeywordColor() {
        return starscriptKeywords.get();
    }

    @Override
    public Color starscriptAccessedObjectColor() {
        return starscriptAccessedObjects.get();
    }

    // Other

    @Override
    public TextRenderer textRenderer() {
        return TextRenderer.get();
    }

    @Override
    public double scale(double value) {
        double scaled = value * scale.get();

        if (SystemUtils.IS_OS_MAC) {
            scaled /= (double) mc.getWindow().getWidth() / mc.getWindow().getWidth();
        }

        return scaled;
    }

    @Override
    public boolean categoryIcons() {
        return categoryIcons.get();
    }

    @Override
    public boolean modulesHelpText() {
        return modulesHelpText.get();
    }

    @Override
    public boolean hideHUD() {
        return hideHUD.get();
    }

    public class ThreeStateColorSetting {
        private final Setting<SettingColor> normal, hovered, pressed;

        public ThreeStateColorSetting(SettingGroup group, String name, SettingColor c1, SettingColor c2, SettingColor c3) {
            normal = color(group, name, "Color of " + name + ".", c1);
            hovered = color(group, "hovered-" + name, "Color of " + name + " when hovered.", c2);
            pressed = color(group, "pressed-" + name, "Color of " + name + " when pressed.", c3);
        }

        public SettingColor get() {
            return normal.get();
        }

        public SettingColor get(boolean pressed, boolean hovered, boolean bypassDisableHoverColor) {
            if (pressed) return this.pressed.get();
            return (hovered && (bypassDisableHoverColor || !disableHoverColor)) ? this.hovered.get() : this.normal.get();
        }

        public SettingColor get(boolean pressed, boolean hovered) {
            return get(pressed, hovered, false);
        }
    }
}
