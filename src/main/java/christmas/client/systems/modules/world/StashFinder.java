/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.systems.modules.world;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import christmas.client.ChristmasClient;
import christmas.client.events.render.Render3DEvent;
import christmas.client.events.world.BlockUpdateEvent;
import christmas.client.events.world.ChunkDataEvent;
import christmas.client.events.world.TickEvent;
import christmas.client.gui.GuiTheme;
import christmas.client.gui.WindowScreen;
import christmas.client.gui.widgets.WWidget;
import christmas.client.gui.widgets.containers.WHorizontalList;
import christmas.client.gui.widgets.containers.WTable;
import christmas.client.gui.widgets.containers.WVerticalList;
import christmas.client.gui.widgets.pressable.WButton;
import christmas.client.gui.widgets.pressable.WCheckbox;
import christmas.client.gui.widgets.pressable.WMinus;
import christmas.client.pathing.PathManagers;
import christmas.client.settings.*;
import christmas.client.systems.modules.Categories;
import christmas.client.systems.modules.Module;
import christmas.client.utils.Utils;
import christmas.client.utils.misc.Keybind;
import christmas.client.utils.misc.text.RunnableClickEvent;
import christmas.client.utils.player.ChatUtils;
import christmas.client.utils.render.ChristmasToast;
import christmas.client.utils.render.RenderUtils;
import christmas.client.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class StashFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private static final List<Block> DEFAULT_SUPPORT_BLOCK_BLACKLIST = List.of(
        Blocks.COPPER_BLOCK.weathering().oxidized(),
        Blocks.CUT_COPPER.weathering().oxidized(),
        Blocks.TUFF_BRICKS,
        Blocks.COPPER_BLOCK.waxed().unaffected(),
        Blocks.COPPER_BLOCK.waxed().oxidized(),
        Blocks.CUT_COPPER.waxed().oxidized(),
        Blocks.BARREL,
        Blocks.COPPER_BULB.waxed().unaffected()
    );

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
        .name("storage-blocks")
        .description("Select the storage blocks to search for.")
        .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
        .build()
    );

    private final Setting<Integer> minimumStorageCount = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-storage-count")
        .description("The minimum amount of storage blocks in a chunk to record the chunk.")
        .defaultValue(4)
        .min(1)
        .sliderMin(1)
        .build()
    );

    private final Setting<List<Block>> blacklistedBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blacklisted-support-blocks")
        .description("Blocks that prevent counting a storage block entity when it sits on them.")
        .defaultValue(DEFAULT_SUPPORT_BLOCK_BLACKLIST)
        .build()
    );

    private final Setting<Integer> minimumDistance = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-distance")
        .description("Minimum horizontal distance of the chunk from coordinate 0, 0.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10000)
        .build()
    );

    private final Setting<Integer> rescanInterval = sgGeneral.add(new IntSetting.Builder()
        .name("rescan-interval")
        .description("Seconds between loaded chunk sweeps. Zero disables automatic rescans.")
        .defaultValue(10)
        .min(0)
        .sliderMax(120)
        .build()
    );

    private final Setting<Integer> rescanBudget = sgGeneral.add(new IntSetting.Builder()
        .name("rescan-chunks-per-tick")
        .description("Maximum loaded chunks to rescan per tick, including block updates.")
        .defaultValue(8)
        .range(1, 128)
        .sliderRange(1, 32)
        .visible(() -> rescanInterval.get() > 0)
        .build()
    );

    private final Setting<Boolean> sendNotifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Sends Minecraft notifications when new stashes are found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Mode> notificationMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("notification-mode")
        .description("The mode to use for notifications.")
        .defaultValue(Mode.Both)
        .visible(sendNotifications::get)
        .build()
    );

    private final Setting<Boolean> renderTracer = sgRender.add(new BoolSetting.Builder()
        .name("render-tracer")
        .description("Renders a tracer to the last found stash.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> traceColor = sgRender.add(new ColorSetting.Builder()
        .name("tracer-color")
        .description("Color of the stash tracer.")
        .defaultValue(new SettingColor(255, 215, 0, 255))
        .visible(renderTracer::get)
        .build()
    );

    private final Setting<Integer> traceArrivalDistance = sgRender.add(new IntSetting.Builder()
        .name("tracer-hide-at-distance")
        .description("Hide the trace when you are this close to the stash.")
        .defaultValue(16)
        .min(1)
        .sliderMin(1)
        .sliderMax(50)
        .visible(renderTracer::get)
        .build()
    );

    private final Setting<Integer> traceMaxDistance = sgRender.add(new IntSetting.Builder()
        .name("tracer-max-distance")
        .description("Hide the trace when you are farther than this distance from the stash.")
        .defaultValue(2000)
        .min(10)
        .sliderMin(50)
        .sliderMax(10000)
        .visible(renderTracer::get)
        .build()
    );

    private final Setting<Boolean> renderChunkColumn = sgRender.add(new BoolSetting.Builder()
        .name("render-chunk-column")
        .description("Renders a vertical column at the center of traced chunks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> traceColumnColor = sgRender.add(new ColorSetting.Builder()
        .name("chunk-column-color")
        .description("Color of the stash tracer column.")
        .defaultValue(new SettingColor(255, 215, 0, 100))
        .visible(renderChunkColumn::get)
        .build()
    );

    private final Setting<Keybind> clearTracesBind = sgRender.add(new KeybindSetting.Builder()
        .name("clear-traces-bind")
        .description("Keybind to clear all stash traces.")
        .defaultValue(Keybind.none())
        .build()
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private record CsvField(String name, ToIntFunction<Chunk> getter, ObjIntConsumer<Chunk> setter) {
        public CsvField(String name, ToIntFunction<Chunk> getter) {
            this(name, getter, null);
        }
    }

    private static final List<CsvField> CSV_FIELDS = List.of(
        new CsvField("x", c -> c.chunkPos.x()),
        new CsvField("z", c -> c.chunkPos.z()),
        new CsvField("chests", c -> c.chests, (c, v) -> c.chests = v),
        new CsvField("barrels", c -> c.barrels, (c, v) -> c.barrels = v),
        new CsvField("shulkers", c -> c.shulkers, (c, v) -> c.shulkers = v),
        new CsvField("enderChests", c -> c.enderChests, (c, v) -> c.enderChests = v),
        new CsvField("furnaces", c -> c.furnaces, (c, v) -> c.furnaces = v),
        new CsvField("dispensersDroppers", c -> c.dispensersDroppers, (c, v) -> c.dispensersDroppers = v),
        new CsvField("hoppers", c -> c.hoppers, (c, v) -> c.hoppers = v),
        new CsvField("other", c -> c.other, (c, v) -> c.other = v)
    );

    private final Map<ChunkPos, Vec3> tracerPositions = new HashMap<>();
    public List<Chunk> chunks = new ArrayList<>();
    private boolean canRecord;
    private ClientLevel scanLevel;
    private String scanWorldName;
    private final Set<ChunkPos> pendingUpdates = new LinkedHashSet<>();
    private static final int MAX_PENDING_UPDATES = 4096;
    private Iterator<ChunkAccess> sweep;
    private int sweepDelay;

    public StashFinder() {
        super(Categories.World, "stash-finder", "Searches loaded chunks for storage blocks. Saves to <your minecraft folder>/christmas-client");
    }

    @Override
    public void onActivate() {
        resetScanState();
        scanLevel = mc.level;
        scanWorldName = Utils.getFileWorldName();
        canRecord = load();
        if (!canRecord) {
            error("Could not load saved stashes. Recording is paused to protect the saved files.");
            return;
        }

        boolean changed = false;
        for (var loadedChunk : Utils.chunks()) {
            if (loadedChunk instanceof LevelChunk levelChunk) changed |= scanChunk(levelChunk, true);
        }
        if (changed) {
            saveJson();
            saveCsv();
        }
    }

    @Override
    public void onDeactivate() {
        resetScanState();
        scanLevel = null;
        scanWorldName = null;
    }

    private void resetScanState() {
        pendingUpdates.clear();
        sweep = null;
        sweepDelay = 0;
    }

    private boolean ensureWorld() {
        if (mc.level == null) return false;
        if (scanLevel != mc.level) {
            resetScanState();
            scanLevel = mc.level;
            tracerPositions.clear();
            String worldName = Utils.getFileWorldName();
            if (!Objects.equals(scanWorldName, worldName)) {
                scanWorldName = worldName;
                canRecord = load();
                if (!canRecord) error("Could not load saved stashes. Recording is paused to protect the saved files.");
            }
        }
        return canRecord;
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (rescanInterval.get() == 0 || !ensureWorld() || event.oldState == null) return;
        if (event.oldState.getBlock() == event.newState.getBlock()) return;
        if (!event.oldState.hasBlockEntity() && !event.newState.hasBlockEntity()
            && !blacklistedBlocks.get().contains(event.oldState.getBlock())
            && !blacklistedBlocks.get().contains(event.newState.getBlock())) return;
        if (pendingUpdates.size() < MAX_PENDING_UPDATES) {
            pendingUpdates.add(new ChunkPos(event.pos.getX() >> 4, event.pos.getZ() >> 4));
        }
        // A later sweep also covers updates dropped when the queue is full.
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (clearTracesBind.get().isPressed()) tracerPositions.clear();
        if (!ensureWorld()) return;
        if (rescanInterval.get() == 0) {
            resetScanState();
            return;
        }

        boolean changed = false;
        int budget = rescanBudget.get();
        Iterator<ChunkPos> pending = pendingUpdates.iterator();
        while (budget > 0 && pending.hasNext()) {
            ChunkPos pos = pending.next();
            pending.remove();
            budget--;
            ChunkAccess chunk = mc.level.getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
            if (chunk instanceof LevelChunk loaded) changed |= scanChunk(loaded, false);
        }

        if (sweep == null && sweepDelay > 0) sweepDelay--;
        if (sweep == null && sweepDelay == 0) sweep = Utils.chunks().iterator();
        while (budget > 0 && sweep != null && sweep.hasNext()) {
            ChunkAccess chunk = sweep.next();
            budget--;
            if (chunk instanceof LevelChunk loaded
                && mc.level.getChunk(loaded.getPos().x(), loaded.getPos().z(), ChunkStatus.FULL, false) == loaded) {
                changed |= scanChunk(loaded, false);
            }
        }
        if (sweep != null && !sweep.hasNext()) {
            sweep = null;
            sweepDelay = (int) Math.min(Integer.MAX_VALUE, (long) rescanInterval.get() * 20);
        }
        if (changed) {
            saveJson();
            saveCsv();
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        if (!ensureWorld()) return;
        if (scanChunk(event.chunk(), true)) {
            saveJson();
            saveCsv();
        }
    }

    private boolean scanChunk(LevelChunk levelChunk, boolean refreshTrace) {
        // Check the distance.
        double chunkX = levelChunk.getPos().x() * 16.0;
        double chunkZ = levelChunk.getPos().z() * 16.0;
        if (Math.hypot(chunkX, chunkZ) < minimumDistance.get()) return false;

        Chunk chunk = new Chunk(levelChunk.getPos());

        List<Block> blockBlacklist = blacklistedBlocks.get();

        for (BlockEntity blockEntity : levelChunk.getBlockEntities().values()) {
            if (!storageBlocks.get().contains(blockEntity.getType())) continue;

            if (!blockBlacklist.isEmpty()) {
                BlockPos below = blockEntity.getBlockPos().below();
                if (blockBlacklist.contains(levelChunk.getBlockState(below).getBlock())) continue;
            }

            switch (blockEntity) {
                case ChestBlockEntity _ -> chunk.chests++;
                case BarrelBlockEntity _ -> chunk.barrels++;
                case ShulkerBoxBlockEntity _ -> chunk.shulkers++;
                case EnderChestBlockEntity _ -> chunk.enderChests++;
                case AbstractFurnaceBlockEntity _ -> chunk.furnaces++;
                case DispenserBlockEntity _ -> chunk.dispensersDroppers++;
                case HopperBlockEntity _ -> chunk.hoppers++;
                default -> chunk.other++;
            }
        }

        if (chunk.getTotal() >= minimumStorageCount.get()) {
            Chunk prevChunk = null;
            int i = chunks.indexOf(chunk);

            if (i < 0) chunks.add(chunk);
            else prevChunk = chunks.set(i, chunk);

            boolean changed = !chunk.countsEqual(prevChunk);
            if (renderTracer.get() && (changed || refreshTrace)) {
                double y = mc.player != null ? mc.player.getEyeY() : 0.0;
                tracerPositions.put(chunk.chunkPos, new Vec3(chunk.chunkPos.getMiddleBlockX(), y, chunk.chunkPos.getMiddleBlockZ()));
            }

            if (sendNotifications.get() && changed) {
                switch (notificationMode.get()) {
                    case Chat -> sendChatNotification(chunk);
                    case Toast -> {
                        ChristmasToast toast = new ChristmasToast.Builder(title).icon(Items.CHEST).text("Found Stash!").build();
                        mc.gui.toastManager().addToast(toast);
                    }
                    case Both -> {
                        sendChatNotification(chunk);
                        ChristmasToast toast = new ChristmasToast.Builder(title).icon(Items.CHEST).text("Found Stash!").build();
                        mc.gui.toastManager().addToast(toast);
                    }
                }
            }
            return changed;
        }
        return false;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        // Sort
        chunks.sort(Comparator.comparingInt(value -> -value.getTotal()));

        WVerticalList list = theme.verticalList();

        // Clear buttons
        WHorizontalList hl = theme.horizontalList();
        WButton clear = hl.add(theme.button("Clear Chunks")).widget();
        WButton resetTracers = hl.add(theme.button("Reset Tracers")).widget();

        list.add(hl);

        WTable table = new WTable();
        if (!chunks.isEmpty()) list.add(table);

        clear.action = () -> {
            chunks.clear();
            table.clear();
            tracerPositions.clear();
            canRecord = true;
            saveJson();
            saveCsv();
        };

        resetTracers.action = () -> {
            table.clear();
            tracerPositions.clear();
            fillTable(theme, table);
        };

        // Chunks
        fillTable(theme, table);

        return list;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        for (Chunk chunk : chunks) {
            table.add(theme.label("Pos: " + chunk.chunkPos.getMiddleBlockX() + ", " + chunk.chunkPos.getMiddleBlockZ())).padRight(10);
            table.add(theme.label("Total: " + chunk.getTotal())).padRight(10);

            WCheckbox visible = table.add(theme.checkbox(tracerPositions.containsKey(chunk.chunkPos))).widget();
            visible.action = () -> {
                if (visible.checked) {
                    double y = mc.player != null ? mc.player.getEyeY() : 0.0;
                    tracerPositions.put(chunk.chunkPos, new Vec3(chunk.chunkPos.getMiddleBlockX(), y, chunk.chunkPos.getMiddleBlockZ()));
                } else tracerPositions.remove(chunk.chunkPos);
            };

            WButton open = table.add(theme.button("Open")).widget();
            open.action = () -> mc.gui.setScreen(new ChunkScreen(theme, chunk));

            WButton gotoBtn = table.add(theme.button("Goto")).widget();
            gotoBtn.action = () -> PathManagers.get().moveTo(new BlockPos(chunk.chunkPos.getMiddleBlockX(), 0, chunk.chunkPos.getMiddleBlockZ()), true);

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                if (chunks.remove(chunk)) {
                    tracerPositions.remove(chunk.chunkPos);
                    table.clear();
                    fillTable(theme, table);

                    saveJson();
                    saveCsv();
                }
            };

            table.row();
        }
    }

    private boolean load() {
        chunks = new ArrayList<>();
        tracerPositions.clear();

        // Try to load json
        Path jsonFile = getJsonFile();
        if (Files.exists(jsonFile)) {
            try (BufferedReader reader = Files.newBufferedReader(jsonFile)) {
                List<Chunk> savedChunks = GSON.fromJson(reader, new TypeToken<List<Chunk>>() {
                }.getType());
                if (savedChunks != null) {
                    chunks = savedChunks;
                    return true;
                }
            } catch (Exception e) {
                ChristmasClient.LOG.error("Error while reading the stash list from json", e);
            }
        }

        // Try to load csv
        Path csvFile = getCsvFile();
        if (Files.exists(csvFile)) {
            try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
                if (reader.readLine() == null) throw new IllegalStateException("Missing CSV header");
                List<Chunk> savedChunks = new ArrayList<>();

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = StringUtils.split(line, ',');
                    if (values.length != CSV_FIELDS.size() && values.length != CSV_FIELDS.size() - 1) {
                        throw new IllegalStateException("Invalid CSV row length: " + values.length);
                    }

                    Chunk chunk = new Chunk(new ChunkPos(Integer.parseInt(values[0]), Integer.parseInt(values[1])));

                    for (int i = 2; i < values.length; i++) {
                        CSV_FIELDS.get(i).setter().accept(chunk, Integer.parseInt(values[i]));
                    }

                    savedChunks.add(chunk);
                }
                chunks = savedChunks;
                return true;
            } catch (Exception e) {
                ChristmasClient.LOG.error("Error while reading the stash list from csv", e);
            }
        }
        return !Files.exists(jsonFile) && !Files.exists(csvFile);
    }

    private void saveCsv() {
        try {
            Path csvFile = getCsvFile();
            Files.createDirectories(csvFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(csvFile)) {
                String header = CSV_FIELDS.stream()
                    .map(CsvField::name)
                    .collect(Collectors.joining(","));
                writer.write(header);
                writer.newLine();
                for (Chunk chunk : chunks) {
                    chunk.write(writer);
                }
            }
        } catch (IOException e) {
            ChristmasClient.LOG.error("Error while writing the stash list to csv", e);
        }
    }

    private void saveJson() {
        try {
            Path jsonFile = getJsonFile();
            Files.createDirectories(jsonFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(jsonFile)) {
                GSON.toJson(chunks, writer);
            }
        } catch (IOException e) {
            ChristmasClient.LOG.error("Error while writing the stash list to json", e);
        }
    }

    private Path getJsonFile() {
        return Path.of(ChristmasClient.FOLDER.getPath(), "stashes", Utils.getFileWorldName(), "stashes.json");
    }

    private Path getCsvFile() {
        return Path.of(ChristmasClient.FOLDER.getPath(), "stashes", Utils.getFileWorldName(), "stashes.csv");
    }

    @Override
    public String getInfoString() {
        return String.valueOf(chunks.size());
    }

    private void sendChatNotification(Chunk chunk) {
        MutableComponent coords = Component.literal(chunk.chunkPos.getMiddleBlockX() + ", " + chunk.chunkPos.getMiddleBlockZ())
            .setStyle(Style.EMPTY
                .withColor(ChatFormatting.WHITE)
                .applyFormat(ChatFormatting.UNDERLINE)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Path to stash")))
                .withClickEvent(new RunnableClickEvent(() -> PathManagers.get().moveTo(new BlockPos(chunk.chunkPos.getMiddleBlockX(), 0, chunk.chunkPos.getMiddleBlockZ()), true))));

        MutableComponent message = Component.literal("Found stash at ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
            .append(coords)
            .append(Component.literal("]").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(" (" + chunk.getTotal() + " storage blocks).").withStyle(ChatFormatting.GRAY));

        ChatUtils.sendMsg(message);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (tracerPositions.isEmpty() || mc.player == null) return;

        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        tracerPositions.entrySet().removeIf(entry -> {
            Vec3 pos = entry.getValue();
            double horizontalDist = Math.hypot(pos.x - playerX, pos.z - playerZ);
            return horizontalDist <= traceArrivalDistance.get();
        });

        if (!renderTracer.get() && !renderChunkColumn.get()) return;

        for (Vec3 pos : tracerPositions.values()) {
            double horizontalDist = Math.hypot(pos.x - playerX, pos.z - playerZ);
            if (horizontalDist > traceMaxDistance.get()) continue;

            if (renderTracer.get()) {
                event.renderer.line(
                    RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, pos.x, mc.player.getEyeY(), pos.z, traceColor.get()
                );
            }

            if (renderChunkColumn.get()) {
                double x1 = pos.x - 0.5;
                double x2 = pos.x + 0.5;
                double z1 = pos.z - 0.5;
                double z2 = pos.z + 0.5;

                int bottomY = mc.level.getMinY();
                int topY = bottomY + mc.level.dimensionType().height();

                event.renderer.line(x1, bottomY, z1, x1, topY, z1, traceColumnColor.get());
                event.renderer.line(x1, bottomY, z2, x1, topY, z2, traceColumnColor.get());
                event.renderer.line(x2, bottomY, z1, x2, topY, z1, traceColumnColor.get());
                event.renderer.line(x2, bottomY, z2, x2, topY, z2, traceColumnColor.get());
            }
        }
    }

    public enum Mode {
        Chat,
        Toast,
        Both
    }

    public static class Chunk {
        public final ChunkPos chunkPos;
        public int chests, barrels, shulkers, enderChests, furnaces, dispensersDroppers, hoppers, other;

        public Chunk(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
        }

        public int getTotal() {
            return chests + barrels + shulkers + enderChests + furnaces + dispensersDroppers + hoppers + other;
        }

        public void write(BufferedWriter writer) throws IOException {
            String line = CSV_FIELDS.stream()
                .map(field -> String.valueOf(field.getter().applyAsInt(this)))
                .collect(Collectors.joining(","));
            writer.write(line);
            writer.newLine();
        }

        public boolean countsEqual(Chunk c) {
            if (c == null) return false;
            return chests == c.chests && barrels == c.barrels && shulkers == c.shulkers && enderChests == c.enderChests && furnaces == c.furnaces && dispensersDroppers == c.dispensersDroppers && hoppers == c.hoppers && other == c.other;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Chunk chunk = (Chunk) o;
            return Objects.equals(chunkPos, chunk.chunkPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkPos);
        }
    }

    private static class ChunkScreen extends WindowScreen {
        private final Chunk chunk;

        public ChunkScreen(GuiTheme theme, Chunk chunk) {
            super(theme, "Chunk at " + chunk.chunkPos.getMiddleBlockX() + ", " + chunk.chunkPos.getMiddleBlockZ());

            this.chunk = chunk;
        }

        @Override
        public void initWidgets() {
            WTable t = add(theme.table()).expandX().widget();

            // Total
            t.add(theme.label("Total:"));
            t.add(theme.label(chunk.getTotal() + ""));
            t.row();

            t.add(theme.horizontalSeparator()).expandX();
            t.row();

            // Separate
            t.add(theme.label("Chests:"));
            t.add(theme.label(chunk.chests + ""));
            t.row();

            t.add(theme.label("Barrels:"));
            t.add(theme.label(chunk.barrels + ""));
            t.row();

            t.add(theme.label("Shulkers:"));
            t.add(theme.label(chunk.shulkers + ""));
            t.row();

            t.add(theme.label("Ender Chests:"));
            t.add(theme.label(chunk.enderChests + ""));
            t.row();

            t.add(theme.label("Furnaces:"));
            t.add(theme.label(chunk.furnaces + ""));
            t.row();

            t.add(theme.label("Dispensers and droppers:"));
            t.add(theme.label(chunk.dispensersDroppers + ""));
            t.row();

            t.add(theme.label("Hoppers:"));
            t.add(theme.label(chunk.hoppers + ""));
            t.row();

            t.add(theme.label("Other:"));
            t.add(theme.label(chunk.other + ""));
        }
    }
}
