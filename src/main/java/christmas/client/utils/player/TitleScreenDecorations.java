package christmas.client.utils.player;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Decorations stay at the edges so the title screen's buttons remain usable. */
public final class TitleScreenDecorations {
    private static final int WIRE = 0xFF315B3D;
    private static final int[] LIGHTS = { 0xFFE4414E, 0xFFF0C65C, 0xFF3EBE70 };
    private static final int SNOW = 0xC8FAF7ED;

    private TitleScreenDecorations() {
    }

    public static void render(GuiGraphicsExtractor graphics, int width, int height) {
        if (width < 240 || height < 90) return;

        // Keep the upper right clear for addon credits.
        int end = width / 2 - 8;
        graphics.fill(8, 1, end, 2, WIRE);
        for (int x = 12, index = 0; x + 3 <= end; x += 16, index++) {
            graphics.fill(x + 1, 2, x + 2, 4, WIRE);
            graphics.fill(x, 4, x + 3, 7, LIGHTS[index % LIGHTS.length]);
        }

        // Animate flakes only in the outer margins and above the footer text.
        int top = Math.max(24, height / 3);
        int bottom = height - 20;
        if (bottom <= top) return;
        int span = bottom - top;
        long time = System.nanoTime() / 70_000_000L;
        for (int i = 0; i < 12; i++) {
            int y = top + Math.floorMod(time + i * 37L, span);
            int inset = 3 + (i * 7 % 9);
            int x = i % 2 == 0 ? inset : width - inset - 1;
            graphics.fill(x, y, x + 1, y + 1, SNOW);
        }
    }
}
