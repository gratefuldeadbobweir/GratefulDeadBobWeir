package christmas.client.systems.modules.world.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Groups known storage positions by three-dimensional proximity. */
public final class StorageClusterer {
    private StorageClusterer() {}

    public record Point(int x, int y, int z, String type) {
        public Point {
            if (type == null || type.isBlank()) throw new IllegalArgumentException("Storage type is required");
        }
    }

    public record Cluster(int count, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                          double centerX, double centerY, double centerZ, Map<String, Integer> types) {
        public Cluster {
            types = Map.copyOf(types);
        }
    }

    private record Position(int x, int y, int z) {}
    private record Cell(int x, int y, int z) {}

    /**
     * Two blocks belong to the same cluster when a chain of links, each no longer than
     * radius blocks in 3D, connects them. Duplicate coordinates count once.
     */
    public static List<Cluster> group(Collection<Point> input, int radius, int minimumCount) {
        if (radius < 1 || minimumCount < 1) throw new IllegalArgumentException("Radius and minimum count must be positive");
        Map<Position, Point> unique = new LinkedHashMap<>();
        for (Point point : input) {
            unique.putIfAbsent(new Position(point.x(), point.y(), point.z()), point);
        }
        List<Point> points = new ArrayList<>(unique.values());
        int[] parent = new int[points.size()];
        int[] sizes = new int[points.size()];
        Map<Cell, List<Integer>> grid = new HashMap<>();
        double maxDistanceSquared = (double) radius * radius;

        for (int i = 0; i < points.size(); i++) {
            parent[i] = i;
            sizes[i] = 1;
            Point point = points.get(i);
            int cx = Math.floorDiv(point.x(), radius);
            int cy = Math.floorDiv(point.y(), radius);
            int cz = Math.floorDiv(point.z(), radius);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        long cellX = (long) cx + dx;
                        long cellY = (long) cy + dy;
                        long cellZ = (long) cz + dz;
                        if (cellX < Integer.MIN_VALUE || cellX > Integer.MAX_VALUE || cellY < Integer.MIN_VALUE || cellY > Integer.MAX_VALUE || cellZ < Integer.MIN_VALUE || cellZ > Integer.MAX_VALUE) continue;
                        List<Integer> neighbors = grid.get(new Cell((int) cellX, (int) cellY, (int) cellZ));
                        if (neighbors == null) continue;
                        for (int neighbor : neighbors) {
                            Point other = points.get(neighbor);
                            double x = (double) point.x() - other.x();
                            double y = (double) point.y() - other.y();
                            double z = (double) point.z() - other.z();
                            if (x * x + y * y + z * z <= maxDistanceSquared) union(parent, sizes, i, neighbor);
                        }
                    }
                }
            }
            grid.computeIfAbsent(new Cell(cx, cy, cz), ignored -> new ArrayList<>()).add(i);
        }

        Map<Integer, Builder> builders = new HashMap<>();
        for (int i = 0; i < points.size(); i++) {
            builders.computeIfAbsent(root(parent, i), ignored -> new Builder()).add(points.get(i));
        }
        List<Cluster> result = new ArrayList<>();
        for (Builder builder : builders.values()) {
            if (builder.count >= minimumCount) result.add(builder.build());
        }
        result.sort(Comparator.comparingInt(Cluster::count).reversed()
            .thenComparingInt(Cluster::minX).thenComparingInt(Cluster::minY).thenComparingInt(Cluster::minZ));
        return List.copyOf(result);
    }

    private static int root(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int[] sizes, int a, int b) {
        int left = root(parent, a);
        int right = root(parent, b);
        if (left == right) return;
        if (sizes[left] < sizes[right]) {
            int swap = left;
            left = right;
            right = swap;
        }
        parent[right] = left;
        sizes[left] += sizes[right];
    }

    private static final class Builder {
        int count;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        long x, y, z;
        final Map<String, Integer> types = new TreeMap<>();

        void add(Point point) {
            count++;
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            minZ = Math.min(minZ, point.z());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
            maxZ = Math.max(maxZ, point.z());
            x += point.x();
            y += point.y();
            z += point.z();
            types.merge(point.type(), 1, Integer::sum);
        }

        Cluster build() {
            return new Cluster(count, minX, minY, minZ, maxX, maxY, maxZ,
                x / (double) count + 0.5, y / (double) count + 0.5, z / (double) count + 0.5, types);
        }
    }
}
