/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("fps")
    static int christmas$getFps() {
        return 0;
    }

    @Mutable
    @Accessor("user")
    void christmas$setUser(User session);

    @Accessor("reloadStateTracker")
    ResourceLoadStateTracker christmas$getReloadStateTracker();

    @Accessor("missTime")
    int christmas$getMissTime();

    @Accessor("missTime")
    void christmas$setMissTime(int attackCooldown);

    @Invoker("startAttack")
    boolean christmas$leftClick();

    @Mutable
    @Accessor("profileKeyPairManager")
    void christmas$setProfileKeyPairManager(ProfileKeyPairManager keys);

    @Mutable
    @Accessor("userApiService")
    void christmas$setUserApiService(UserApiService apiService);

    @Mutable
    @Accessor("skinManager")
    void christmas$setSkinManager(SkinManager skinProvider);

    @Mutable
    @Accessor("playerSocialManager")
    void christmas$setPlayerSocialManager(PlayerSocialManager socialInteractionsManager);

    @Mutable
    @Accessor("reportingContext")
    void christmas$setReportingContext(ReportingContext abuseReportContext);

    @Mutable
    @Accessor("profileFuture")
    void christmas$setProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("services")
    void christmas$setServices(Services apiServices);

    @Invoker("handleKeybinds")
    void christmas$handleInputEvents();
}
