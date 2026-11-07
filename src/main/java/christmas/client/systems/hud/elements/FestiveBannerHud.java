package christmas.client.systems.hud.elements;

import christmas.client.ChristmasClient;
import christmas.client.settings.BoolSetting;
import christmas.client.settings.ColorSetting;
import christmas.client.settings.Setting;
import christmas.client.settings.SettingGroup;
import christmas.client.systems.hud.Hud;
import christmas.client.systems.hud.HudElement;
import christmas.client.systems.hud.HudElementInfo;
import christmas.client.systems.hud.HudRenderer;
import christmas.client.utils.render.color.Color;
import christmas.client.utils.render.color.SettingColor;

public class FestiveBannerHud extends HudElement {
    public static final HudElementInfo<FestiveBannerHud> INFO = new HudElementInfo<>(Hud.GROUP, "festive-banner", "A Christmas watermark with holiday lights.", FestiveBannerHud::new);
    private static final Color SNOW = new Color(250, 247, 237, 230);
    private static final Color RED = new Color(228, 65, 78);
    private static final Color GREEN = new Color(62, 190, 112);
    private static final Color GOLD = new Color(240, 198, 92);
    private static final Color[] LIGHTS = { RED, GOLD, GREEN };

    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Boolean> showVersion = general.add(new BoolSetting.Builder()
        .name("show-version")
        .description("Include the client version in the banner.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> lights = general.add(new BoolSetting.Builder()
        .name("lights")
        .description("Decorate the banner with holiday lights.")
        .defaultValue(true)
        .build()
    );
    private final Setting<SettingColor> textColor = general.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the banner text.")
        .defaultValue(new SettingColor(250, 247, 237))
        .build()
    );
    private final Setting<SettingColor> backgroundColor = general.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the banner background.")
        .defaultValue(new SettingColor(12, 35, 24, 195))
        .build()
    );

    public FestiveBannerHud() {
        super(INFO);
    }

    private String bannerText() {
        return ChristmasClient.NAME + (showVersion.get() ? " " + ChristmasClient.VERSION : "");
    }

    @Override
    public void tick(HudRenderer renderer) {
        setSize(renderer.textWidth(bannerText(), true) + 16, renderer.textHeight(true) + 14);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
        renderer.quad(x, y, getWidth(), 2, SNOW);
        if (lights.get()) {
            for (int i = 0; 8 + i * 12 + 3 <= getWidth(); i++) {
                renderer.quad(x + 8 + i * 12, y + 3, 3, 3, LIGHTS[i % LIGHTS.length]);
            }
        }
        renderer.text(bannerText(), x + 8, y + 8, textColor.get(), true);
    }
}
