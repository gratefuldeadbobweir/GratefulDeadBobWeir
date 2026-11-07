package christmas.client.utils.render.postprocess;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import christmas.client.mixininterface.ILevelRenderer;
import net.minecraft.world.entity.Entity;

import static christmas.client.ChristmasClient.mc;

public abstract class EntityShader extends PostProcessShader {
    protected EntityShader(RenderPipeline pipeline) {
        super(pipeline);
    }

    public abstract boolean shouldDraw(Entity entity);

    @Override
    protected void preDraw() {
        ((ILevelRenderer) mc.levelRenderer).christmas$pushEntityOutlineFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        ((ILevelRenderer) mc.levelRenderer).christmas$popEntityOutlineFramebuffer();
    }

    public void submitVertices() {
    }
}
