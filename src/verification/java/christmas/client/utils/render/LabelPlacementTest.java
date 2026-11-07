package christmas.client.utils.render;

public final class LabelPlacementTest {
    private LabelPlacementTest() {}

    public static void main(String[] args) {
        LabelPlacement placement = new LabelPlacement();
        double first = placement.place(100, 100, 40, 10, 1, 200, 200);
        check(first == 100, "first position");
        double second = placement.place(100, 100, 40, 10, 1, 200, 200);
        check(second == 86, "overlap moved above");
        double third = placement.place(100, 100, 40, 10, 1, 200, 200);
        check(third == 72, "overlapping stack");
        check(placement.place(160, 100, 20, 10, 1, 200, 200) == 100, "separate column");
        check(Double.isNaN(placement.place(1, 100, 40, 10, 1, 200, 200)), "left edge");
        check(Double.isNaN(placement.place(199, 100, 40, 10, 1, 200, 200)), "right edge");
        check(Double.isNaN(placement.place(100, 8, 40, 10, 1, 200, 200)), "top edge");
        check(Double.isNaN(placement.place(100, 200, 40, 10, 1, 200, 200)), "bottom edge");
        check(Double.isNaN(placement.place(100, 100, 40, 10, 0, 200, 200)), "zero scale");
        check(Double.isNaN(placement.place(Double.NaN, 100, 40, 10, 1, 200, 200)), "nonfinite input");
        placement.clear();
        check(placement.place(100, 100, 40, 10, 1, 200, 200) == 100, "clear");
        placement.clear();
        check(placement.place(100, 100, 20, 10, 2, 200, 200) == 100, "scaled first");
        check(placement.place(100, 100, 20, 10, 2, 200, 200) == 74, "scaled overlap");
        System.out.println("LabelPlacement regression suite passed");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
