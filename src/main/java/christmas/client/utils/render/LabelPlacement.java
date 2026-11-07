package christmas.client.utils.render;

import java.util.ArrayList;
import java.util.List;

/** Places labels upwards, keeping their padded screen rectangles from overlapping. */
public final class LabelPlacement {
    private final List<Bounds> bounds = new ArrayList<>();

    private record Bounds(double left, double top, double right, double bottom) {}

    public void clear() {
        bounds.clear();
    }

    /** Returns the adjusted screen Y, or NaN if the label cannot fit onscreen. */
    public double place(double x, double y, double width, double height, double scale, double screenWidth, double screenHeight) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width) || !Double.isFinite(height)
            || !Double.isFinite(scale) || scale <= 0 || width < 0 || height < 0 || screenWidth <= 0 || screenHeight <= 0) return Double.NaN;
        double left = x - (width / 2 + 2) * scale;
        double right = x + (width / 2 + 2) * scale;
        if (left < 0 || right > screenWidth) return Double.NaN;
        double top = y - (height + 1) * scale;
        double bottom = y + scale;
        boolean moved;
        do {
            moved = false;
            for (Bounds other : bounds) {
                if (left < other.right && right > other.left && top < other.bottom && bottom > other.top) {
                    double shift = bottom - other.top + 2;
                    y -= shift;
                    top -= shift;
                    bottom -= shift;
                    moved = true;
                }
            }
        } while (moved && top >= 0);
        if (top < 0 || bottom > screenHeight) return Double.NaN;
        bounds.add(new Bounds(left, top, right, bottom));
        return y;
    }
}
