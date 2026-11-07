/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.systems;

import christmas.client.ChristmasClient;
import christmas.client.utils.files.StreamUtils;
import christmas.client.utils.misc.ISerializable;
import net.minecraft.ReportedException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public abstract class System<T> implements ISerializable<T> {
    private final String name;
    private File file;

    protected boolean isFirstInit;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);

    public System(String name) {
        this.name = name;

        if (name != null) {
            this.file = new File(ChristmasClient.FOLDER, name + ".nbt");
            this.isFirstInit = !file.exists();
        }
    }

    public void init() {
    }

    public void save(File folder) {
        File file = getFile();
        if (file == null) return;

        CompoundTag tag = toTag();
        if (tag == null) return;

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(ChristmasClient.MOD_ID, file.getName());
            NbtIo.write(tag, tempFile);

            if (folder != null) file = new File(folder, file.getName());

            Files.createDirectories(file.toPath().getParent());

            try {
                Files.move(tempFile, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException _) {
                StreamUtils.copy(tempFile.toFile(), file);
            }
        } catch (IOException e) {
            ChristmasClient.LOG.error("Error saving {}. Possibly corrupted?", this.name, e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    ChristmasClient.LOG.warn("Failed to delete temporary file '{}'.", tempFile, e);
                }
            }
        }
    }

    public void save() {
        save(null);
    }

    public void load(File folder) {
        File file = getFile();
        if (file == null) return;

        try {
            if (folder != null) file = new File(folder, file.getName());

            if (file.exists()) {
                try {
                    fromTag(NbtIo.read(file.toPath()));
                } catch (ReportedException e) {
                    String backupName = FilenameUtils.removeExtension(file.getName()) + "-" + ZonedDateTime.now().format(DATE_TIME_FORMATTER) + ".backup.nbt";
                    File backup = new File(file.getParentFile(), backupName);

                    try {
                        Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException _) {
                        StreamUtils.copy(file, backup);
                    }

                    ChristmasClient.LOG.error("Error loading {}. Possibly corrupted?", this.name, e);
                    ChristmasClient.LOG.info("Saved settings backup to '{}'.", backup);
                }
            }
        } catch (IOException e) {
            ChristmasClient.LOG.error("Error loading {}. Possibly corrupted?", this.name, e);
        }
    }

    public void load() {
        load(null);
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    @Override
    public CompoundTag toTag() {
        return null;
    }

    @Override
    public T fromTag(CompoundTag tag) {
        return null;
    }
}
