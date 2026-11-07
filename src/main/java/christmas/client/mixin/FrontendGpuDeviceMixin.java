/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.mixin;

import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import christmas.client.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FrontendGpuDevice.class)
public abstract class FrontendGpuDeviceMixin implements IGpuDevice {
    @Shadow
    @Final
    private GpuDeviceBackend backend;

    @Override
    public void christmas$pushScissor(int x, int y, int width, int height) {
        ((IGpuDevice) backend).christmas$pushScissor(x, y, width, height);
    }

    @Override
    public void christmas$popScissor() {
        ((IGpuDevice) backend).christmas$popScissor();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void christmas$onCreateRenderPass(RenderPassBackend backend) {
        ((IGpuDevice) this.backend).christmas$onCreateRenderPass(backend);
    }
}
