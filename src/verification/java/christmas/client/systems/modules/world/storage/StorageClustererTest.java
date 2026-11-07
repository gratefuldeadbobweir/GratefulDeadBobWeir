package christmas.client.systems.modules.world.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Standalone regression suite for the storage grouping algorithm. */
public final class StorageClustererTest {
    private StorageClustererTest() {}

    public static void main(String[] args) {
        emptyAndInvalidInputs();
        boundariesAndHeight();
        connectedChainsAndSeparation();
        duplicatesAndTypes();
        extremesAndImmutability();
        randomizedAgainstBruteForce();
        System.out.println("StorageClusterer regression suite passed");
    }

    private static StorageClusterer.Point point(int x, int y, int z) {
        return new StorageClusterer.Point(x, y, z, "Chest");
    }

    private static void check(boolean result, String description) {
        if (!result) throw new AssertionError(description);
    }

    private static void emptyAndInvalidInputs() {
        check(StorageClusterer.group(List.of(), 6, 1).isEmpty(), "empty list");
        expectException(() -> StorageClusterer.group(List.of(), 0, 1), "zero radius");
        expectException(() -> StorageClusterer.group(List.of(), 1, 0), "zero minimum");
        expectException(() -> new StorageClusterer.Point(0, 0, 0, ""), "empty type");
        expectException(() -> new StorageClusterer.Point(0, 0, 0, null), "missing type");
    }

    private static void expectException(Runnable action, String description) {
        try {
            action.run();
            throw new AssertionError(description + " accepted");
        } catch (IllegalArgumentException expected) {
            // Expected input validation.
        }
    }

    private static void boundariesAndHeight() {
        var points = List.of(point(15, -60, -1), point(16, -60, -1), point(17, -60, -1));
        var groups = StorageClusterer.group(points, 1, 3);
        check(groups.size() == 1 && groups.getFirst().count() == 3, "connected across chunk boundary");
        var group = groups.getFirst();
        check(group.minX() == 15 && group.maxX() == 17 && group.minY() == -60, "bounds");
        check(group.centerX() == 16.5 && group.centerY() == -59.5 && group.centerZ() == -0.5, "center");
        check(StorageClusterer.group(List.of(point(-1, -64, -1), point(-1, -60, -1)), 3, 2).isEmpty(), "vertical separation");
        check(StorageClusterer.group(List.of(point(-1, -64, -1), point(-1, -60, -1)), 4, 2).size() == 1, "inclusive link radius");
        check(StorageClusterer.group(List.of(point(0, 0, 0), point(3, 4, 0)), 5, 2).size() == 1, "euclidean distance");
        check(StorageClusterer.group(List.of(point(0, 0, 0), point(4, 4, 0)), 5, 2).isEmpty(), "diagonal outside radius");
    }

    private static void connectedChainsAndSeparation() {
        var points = List.of(point(0, 0, 0), point(5, 0, 0), point(10, 0, 0), point(100, 0, 0));
        var groups = StorageClusterer.group(points, 5, 2);
        check(groups.size() == 1 && groups.getFirst().count() == 3, "transitive connection");
        check(groups.getFirst().maxX() == 10, "separate point excluded");
        check(StorageClusterer.group(points, 5, 4).isEmpty(), "minimum size");
    }

    private static void duplicatesAndTypes() {
        var points = List.of(point(0, 1, 2), point(0, 1, 2),
            new StorageClusterer.Point(1, 1, 2, "Barrel"),
            new StorageClusterer.Point(2, 1, 2, "Barrel"));
        var cluster = StorageClusterer.group(points, 1, 3).getFirst();
        check(cluster.count() == 3, "duplicate positions count once");
        check(cluster.types().equals(Map.of("Chest", 1, "Barrel", 2)), "type breakdown");
    }

