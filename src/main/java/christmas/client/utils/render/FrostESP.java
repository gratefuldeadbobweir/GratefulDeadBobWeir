package christmas.client.utils.render;

import christmas.client.renderer.Renderer3D;
import christmas.client.utils.player.PlayerUtils;
import christmas.client.utils.render.color.Color;

/** Small, static winter details shared by ESP modules; no textures or particle state. */
public final class FrostESP {
    private final Color ice = new Color();
    private final Color snow = new Color();

    public void render(Renderer3D renderer, double x1, double y1, double z1,
                       double x2, double y2, double z2, Color tint) {
        if (tint.a <= 0) return;
        double cx = (x1 + x2) * 0.5;
        double cy = (y1 + y2) * 0.5;
        double cz = (z1 + z2) * 0.5;
        // Details fade away from 48 to 64 blocks, leaving the primary ESP readable.
        double distance = Math.sqrt(PlayerUtils.squaredDistanceToCamera(cx, cy, cz));
        double fade = Math.max(0, Math.min(1, (64 - distance) / 16));
        int alpha = (int) (tint.a * fade);
        if (alpha == 0) return;
        ice.set(154, 219, 244, alpha);
        snow.set(235, 249, 255, alpha);
        double dx = Math.min(0.18, (x2 - x1) * 0.25);
        double dy = Math.min(0.18, (y2 - y1) * 0.2);
        double dz = Math.min(0.18, (z2 - z1) * 0.25);
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                double px = x == 0 ? x1 : x2;
                double pz = z == 0 ? z1 : z2;
                double sx = x == 0 ? dx : -dx;
                double sz = z == 0 ? dz : -dz;
                // Snow caps on top corners, ice at the feet.
                renderer.line(px, y2, pz, px + sx, y2, pz, snow);
                renderer.line(px, y2, pz, px, y2, pz + sz, snow);
                renderer.line(px, y2, pz, px, y2 - dy, pz, snow, ice);
                renderer.line(px, y1, pz, px + sx, y1, pz, ice);
                renderer.line(px, y1, pz, px, y1, pz + sz, ice);
            }
        }
        // A restrained six-spoke snow crystal above the target, in the X/Z plane.
        double radius = Math.min(0.16, Math.min(x2 - x1, z2 - z1) * 0.3);
        double top = y2 + 0.12;
        for (int spoke = 0; spoke < 6; spoke++) {
            double angle = spoke * Math.PI / 3;
            double ex = cx + Math.cos(angle) * radius;
            double ez = cz + Math.sin(angle) * radius;
            renderer.line(cx, top, cz, ex, top, ez, snow, ice);
            for (int branch = -1; branch <= 1; branch += 2) {
                double bx = cx + Math.cos(angle) * radius * 0.6;
                double bz = cz + Math.sin(angle) * radius * 0.6;
                double turn = angle + branch * Math.PI / 3;
                renderer.line(bx, top, bz, bx - Math.cos(turn) * radius * 0.3,
                    top, bz - Math.sin(turn) * radius * 0.3, ice);
            }
        }
    }
}
