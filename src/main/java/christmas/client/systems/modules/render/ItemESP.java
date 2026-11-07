package christmas.client.systems.modules.render;

import christmas.client.events.render.Render2DEvent;
import christmas.client.events.render.Render3DEvent;
import christmas.client.renderer.Renderer2D;
import christmas.client.renderer.ShapeMode;
import christmas.client.renderer.text.TextRenderer;
import christmas.client.settings.*;
import christmas.client.systems.config.Config;
import christmas.client.systems.modules.Categories;
import christmas.client.systems.modules.Module;
import christmas.client.utils.Utils;
import christmas.client.utils.misc.Names;
import christmas.client.utils.player.PlayerUtils;
import christmas.client.utils.render.NametagUtils;
import christmas.client.utils.render.LabelPlacement;
import christmas.client.utils.render.FrostESP;
import christmas.client.utils.render.RenderUtils;
import christmas.client.utils.render.color.Color;
import christmas.client.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class ItemESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final FrostESP frost = new FrostESP();
    private final Setting<Boolean> frostAccents = sgRender.add(new BoolSetting.Builder()
        .name("frost-accents")
        .description("Snow caps, icy corners and a snow crystal within 64 blocks. Fades after 48 blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Filter> filter = sgGeneral.add(new EnumSetting.Builder<Filter>()
        .name("filter")
        .description("Show all dropped items, only selected items, or everything except selected items.")
        .defaultValue(Filter.All)
        .build()
    );

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items used by the allow list or deny list.")
        .visible(() -> filter.get() != Filter.All)
        .build()
    );

    private final Setting<Integer> minStackSize = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-stack-size")
        .description("Only show dropped stacks with at least this many items.")
        .defaultValue(1)
        .min(1)
        .sliderMax(64)
        .build()
    );

    private final Setting<Integer> maxDistance = sgGeneral.add(new IntSetting.Builder()
        .name("max-distance")
        .description("Maximum distance from the camera in blocks. Zero shows all loaded items.")
        .defaultValue(0)
        .min(0)
        .sliderMax(512)
        .build()
    );

    private final Setting<Boolean> boxes = sgRender.add(new BoolSetting.Builder()
        .name("boxes")
        .description("Draw a box around dropped items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How item boxes are rendered.")
        .defaultValue(ShapeMode.Lines)
        .visible(boxes::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of item boxes, tracers and beams.")
        .defaultValue(new SettingColor(154, 219, 244, 255))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Color of the box fill.")
        .defaultValue(new SettingColor(154, 219, 244, 35))
        .visible(() -> boxes.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<Double> boxPadding = sgRender.add(new DoubleSetting.Builder()
        .name("box-padding")
        .description("Extra space around the item hitbox.")
        .defaultValue(0.05)
        .min(0)
        .sliderMax(1)
        .visible(boxes::get)
        .build()
    );

    private final Setting<Boolean> tracers = sgRender.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draw a line from the crosshair to each item.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> beams = sgRender.add(new BoolSetting.Builder()
        .name("beams")
        .description("Draw a vertical marker above each item.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> beamHeight = sgRender.add(new DoubleSetting.Builder()
        .name("beam-height")
        .description("Height of item markers in blocks.")
        .defaultValue(3)
        .min(0)
        .sliderMax(16)
        .visible(beams::get)
        .build()
    );

    private final Setting<Boolean> labels = sgRender.add(new BoolSetting.Builder()
        .name("labels")
        .description("Show item names and stack sizes above the boxes.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showDistance = sgRender.add(new BoolSetting.Builder()
        .name("label-distance")
        .description("Include distance from the camera in item labels.")
        .defaultValue(false)
        .visible(labels::get)
        .build()
    );

    private final Setting<Boolean> avoidLabelOverlap = sgRender.add(new BoolSetting.Builder()
        .name("avoid-label-overlap")
        .description("Stack overlapping item labels, showing nearby items first.")
        .defaultValue(true).visible(labels::get).build()
    );

    private final Setting<Integer> labelMaxDistance = sgRender.add(new IntSetting.Builder()
        .name("label-max-distance")
        .description("Maximum label distance in blocks. Zero uses the item render distance.")
        .defaultValue(0).min(0).sliderMax(512).visible(labels::get).build()
    );

    private final Setting<Integer> labelLimit = sgRender.add(new IntSetting.Builder()
        .name("label-limit")
        .description("Maximum number of nearest item labels. Zero has no limit.")
        .defaultValue(0).min(0).sliderMax(256).visible(labels::get).build()
    );

    private final Setting<Double> labelScale = sgRender.add(new DoubleSetting.Builder()
        .name("label-scale")
        .description("Scale of the item labels.")
        .defaultValue(1)
        .min(0.1)
        .sliderMax(3)
        .visible(labels::get)
        .build()
    );

    private static final Color LABEL_BACKGROUND = new Color(12, 22, 36, 175);
    private final Vector3d labelPos = new Vector3d();
    private final LabelPlacement labelPlacement = new LabelPlacement();
    private record LabelTarget(ItemEntity item, double distanceSquared) {}
    private int count;

    public ItemESP() {
        super(Categories.Render, "item-esp", "Highlights dropped items through walls.");
    }

    @Override
    public void onDeactivate() {
        count = 0;
    }

    private boolean matches(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || stack.getCount() < minStackSize.get()) return false;
        if (filter.get() == Filter.AllowList && !items.get().contains(stack.getItem())) return false;
        if (filter.get() == Filter.DenyList && items.get().contains(stack.getItem())) return false;
        int limit = maxDistance.get();
        return limit == 0 || PlayerUtils.squaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ()) <= (double) limit * limit;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        count = 0;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || !matches(item)) continue;
            count++;

            double x = Mth.lerp(event.tickDelta, item.xOld, item.getX()) - item.getX();
            double y = Mth.lerp(event.tickDelta, item.yOld, item.getY()) - item.getY();
            double z = Mth.lerp(event.tickDelta, item.zOld, item.getZ()) - item.getZ();
            AABB box = item.getBoundingBox().move(x, y, z);
            if (boxes.get()) {
                AABB padded = box.inflate(boxPadding.get());
                event.renderer.box(padded, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (frostAccents.get()) frost.render(event.renderer, padded.minX, padded.minY, padded.minZ,
                    padded.maxX, padded.maxY, padded.maxZ, lineColor.get());
            }

            double centerX = (box.minX + box.maxX) / 2;
            double centerY = (box.minY + box.maxY) / 2;
            double centerZ = (box.minZ + box.maxZ) / 2;
            if (tracers.get()) event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, centerX, centerY, centerZ, lineColor.get());
            if (beams.get()) event.renderer.line(centerX, box.maxY, centerZ, centerX, box.maxY + beamHeight.get(), centerZ, lineColor.get());
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!labels.get()) return;
        boolean shadow = Config.get().customFont.get();
        TextRenderer text = TextRenderer.get();
        labelPlacement.clear();
        List<LabelTarget> targets = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || !matches(item)) continue;
            double distance = PlayerUtils.squaredDistanceToCamera(item.getX(), item.getY(), item.getZ());
            int limit = labelMaxDistance.get();
            if (limit > 0 && distance > (double) limit * limit) continue;
            targets.add(new LabelTarget(item, distance));
        }
        targets.sort(Comparator.comparingDouble(LabelTarget::distanceSquared));
        int drawn = 0;
        for (LabelTarget target : targets) {
            if (labelLimit.get() > 0 && drawn >= labelLimit.get()) break;
            ItemEntity item = target.item();
            Utils.set(labelPos, item, event.tickDelta);
            labelPos.add(0, item.getBbHeight() + 0.15, 0);
            if (!NametagUtils.to2D(labelPos, labelScale.get())) continue;

            ItemStack stack = item.getItem();
            String label = Names.get(stack) + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
            if (showDistance.get()) label += " [" + (int) Math.sqrt(PlayerUtils.squaredDistanceToCamera(item.getX(), item.getY(), item.getZ())) + "m]";
            double width = text.getWidth(label, shadow);
            double height = text.getHeight(shadow);
            if (avoidLabelOverlap.get()) {
                double placed = labelPlacement.place(labelPos.x, labelPos.y, width, height, NametagUtils.scale, Utils.getWindowWidth(), Utils.getWindowHeight());
                if (Double.isNaN(placed)) continue;
                labelPos.y = placed;
            }
            drawn++;
            NametagUtils.begin(labelPos, event.graphics);
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-width / 2 - 2, -height - 1, width + 4, height + 2, LABEL_BACKGROUND);
            Renderer2D.COLOR.render();
            text.beginBig(event.graphics);
            text.render(label, -width / 2, -height, lineColor.get(), shadow);
            text.end();
            NametagUtils.end(event.graphics);
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    public enum Filter {
        All,
        AllowList,
        DenyList
    }
}