    private static void extremesAndImmutability() {
        var points = List.of(point(Integer.MIN_VALUE, -64, 0), point(Integer.MIN_VALUE + 1, -64, 0),
            point(Integer.MAX_VALUE - 1, -64, 0), point(Integer.MAX_VALUE, -64, 0));
        var groups = StorageClusterer.group(points, 1, 2);
        check(groups.size() == 2, "no coordinate overflow");
        check(groups.getFirst().centerX() == -2147483647.0, "negative centroid");
        check(groups.getLast().centerX() == 2147483647.0, "positive centroid");
        try {
            groups.clear();
            throw new AssertionError("group list mutable");
        } catch (UnsupportedOperationException expected) {
            // Results are snapshots.
        }
        try {
            groups.getFirst().types().clear();
            throw new AssertionError("type counts mutable");
        } catch (UnsupportedOperationException expected) {
            // Results are snapshots.
        }
    }

    private static void randomizedAgainstBruteForce() {
        Random random = new Random(392114);
        for (int run = 0; run < 350; run++) {
            int count = random.nextInt(65);
            int radius = 1 + random.nextInt(9);
            int minimum = 1 + random.nextInt(5);
            List<StorageClusterer.Point> points = new ArrayList<>();
            Set<String> positions = new HashSet<>();
            while (points.size() < count) {
                int x = random.nextInt(55) - 27;
                int y = random.nextInt(25) - 74;
                int z = random.nextInt(55) - 27;
                if (positions.add(x + ":" + y + ":" + z)) points.add(point(x, y, z));
            }
            var expected = bruteForce(points, radius, minimum);
            var actual = signatures(StorageClusterer.group(points, radius, minimum));
            check(actual.equals(expected), "randomized run " + run);
            Collections.shuffle(points, random);
            check(signatures(StorageClusterer.group(points, radius, minimum)).equals(expected), "input order " + run);
        }
    }

    private static Set<String> bruteForce(List<StorageClusterer.Point> points, int radius, int minimum) {
        int n = points.size();
        boolean[] visited = new boolean[n];
        Set<String> result = new HashSet<>();
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            List<Integer> queue = new ArrayList<>();
            queue.add(start);
            visited[start] = true;
            for (int cursor = 0; cursor < queue.size(); cursor++) {
                var p = points.get(queue.get(cursor));
                for (int i = 0; i < n; i++) {
                    var q = points.get(i);
                    long dx = (long) p.x() - q.x(), dy = (long) p.y() - q.y(), dz = (long) p.z() - q.z();
                    if (!visited[i] && dx * dx + dy * dy + dz * dz <= (long) radius * radius) {
                        visited[i] = true;
                        queue.add(i);
                    }
                }
            }
            if (queue.size() < minimum) continue;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            long sx = 0, sy = 0, sz = 0;
            for (int i : queue) {
                var p = points.get(i);
                minX = Math.min(minX, p.x()); minY = Math.min(minY, p.y()); minZ = Math.min(minZ, p.z());
                maxX = Math.max(maxX, p.x()); maxY = Math.max(maxY, p.y()); maxZ = Math.max(maxZ, p.z());
                sx += p.x(); sy += p.y(); sz += p.z();
            }
            result.add(signature(queue.size(), minX, minY, minZ, maxX, maxY, maxZ, sx, sy, sz));
        }
        return result;
    }

    private static Set<String> signatures(List<StorageClusterer.Cluster> clusters) {
        Set<String> result = new HashSet<>();
        for (var c : clusters) {
            long sx = Math.round((c.centerX() - 0.5) * c.count());
            long sy = Math.round((c.centerY() - 0.5) * c.count());
            long sz = Math.round((c.centerZ() - 0.5) * c.count());
            result.add(signature(c.count(), c.minX(), c.minY(), c.minZ(), c.maxX(), c.maxY(), c.maxZ(), sx, sy, sz));
        }
        return result;
    }

    private static String signature(int count, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long sx, long sy, long sz) {
        return count + ":" + minX + ":" + minY + ":" + minZ + ":" + maxX + ":" + maxY + ":" + maxZ + ":" + sx + ":" + sy + ":" + sz;
    }
}
