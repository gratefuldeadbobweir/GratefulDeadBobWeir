/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package christmas.client.renderer;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.*;
import christmas.client.ChristmasClient;
import christmas.client.utils.network.ChristmasExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class ChristmasRenderPipelines {
    private static final List<RenderPipeline> PIPELINES = new ArrayList<>();

    // Snippets

    private static final BindGroupLayout MESH_BIND_GROUP = BindGroupLayout.builder()
        .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
        .build();

    private static final RenderPipeline.Snippet MESH_UNIFORMS = RenderPipeline.builder()
        .withBindGroupLayout(MESH_BIND_GROUP)
        .buildSnippet();

    // World

    public static final RenderPipeline WORLD_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/world_colored"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/pos_color.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/pos_color.fsh"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline WORLD_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLineSmooth()
        .withLocation(ChristmasClient.identifier("pipeline/world_colored_lines"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
        .withVertexShader(ChristmasClient.identifier("shaders/pos_color.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/pos_color.fsh"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline WORLD_COLORED_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/world_colored_depth"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/pos_color.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/pos_color.fsh"))
        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline WORLD_COLORED_LINES_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLineSmooth()
        .withLocation(ChristmasClient.identifier("pipeline/world_colored_lines_depth"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
        .withVertexShader(ChristmasClient.identifier("shaders/pos_color.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/pos_color.fsh"))
        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    // UI

    public static final RenderPipeline UI_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/ui_colored"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/pos_color.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/pos_color.fsh"))
        .withShaderDefine("UI")
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build()
    );

    public static final RenderPipeline UI_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/ui_colored_lines"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
        .withVertexShader(ChristmasClient.identifier("shaders/pos_color.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/pos_color.fsh"))
        .withShaderDefine("UI")
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build()
    );

    public static final RenderPipeline UI_TEXTURED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/ui_textured"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2_TEXTURE_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/pos_tex_color.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/pos_tex_color.fsh"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build()
    );

    public static final RenderPipeline UI_TEXT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/ui_text"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2_TEXTURE_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/text.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/text.fsh"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build()
    );

    // Post Process

    public static final RenderPipeline POST_OUTLINE = add(new ExtendedRenderPipelineBuilder()
        .withLocation(ChristmasClient.identifier("pipeline/post/outline"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/post-process/base.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/post-process/outline.fsh"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("PostData", UniformType.UNIFORM_BUFFER)
            .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline POST_IMAGE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/post/image"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/post-process/base.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/post-process/image.fsh"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("u_TextureI", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("PostData", UniformType.UNIFORM_BUFFER)
            .withUniform("ImageData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    // Blur

    public static final RenderPipeline BLUR_DOWN = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/blur/down"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/blur.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/blur_down.fsh"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline BLUR_UP = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/blur/up"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/blur.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/blur_up.fsh"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline BLUR_PASSTHROUGH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(ChristmasClient.identifier("pipeline/blur/up"))
        .withVertexBinding(0, ChristmasVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(ChristmasClient.identifier("shaders/passthrough.vsh"))
        .withFragmentShader(ChristmasClient.identifier("shaders/passthrough.fsh"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    private static RenderPipeline add(RenderPipeline pipeline) {
        PIPELINES.add(pipeline);
        return pipeline;
    }

    public static void precompile(PipelineCache cache) {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        final Map<Identifier, ShaderSource.CachedIncludeSource> includes = ShaderManager.listAllIncludes(resources);
        ShaderSource shaderSource = new ShaderSource() {
            @Override
            public @Nullable String getShader(@NonNull Identifier id, @NonNull ShaderType type) {
                var resource = resources.getResource(id).get();

                try (var in = resource.open()) {
                    return IOUtils.toString(in, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public @Nullable CachedIncludeSource getInclude(@NonNull Identifier id) {
                return includes.get(id);
            }

            @Override
            public void close() {
                includes.values().forEach(ShaderSource.CachedIncludeSource::close);
            }
        };

        for (RenderPipeline pipeline : PIPELINES) {
            CompletableFuture<CompiledRenderPipeline.Pending> pendingPipeline = device.compilePipeline(pipeline, shaderSource, ChristmasExecutor.executor);

            CompiledRenderPipeline compiled = pendingPipeline.join().finishCompile();
            cache.insert(pipeline, compiled);
        }
    }

    private ChristmasRenderPipelines() {
    }
}
