package christmas.client.systems.modules.world;

import christmas.client.events.render.Render2DEvent;
import christmas.client.events.render.Render3DEvent;
import christmas.client.events.world.TickEvent;
import christmas.client.gui.GuiTheme;
import christmas.client.gui.widgets.WWidget;
import christmas.client.gui.widgets.containers.WTable;
import christmas.client.gui.widgets.containers.WVerticalList;
import christmas.client.gui.widgets.pressable.WButton;
import christmas.client.pathing.PathManagers;
import christmas.client.renderer.Renderer2D;
import christmas.client.renderer.ShapeMode;
import christmas.client.renderer.text.TextRenderer;
import christmas.client.settings.*;
import christmas.client.systems.config.Config;
import christmas.client.systems.modules.Categories;
import christmas.client.systems.modules.Module;
import christmas.client.systems.modules.world.storage.StorageClusterer;
import christmas.client.utils.Utils;
import christmas.client.utils.player.PlayerUtils;
import christmas.client.utils.render.NametagUtils;
import christmas.client.utils.render.LabelPlacement;
import christmas.client.utils.render.RenderUtils;
import christmas.client.utils.render.color.Color;
import christmas.client.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Live, bounded survey of groups of storage blocks, including groups spanning chunks. */
public class StorageClusters extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final SettingGroup render = settings.createGroup("Render");

    private final Setting<List<BlockEntityType<?>>> types = general.add(new StorageBlockListSetting.Builder()
        .name("storage-blocks")
        .description("Storage blocks included in the live survey.")
        .defaultValue(StorageBlockListSetting.AVAILABLE_STORAGE_BLOCKS)
        .build());
    private final Setting<Integer> minimum = general.add(new IntSetting.Builder()
        .name("minimum-blocks")
        .description("Minimum storage blocks connected by the link distance.")
        .defaultValue(4).range(1, 512).sliderRange(1, 32).build());
    private final Setting<Integer> radius = general.add(new IntSetting.Builder()
        .name("link-distance")
        .description("Maximum 3D distance between neighboring storage blocks in a group.")
        .defaultValue(6).range(1, 32).sliderRange(1, 16).build());
    private final Setting<Boolean> limitHeight = general.add(new BoolSetting.Builder()
        .name("limit-height").description("Only include storage within the selected Y range.")
        .defaultValue(false).build());
    private final Setting<Integer> minY = general.add(new IntSetting.Builder()
        .name("minimum-y").description("Lowest included storage block Y coordinate.")
        .defaultValue(-64).range(-2048, 2048).sliderRange(-128, 320).visible(limitHeight::get).build());
    private final Setting<Integer> maxY = general.add(new IntSetting.Builder()
        .name("maximum-y").description("Highest included storage block Y coordinate.")
        .defaultValue(320).range(-2048, 2048).sliderRange(-128, 320).visible(limitHeight::get).build());
    private final Setting<Integer> chunksPerTick = general.add(new IntSetting.Builder()
        .name("chunks-per-tick").description("Maximum loaded chunks examined per tick.")
        .defaultValue(4).range(1, 64).sliderRange(1, 16).build());
    private final Setting<Integer> interval = general.add(new IntSetting.Builder()
        .name("survey-interval").description("Seconds between completed surveys.")
        .defaultValue(3).range(1, 120).sliderRange(1, 30).build());
    private final Setting<Integer> pointLimit = general.add(new IntSetting.Builder()
        .name("storage-limit").description("Maximum storage blocks per survey; larger surveys are marked partial.")
        .defaultValue(2048).range(64, 4096).sliderRange(64, 4096).build());
    private final Setting<Integer> visibleLimit = render.add(new IntSetting.Builder()
        .name("max-groups").description("Maximum nearest groups to draw or show in the list.")
        .defaultValue(32).range(1, 128).sliderRange(1, 64).build());
    private final Setting<Boolean> boxes = render.add(new BoolSetting.Builder()
        .name("boxes").description("Draw a box around each group.").defaultValue(true).build());
    private final Setting<Boolean> tracers = render.add(new BoolSetting.Builder()
        .name("tracers").description("Draw tracers to group centers.").defaultValue(false).build());
    private final Setting<Boolean> labels = render.add(new BoolSetting.Builder()
        .name("labels").description("Show block count and Y range above groups.").defaultValue(true).build());
    private final Setting<Boolean> details = render.add(new BoolSetting.Builder()
        .name("label-types").description("Include the types of storage blocks in labels.")
        .defaultValue(false).visible(labels::get).build());
    private final Setting<Double> labelScale = render.add(new DoubleSetting.Builder()
        .name("label-scale").description("Scale of group labels.").defaultValue(1).range(0.1, 3)
        .sliderRange(0.1, 3).visible(labels::get).build());
    private final Setting<SettingColor> lines = render.add(new ColorSetting.Builder()
        .name("line-color").description("Color of group boxes, tracers, and labels.")
        .defaultValue(new SettingColor(154, 219, 244, 255)).build());
    private final Setting<SettingColor> fill = render.add(new ColorSetting.Builder()
        .name("fill-color").description("Color of group box fill.")
        .defaultValue(new SettingColor(154, 219, 244, 25)).visible(boxes::get).build());
    private final Setting<ShapeMode> shapeMode = render.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").description("How group boxes are drawn.")
        .defaultValue(ShapeMode.Lines).visible(boxes::get).build());

    private static final Color LABEL_BACKGROUND = new Color(12, 22, 36, 190);
    private final Vector3d labelPosition = new Vector3d();
    private final LabelPlacement labelPlacement = new LabelPlacement();
    private final List<Observed> observed = new ArrayList<>();
    private List<StorageClusterer.Cluster> groups = List.of();
    private ClientLevel level;
    private Iterator<ChunkAccess> survey;
    private Filter filter;
    private int delay;
    private boolean partial;
    private boolean collectingPartial;

    private record Observed(ChunkPos chunk, LevelChunk source, BlockPos pos) {}
    private record Filter(Set<BlockEntityType<?>> types, int minimum, int radius, boolean limitHeight,
                          int minY, int maxY, int limit) {}

    public StorageClusters() {
        super(Categories.World, "storage-clusters", "Finds groups of loaded storage blocks, including across chunk borders.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        level = null;
        filter = null;
        survey = null;
        observed.clear();
        groups = List.of();
        partial = false;
        collectingPartial = false;
        delay = 0;
    }

    private Filter currentFilter() {
        return new Filter(Set.copyOf(types.get()), minimum.get(), radius.get(), limitHeight.get(), minY.get(), maxY.get(), pointLimit.get());
    }

    private boolean eligible(BlockEntity entity, Filter filter) {
        int y = entity.getBlockPos().getY();
        return filter.types().contains(entity.getType()) && (!filter.limitHeight() || y >= filter.minY() && y <= filter.maxY());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) {
            reset();
            return;
        }
        Filter current = currentFilter();
        if (level != mc.level || !current.equals(filter)) {
            reset();
            level = mc.level;
            filter = current;
        }
        if (survey == null) {
            if (delay > 0) {
                delay--;
                return;
            }
            observed.clear();
            collectingPartial = false;
            survey = Utils.chunks().iterator();
        }
        int budget = chunksPerTick.get();
        while (budget-- > 0 && survey.hasNext()) {
            ChunkAccess access = survey.next();
            if (!(access instanceof LevelChunk chunk)) continue;
            for (BlockEntity entity : chunk.getBlockEntities().values()) {
                if (!eligible(entity, filter)) continue;
                if (observed.size() >= filter.limit()) {
                    collectingPartial = true;
                    break;
                }
                observed.add(new Observed(chunk.getPos(), chunk, entity.getBlockPos().immutable()));
            }
            if (collectingPartial) break;
        }
        if (collectingPartial || !survey.hasNext()) finishSurvey();
    }

    private void finishSurvey() {
        List<StorageClusterer.Point> points = new ArrayList<>(observed.size());
        for (Observed entry : observed) {
            if (mc.level.getChunk(entry.chunk().x(), entry.chunk().z(), ChunkStatus.FULL, false) != entry.source()) continue;
            BlockEntity entity = mc.level.getBlockEntity(entry.pos());
            if (entity == null || !eligible(entity, filter)) continue;
            points.add(new StorageClusterer.Point(entry.pos().getX(), entry.pos().getY(), entry.pos().getZ(), entity.getBlockState().getBlock().getName().getString()));
        }
        groups = StorageClusterer.group(points, filter.radius(), filter.minimum());
        partial = collectingPartial;
        survey = null;
        observed.clear();
        delay = interval.get() * 20;
    }

    private List<StorageClusterer.Cluster> nearest() {
        return groups.stream().sorted(Comparator.comparingDouble(group -> PlayerUtils.squaredDistanceToCamera(group.centerX(), group.centerY(), group.centerZ())))
            .limit(visibleLimit.get()).toList();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (level != mc.level) return;
        for (StorageClusterer.Cluster group : nearest()) {
            if (boxes.get()) event.renderer.box(group.minX(), group.minY(), group.minZ(),
                (double) group.maxX() + 1, (double) group.maxY() + 1, (double) group.maxZ() + 1,
                fill.get(), lines.get(), shapeMode.get(), 0);
            if (tracers.get()) event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                group.centerX(), group.centerY(), group.centerZ(), lines.get());
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!labels.get() || level != mc.level) return;
        labelPlacement.clear();
        boolean shadow = Config.get().customFont.get();
        TextRenderer text = TextRenderer.get();
        for (StorageClusterer.Cluster group : nearest()) {
            labelPosition.set(group.centerX(), (double) group.maxY() + 1.2, group.centerZ());
            if (!NametagUtils.to2D(labelPosition, labelScale.get())) continue;
            String label = describe(group, details.get());
            double width = text.getWidth(label, shadow);
            double height = text.getHeight(shadow);
            double placed = labelPlacement.place(labelPosition.x, labelPosition.y, width, height, NametagUtils.scale, Utils.getWindowWidth(), Utils.getWindowHeight());
            if (Double.isNaN(placed)) continue;
            labelPosition.y = placed;
            NametagUtils.begin(labelPosition, event.graphics);
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-width / 2 - 2, -height - 1, width + 4, height + 2, LABEL_BACKGROUND);
            Renderer2D.COLOR.render();
            text.beginBig(event.graphics);
            text.render(label, -width / 2, -height, lines.get(), shadow);
            text.end();
            NametagUtils.end(event.graphics);
        }
    }

    private static String describe(StorageClusterer.Cluster group, boolean types) {
        String label = group.count() + " storage [Y " + group.minY() + (group.minY() == group.maxY() ? "" : " to " + group.maxY()) + "]";
        if (types) label += " " + group.types().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + " x" + entry.getValue()).collect(Collectors.joining(", "));
        return label;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WTable table = list.add(theme.table()).widget();
        WButton refresh = list.add(theme.button("Refresh List")).widget();
        Runnable populate = () -> {
            table.clear();
            table.add(theme.label(partial ? "Partial survey: storage limit reached" : "Live loaded storage groups"));
            table.row();
            for (StorageClusterer.Cluster group : nearest()) {
                table.add(theme.label(describe(group, false))).padRight(8);
                BlockPos center = BlockPos.containing(group.centerX(), group.centerY(), group.centerZ());
                table.add(theme.label(center.getX() + ", " + center.getY() + ", " + center.getZ())).padRight(8);
                WButton go = table.add(theme.button("Goto")).widget();
                go.action = () -> PathManagers.get().moveTo(center, true);
                table.row();
            }
        };
        refresh.action = populate;
        populate.run();
        return list;
    }

    @Override
    public String getInfoString() {
        return groups.size() + (partial ? "+" : "");
    }
}
