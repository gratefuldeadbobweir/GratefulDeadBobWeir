/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.addons;

import christmas.client.ChristmasClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import java.util.ArrayList;
import java.util.List;

public class AddonManager {
    public static final List<ChristmasAddon> ADDONS = new ArrayList<>();

    public static void init() {
        // Christmas pseudo addon
        {
            ChristmasClient.ADDON = new ChristmasAddon() {
                @Override
                public void onInitialize() {}

                @Override
                public String getPackage() {
                    return "christmas.client";
                }

                @Override
                public String getWebsite() {
                    return "https://github.com/gratefuldeadbobweir/GratefulDeadBobWeir";
                }

                @Override
                public GithubRepo getRepo() {
                    return new GithubRepo("gratefuldeadbobweir", "GratefulDeadBobWeir");
                }

                @Override
                public String getCommit() {
                    String commit = ChristmasClient.MOD_META.getCustomValue(ChristmasClient.MOD_ID + ":commit").getAsString();
                    return commit.isEmpty() ? null : commit;
                }
            };

            ModMetadata metadata = FabricLoader.getInstance().getModContainer(ChristmasClient.MOD_ID).get().getMetadata();

            ChristmasClient.ADDON.name = metadata.getName();
            ChristmasClient.ADDON.authors = new String[metadata.getAuthors().size()];
            if (metadata.containsCustomValue(ChristmasClient.MOD_ID + ":color")) {
                ChristmasClient.ADDON.color.parse(metadata.getCustomValue(ChristmasClient.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                ChristmasClient.ADDON.authors[i++] = author.getName();
            }

            ADDONS.add(ChristmasClient.ADDON);
        }

        // Addons
        for (EntrypointContainer<ChristmasAddon> entrypoint : FabricLoader.getInstance().getEntrypointContainers("christmas", ChristmasAddon.class)) {
            ModMetadata metadata = entrypoint.getProvider().getMetadata();
            ChristmasAddon addon;
            try {
                addon = entrypoint.getEntrypoint();
            } catch (Throwable throwable) {
                throw new RuntimeException("Exception during addon init \"%s\".".formatted(metadata.getName()), throwable);
            }

            addon.name = metadata.getName();

            if (metadata.getAuthors().isEmpty()) throw new RuntimeException("Addon \"%s\" requires at least 1 author to be defined in it's fabric.mod.json. See https://fabricmc.net/wiki/documentation:fabric_mod_json_spec".formatted(addon.name));
            addon.authors = new String[metadata.getAuthors().size()];

            if (metadata.containsCustomValue(ChristmasClient.MOD_ID + ":color")) {
                addon.color.parse(metadata.getCustomValue(ChristmasClient.MOD_ID + ":color").getAsString());
            }

            int i = 0;
            for (Person author : metadata.getAuthors()) {
                addon.authors[i++] = author.getName();
            }

            ADDONS.add(addon);
        }
    }
}
