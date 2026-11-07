/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.systems.accounts;

import com.mojang.util.UndashedUuid;
import christmas.client.utils.misc.ISerializable;
import christmas.client.utils.misc.NbtException;
import christmas.client.utils.network.ChristmasExecutor;
import christmas.client.utils.render.PlayerHeadTexture;
import christmas.client.utils.render.PlayerHeadUtils;
import net.minecraft.nbt.CompoundTag;

import static christmas.client.ChristmasClient.mc;

public class AccountCache implements ISerializable<AccountCache> {
    public String username = "";
    public String uuid = "";
    private PlayerHeadTexture headTexture;
    private volatile boolean loadingHead;

    public PlayerHeadTexture getHeadTexture() {
        return headTexture != null ? headTexture : PlayerHeadUtils.STEVE_HEAD;
    }

    public void loadHead() {
        loadHead(null);
    }

    public void loadHead(Runnable callback) {
        if (headTexture != null || uuid == null || uuid.isBlank()) {
            if (callback != null) mc.execute(callback);
            return;
        }

        if (loadingHead) return;

        loadingHead = true;

        ChristmasExecutor.execute(() -> {
            byte[] head = PlayerHeadUtils.fetchHead(UndashedUuid.fromStringLenient(uuid));

            mc.execute(() -> {
                if (head != null) headTexture = new PlayerHeadTexture(head, true);
                loadingHead = false;
                if (callback != null) callback.run();
            });
        });
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("username", username);
        tag.putString("uuid", uuid);

        return tag;
    }

    @Override
    public AccountCache fromTag(CompoundTag tag) {
        if (tag.getString("username").isEmpty() || tag.getString("uuid").isEmpty()) throw new NbtException();

        username = tag.getString("username").get();
        uuid = tag.getString("uuid").get();
        loadHead();

        return this;
    }
}
