/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.systems.modules.render;

import christmas.client.events.entity.player.InteractBlockEvent;
import christmas.client.events.render.Render2DEvent;
import christmas.client.events.render.Render3DEvent;
import christmas.client.events.world.BlockUpdateEvent;
import christmas.client.gui.GuiTheme;
import christmas.client.gui.widgets.WWidget;
import christmas.client.gui.widgets.containers.WVerticalList;
import christmas.client.gui.widgets.pressable.WButton;
import christmas.client.renderer.MeshBuilder;
import christmas.client.renderer.MeshRenderer;
import christmas.client.renderer.ChristmasRenderPipelines;
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
import christmas.client.utils.render.MeshBuilderVertexConsumerProvider;
import christmas.client.utils.render.NametagUtils;
import christmas.client.utils.render.FrostESP;
import christmas.client.utils.render.RenderUtils;
import christmas.client.utils.render.SimpleBlockRenderer;
import christmas.client.utils.render.color.Color;
import christmas.client.utils.render.color.SettingColor;
import christmas.client.utils.render.postprocess.PostProcessShaders;
import christmas.client.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StorageESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final SettingGroup sgOpened = settings.createGroup("Opened Rendering");
    private final SettingGroup sgLabels = settings.createGroup("Labels");
    private final Set<BlockPos> interactedBlocks = new HashSet<>();
    private WeakReference<ClientLevel> cacheLevel = new WeakReference<>(null);

    private final FrostESP frost = new FrostESP();
    private final Setting<Boolean> frostAccents = sgGeneral.add(new BoolSetting.Builder()
        .name("frost-accents")
        .description("Snow caps, icy corners and a snow crystal within 64 blocks. Fades after 48 blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Rendering mode.")
        .defaultValue(Mode.Shader)
        .build()
    );

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
        .name("storage-blocks")
        .description("Select the storage blocks to display.")
        .defaultValue(StorageBlockListSetting.AVAILABLE_STORAGE_BLOCKS)
        .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draws tracers to storage blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maxDistance = sgGeneral.add(new IntSetting.Builder()
        .name("max-distance")
        .description("Maximum distance from the camera in blocks. Zero renders all loaded storage.")
        .defaultValue(0)
        .min(0)
        .sliderMax(512)
        .build()
    );

    private final Setting<Integer> tracerDistance = sgGeneral.add(new IntSetting.Builder()
        .name("tracer-distance")
        .description("Maximum tracer distance in blocks. Zero uses the storage render distance.")
        .defaultValue(0)
        .min(0)
        .sliderMax(512)
        .visible(tracers::get)
        .build()
    );

    private final Setting<Boolean> beams = sgGeneral.add(new BoolSetting.Builder()
        .name("beams")
        .description("Draw a vertical marker above storage blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> beamHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("beam-height")
        .description("Height of the markers in blocks.")
        .defaultValue(3)
        .min(0)
        .sliderMax(16)
        .visible(beams::get)
        .build()
    );

    public final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public final Setting<Integer> fillOpacity = sgGeneral.add(new IntSetting.Builder()
        .name("fill-opacity")
        .description("The opacity of the shape fill.")
        .visible(() -> shapeMode.get() != ShapeMode.Lines)
        .defaultValue(50)
        .range(0, 255)
        .sliderMax(255)
        .build()
    );

    public final Setting<Integer> outlineWidth = sgGeneral.add(new IntSetting.Builder()
        .name("width")
        .description("The width of the shader outline.")
        .visible(() -> mode.get() == Mode.Shader)
        .defaultValue(1)
        .range(1, 10)
        .sliderRange(1, 5)
        .build()
    );

    public final Setting<Double> glowMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("glow-multiplier")
        .description("Multiplier for glow effect")
        .visible(() -> mode.get() == Mode.Shader)
        .decimalPlaces(3)
        .defaultValue(3.5)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<SettingColor> chest = sgGeneral.add(new ColorSetting.Builder()
        .name("chest")
        .description("The color of chests.")
        .defaultValue(new SettingColor(240, 198, 92, 255))
        .build()
    );

    private final Setting<SettingColor> trappedChest = sgGeneral.add(new ColorSetting.Builder()
        .name("trapped-chest")
        .description("The color of trapped chests.")
        .defaultValue(new SettingColor(228, 65, 78, 255))
        .build()
    );

    private final Setting<SettingColor> barrel = sgGeneral.add(new ColorSetting.Builder()
        .name("barrel")
        .description("The color of barrels.")
        .defaultValue(new SettingColor(62, 190, 112, 255))
        .build()
    );

    private final Setting<SettingColor> shulker = sgGeneral.add(new ColorSetting.Builder()
        .name("shulker")
        .description("The color of Shulker Boxes.")
        .defaultValue(new SettingColor(250, 247, 237, 255))
        .build()
    );

    private final Setting<SettingColor> enderChest = sgGeneral.add(new ColorSetting.Builder()
        .name("ender-chest")
        .description("The color of Ender Chests.")
        .defaultValue(new SettingColor(106, 206, 220, 255))
        .build()
    );

    private final Setting<SettingColor> furnace = sgColors.add(new ColorSetting.Builder()
        .name("furnace")
        .description("The color of furnaces, smokers and blast furnaces.")
        .defaultValue(new SettingColor(230, 140, 72, 255))
        .build()
    );

    private final Setting<SettingColor> hopper = sgColors.add(new ColorSetting.Builder()
        .name("hopper")
        .description("The color of hoppers.")
        .defaultValue(new SettingColor(86, 159, 119, 255))
        .build()
    );

    private final Setting<SettingColor> dispenser = sgColors.add(new ColorSetting.Builder()
        .name("dispenser")
        .description("The color of dispensers and droppers.")
        .defaultValue(new SettingColor(201, 104, 113, 255))
        .build()
    );

    private final Setting<SettingColor> brewingStand = sgColors.add(new ColorSetting.Builder()
        .name("brewing-stand")
        .description("The color of brewing stands.")
        .defaultValue(new SettingColor(240, 198, 92, 255))
        .build()
    );

    private final Setting<SettingColor> crafter = sgColors.add(new ColorSetting.Builder()
        .name("crafter")
        .description("The color of crafters.")
        .defaultValue(new SettingColor(62, 190, 112, 255))
        .build()
    );

    private final Setting<SettingColor> other = sgGeneral.add(new ColorSetting.Builder()
        .name("other")
        .description("The color of other storage blocks, including bookshelves, decorated pots and campfires.")
        .defaultValue(new SettingColor(188, 201, 188, 255))
        .build()
    );

    private final Setting<Double> fadeDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fade-distance")
        .description("The distance at which the color will fade.")
        .defaultValue(6)
        .min(0)
        .sliderMax(12)
        .build()
    );

    private final Setting<Boolean> hideOpened = sgOpened.add(new BoolSetting.Builder()
        .name("hide-opened")
        .description("Hides opened containers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> openedColor = sgOpened.add(new ColorSetting.Builder()
        .name("opened-color")
        .description("Optional setting to change colors of opened chests, as opposed to not rendering. Disabled at zero opacity.")
        .defaultValue(new SettingColor(62, 190, 112, 0)) // TRANSPARENT BY DEFAULT.
        .build()
    );

    private final Setting<Boolean> labels = sgLabels.add(new BoolSetting.Builder()
        .name("labels")
        .description("Show the name of each visible storage block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> labelDistance = sgLabels.add(new BoolSetting.Builder()
        .name("label-distance")
        .description("Include distance from the camera in labels.")
        .defaultValue(true)
        .visible(labels::get)
        .build()
    );

    private final Setting<Boolean> labelInteracted = sgLabels.add(new BoolSetting.Builder()
        .name("label-interacted")
        .description("Mark containers you have interacted with.")
        .defaultValue(true)
        .visible(labels::get)
        .build()
    );

    private final Setting<Boolean> labelCoordinates = sgLabels.add(new BoolSetting.Builder()
        .name("label-coordinates")
        .description("Include block coordinates in labels.")
        .defaultValue(false)
        .visible(labels::get)
        .build()
    );

    private final Setting<Boolean> labelDetails = sgLabels.add(new BoolSetting.Builder()
        .name("label-details")
        .description("Show available block states and the items displayed on shelves.")
        .defaultValue(false)
        .visible(labels::get)
        .build()
    );

    private final Setting<Boolean> avoidLabelOverlap = sgLabels.add(new BoolSetting.Builder()
        .name("avoid-label-overlap")
        .description("Stack overlapping labels, prioritizing nearby storage.")
        .defaultValue(true)
        .visible(labels::get)
        .build()
    );

    private final Setting<Integer> labelMaxDistance = sgLabels.add(new IntSetting.Builder()
        .name("label-max-distance")
        .description("Maximum label distance in blocks. Zero uses the storage render distance.")
        .defaultValue(0)
        .min(0)
        .sliderMax(512)
        .visible(labels::get)
        .build()
    );

    private final Setting<Double> labelScale = sgLabels.add(new DoubleSetting.Builder()
        .name("label-scale")
        .description("Scale of storage labels.")
        .defaultValue(1)
        .min(0.1)
        .sliderMax(3)
        .visible(labels::get)
        .build()
    );


    private final Color lineColor = new Color(0, 0, 0, 0);
    private final Color sideColor = new Color(0, 0, 0, 0);
    private final Color labelBackground = new Color(10, 20, 15, 175);
    private final Vector3d labelPos = new Vector3d();
    private final List<LabelTarget> labelTargets = new ArrayList<>();
    private final List<LabelBounds> labelBounds = new ArrayList<>();
    private boolean render;
    private int count;

    private final MeshBuilder mesh;
    private final MeshBuilderVertexConsumerProvider vertexConsumerProvider;

    public StorageESP() {
        super(Categories.Render, "storage-esp", "Renders all specified storage blocks.");

        mesh = new MeshBuilder(ChristmasRenderPipelines.WORLD_COLORED);
        vertexConsumerProvider = new MeshBuilderVertexConsumerProvider(mesh);
    }

    @Override
    public void onDeactivate() {
        count = 0;
        labelTargets.clear();
        labelBounds.clear();
    }

    private void getBlockEntityColor(BlockEntity blockEntity) {
        render = false;

        if (!storageBlocks.get().contains(blockEntity.getType())) return;

        if (blockEntity instanceof TrappedChestBlockEntity)
            lineColor.set(trappedChest.get()); // Must come before ChestBlockEntity as it is the superclass of TrappedChestBlockEntity
        else if (blockEntity instanceof ChestBlockEntity) lineColor.set(chest.get());
        else if (blockEntity instanceof BarrelBlockEntity) lineColor.set(barrel.get());
        else if (blockEntity instanceof ShulkerBoxBlockEntity) lineColor.set(shulker.get());
        else if (blockEntity instanceof EnderChestBlockEntity) lineColor.set(enderChest.get());
        else if (blockEntity instanceof AbstractFurnaceBlockEntity) lineColor.set(furnace.get());
        else if (blockEntity instanceof HopperBlockEntity) lineColor.set(hopper.get());
        else if (blockEntity instanceof DispenserBlockEntity) lineColor.set(dispenser.get());
        else if (blockEntity instanceof BrewingStandBlockEntity) lineColor.set(brewingStand.get());
        else if (blockEntity instanceof CrafterBlockEntity) lineColor.set(crafter.get());
        else lineColor.set(other.get());

        render = true;

        if (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) {
            sideColor.set(lineColor);
            sideColor.a = fillOpacity.get();
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        // Button to Clear Interacted Blocks
        WButton clear = list.add(theme.button("Clear Rendering Cache")).expandX().widget();

        clear.action = interactedBlocks::clear;

        return list;
    }

    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        clearCacheIfWorldChanged();
        BlockPos pos = event.result.getBlockPos();
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);

        if (blockEntity == null) return;

        interactedBlocks.add(pos);
        if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
            BlockState state = chestBlockEntity.getBlockState();
            ChestType chestType = state.getValue(ChestBlock.TYPE);

            if (chestType == ChestType.LEFT || chestType == ChestType.RIGHT) {
                // It's part of a double chest
                Direction facing = state.getValue(ChestBlock.FACING);
                BlockPos otherPartPos = pos.relative(chestType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise());

                interactedBlocks.add(otherPartPos);
            }
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        clearCacheIfWorldChanged();
        if (event.oldState != null && event.oldState.getBlock() != event.newState.getBlock()) {
            interactedBlocks.remove(event.pos);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        clearCacheIfWorldChanged();
        count = 0;

        for (BlockEntity blockEntity : Utils.blockEntities()) {
            double dist = distanceSquared(blockEntity.getBlockPos());
            if (!isVisible(blockEntity, dist)) continue;

            // Check if the block has been interacted with (opened)
            boolean interacted = interactedBlocks.contains(blockEntity.getBlockPos());
            getBlockEntityColor(blockEntity);

            // Set the color to openedColor if its alpha is greater than 0
            if (interacted && openedColor.get().a > 0) {
                // openedColor takes precedence.
                lineColor.set(openedColor.get());
                sideColor.set(openedColor.get());
                sideColor.a = fillOpacity.get(); // Maintain fill opacity setting for consistency
            }

            if (render) {
                double a = fadeAlpha(dist);

                // Only start a mesh when there's something to render
                if (count == 0 && mode.get() == Mode.Shader) {
                    mesh.begin();
                }

                int prevLineA = lineColor.a;
                int prevSideA = sideColor.a;

                lineColor.a *= a;
                sideColor.a *= a;

                if (tracers.get() && isWithinDistance(dist, tracerDistance.get())) {
                    event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5, lineColor);
                }

                if (beams.get()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    event.renderer.line(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 1 + beamHeight.get(), pos.getZ() + 0.5, lineColor);
                }

                if (mode.get() == Mode.Box) {
                    renderBox(event, blockEntity);
                }

                if (mode.get() == Mode.Shader) {
                    renderShader(event, blockEntity);
                }

                if (frostAccents.get()) renderFrost(event, blockEntity);

                lineColor.a = prevLineA;
                sideColor.a = prevSideA;

                count++;
            }
        }

        if (mode.get() == Mode.Shader && count > 0) {
            MeshRenderer.begin()
                .attachments(PostProcessShaders.STORAGE_OUTLINE.framebuffer)
                .clearColor(Color.CLEAR)
                .pipeline(ChristmasRenderPipelines.WORLD_COLORED)
                .mesh(mesh, event.matrices)
                .end();

            PostProcessShaders.STORAGE_OUTLINE.render();
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!labels.get()) return;
        clearCacheIfWorldChanged();
        boolean shadow = Config.get().customFont.get();
        TextRenderer text = TextRenderer.get();

        labelTargets.clear();
        labelBounds.clear();
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            BlockPos pos = blockEntity.getBlockPos();
            double dist = distanceSquared(pos);
            if (!isVisible(blockEntity, dist) || !isWithinDistance(dist, labelMaxDistance.get())) continue;
            labelTargets.add(new LabelTarget(blockEntity, dist));
        }
        labelTargets.sort(Comparator.comparingDouble(LabelTarget::distanceSquared));

        for (LabelTarget target : labelTargets) {
            BlockEntity blockEntity = target.entity();
            BlockPos pos = blockEntity.getBlockPos();
            double dist = target.distanceSquared();

            BlockPos neighbor = visibleChestNeighbor(blockEntity);
            if (neighbor != null && !isWithinDistance(distanceSquared(neighbor), labelMaxDistance.get())) neighbor = null;
            if (neighbor != null && mc.level.getBlockState(pos).getValue(ChestBlock.TYPE) == ChestType.RIGHT) continue;

            double x = pos.getX() + 0.5;
            double z = pos.getZ() + 0.5;
            if (neighbor != null) {
                x = (x + neighbor.getX() + 0.5) / 2;
                z = (z + neighbor.getZ() + 0.5) / 2;
            }
            labelPos.set(x, pos.getY() + 1.15, z);
            if (!NametagUtils.to2D(labelPos, labelScale.get())) continue;

            boolean interacted = interactedBlocks.contains(pos) || (neighbor != null && interactedBlocks.contains(neighbor));
            getBlockEntityColor(blockEntity);
            if (interacted && openedColor.get().a > 0) lineColor.set(openedColor.get());
            double alpha = fadeAlpha(dist);
            lineColor.a = (int) (lineColor.a * alpha);
            labelBackground.a = (int) (175 * alpha);

            String label = describeStorage(blockEntity, labelDetails.get());
            if (labelInteracted.get() && interacted) label += " (interacted)";
            if (labelDistance.get()) label += " [" + (int) Math.sqrt(dist) + "m]";
            if (labelCoordinates.get()) label += " [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
            double width = text.getWidth(label, shadow);
            double height = text.getHeight(shadow);
            if (avoidLabelOverlap.get() && !placeLabel(width, height)) continue;
            NametagUtils.begin(labelPos, event.graphics);
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-width / 2 - 2, -height - 1, width + 4, height + 2, labelBackground);
            Renderer2D.COLOR.render();
            text.beginBig(event.graphics);
            text.render(label, -width / 2, -height, lineColor, shadow);
            text.end();
            NametagUtils.end(event.graphics);
        }
        labelTargets.clear();
        labelBounds.clear();
    }

    private boolean placeLabel(double width, double height) {
        double scale = NametagUtils.scale;
        double left = labelPos.x - (width / 2 + 2) * scale;
        double right = labelPos.x + (width / 2 + 2) * scale;
        if (right < 0 || left > Utils.getWindowWidth()) return false;

        double top = labelPos.y - (height + 1) * scale;
        double bottom = labelPos.y + scale;
        boolean moved;
        do {
            moved = false;
            for (LabelBounds other : labelBounds) {
                if (left < other.right() && right > other.left() && top < other.bottom() && bottom > other.top()) {
                    labelPos.y -= bottom - other.top() + 2;
                    top = labelPos.y - (height + 1) * scale;
                    bottom = labelPos.y + scale;
                    moved = true;
                }
            }
        } while (moved && top >= 0);

        if (top < 0 || bottom > Utils.getWindowHeight()) return false;
        labelBounds.add(new LabelBounds(left, top, right, bottom));
        return true;
    }

    private record LabelTarget(BlockEntity entity, double distanceSquared) {}
    private record LabelBounds(double left, double top, double right, double bottom) {}

    private static String describeStorage(BlockEntity entity, boolean details) {
        BlockState state = entity.getBlockState();
        StringBuilder label = new StringBuilder(state.getBlock().getName().getString());
        if (entity instanceof Nameable named) {
            Component customName = named.getCustomName();
            if (customName != null) label.append(" (").append(customName.getString()).append(")");
        }
        if (!details) return label.toString();

        if (state.getBlock() instanceof AbstractFurnaceBlock) label.append(state.getValue(AbstractFurnaceBlock.LIT) ? " [lit]" : " [unlit]");
        else if (state.getBlock() instanceof CampfireBlock) label.append(state.getValue(CampfireBlock.LIT) ? " [lit]" : " [unlit]");
        else if (state.getBlock() instanceof BarrelBlock) label.append(state.getValue(BarrelBlock.OPEN) ? " [open]" : " [closed]");
        else if (state.getBlock() instanceof LecternBlock) label.append(state.getValue(LecternBlock.HAS_BOOK) ? " [book]" : " [no book]");
        else if (state.getBlock() instanceof JukeboxBlock) label.append(state.getValue(JukeboxBlock.HAS_RECORD) ? " [record]" : " [no record]");
        else if (state.getBlock() instanceof ChiseledBookShelfBlock) {
            int occupied = 0;
            for (var property : ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES) if (state.getValue(property)) occupied++;
            label.append(" [").append(occupied).append("/6 books]");
        }
        if (entity instanceof ShelfBlockEntity shelf) {
            boolean first = true;
            for (ItemStack stack : shelf.getItems()) {
                if (stack.isEmpty()) continue;
                label.append(first ? " [" : ", ");
                label.append(Names.get(stack));
                if (stack.getCount() > 1) label.append(" x").append(stack.getCount());
                first = false;
            }
            label.append(first ? " [empty]" : "]");
        }
        return label.toString();
    }

    private static boolean isWithinDistance(double squaredDistance, int limit) {
        return limit == 0 || squaredDistance <= (double) limit * limit;
    }

    private double distanceSquared(BlockPos pos) {
        return PlayerUtils.squaredDistanceToCamera(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private double fadeAlpha(double squaredDistance) {
        double distance = fadeDistance.get();
        return distance > 0 ? Math.min(1, squaredDistance / (distance * distance)) : 1;
    }

    private boolean isVisible(BlockEntity blockEntity, double squaredDistance) {
        return storageBlocks.get().contains(blockEntity.getType())
            && !(hideOpened.get() && interactedBlocks.contains(blockEntity.getBlockPos()))
            && isWithinDistance(squaredDistance, maxDistance.get())
            && fadeAlpha(squaredDistance) >= 0.075;
    }

    private void clearCacheIfWorldChanged() {
        if (cacheLevel.get() == mc.level) return;

        interactedBlocks.clear();
        cacheLevel = new WeakReference<>(mc.level);
    }

    @Nullable
    private BlockPos visibleChestNeighbor(BlockEntity blockEntity) {
        if (!(blockEntity instanceof ChestBlockEntity)) return null;
        BlockState state = mc.level.getBlockState(blockEntity.getBlockPos());
        if ((state.getBlock() != Blocks.CHEST && state.getBlock() != Blocks.TRAPPED_CHEST) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return null;

        BlockPos pos = blockEntity.getBlockPos().relative(ChestBlock.getConnectedDirection(state));
        BlockState neighbor = mc.level.getBlockState(pos);
        BlockEntity neighborEntity = mc.level.getBlockEntity(pos);
        if (neighbor.getBlock() != state.getBlock() || neighbor.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)
            || neighbor.getValue(ChestBlock.TYPE) != (state.getValue(ChestBlock.TYPE) == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT)
            || neighborEntity == null || !isVisible(neighborEntity, distanceSquared(pos))) return null;
        return pos;
    }


    private void renderBox(Render3DEvent event, BlockEntity blockEntity) {
        double x1 = blockEntity.getBlockPos().getX();
        double y1 = blockEntity.getBlockPos().getY();
        double z1 = blockEntity.getBlockPos().getZ();

        double x2 = blockEntity.getBlockPos().getX() + 1;
        double y2 = blockEntity.getBlockPos().getY() + 1;
        double z2 = blockEntity.getBlockPos().getZ() + 1;

        int excludeDir = 0;
        if (visibleChestNeighbor(blockEntity) != null) excludeDir = Dir.get(ChestBlock.getConnectedDirection(mc.level.getBlockState(blockEntity.getBlockPos())));

        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity) {
            double a = 1.0 / 16.0;

            if (Dir.isNot(excludeDir, Dir.WEST)) x1 += a;
            if (Dir.isNot(excludeDir, Dir.NORTH)) z1 += a;

            if (Dir.isNot(excludeDir, Dir.EAST)) x2 -= a;
            y2 -= a * 2;
            if (Dir.isNot(excludeDir, Dir.SOUTH)) z2 -= a;
        }

        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode.get(), excludeDir);
    }

    private void renderFrost(Render3DEvent event, BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        BlockPos neighbor = visibleChestNeighbor(blockEntity);
        // Draw one ornament around a joined chest rather than two overlapping markers.
        if (neighbor != null && pos.compareTo(neighbor) > 0) return;
        double x1 = neighbor == null ? pos.getX() : Math.min(pos.getX(), neighbor.getX());
        double z1 = neighbor == null ? pos.getZ() : Math.min(pos.getZ(), neighbor.getZ());
        double x2 = (neighbor == null ? pos.getX() : Math.max(pos.getX(), neighbor.getX())) + 1;
        double z2 = (neighbor == null ? pos.getZ() : Math.max(pos.getZ(), neighbor.getZ())) + 1;
        boolean chest = blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity;
        double inset = chest ? 1.0 / 16 : 0;
        frost.render(event.renderer, x1 + inset, pos.getY(), z1 + inset,
            x2 - inset, pos.getY() + (chest ? 0.875 : 1), z2 - inset, lineColor);
    }

    private void renderShader(Render3DEvent event, BlockEntity blockEntity) {
        vertexConsumerProvider.setColor(lineColor);
        SimpleBlockRenderer.renderWithBlockEntity(blockEntity, event.tickDelta, vertexConsumerProvider);
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    public boolean isShader() {
        return isActive() && mode.get() == Mode.Shader;
    }

    public enum Mode {
        Box,
        Shader
    }
}
