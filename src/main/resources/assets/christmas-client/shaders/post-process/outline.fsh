#version 330
#extension GL_ARB_separate_shader_objects : require

layout (location = 0) in vec2 v_TexCoord;
layout (location = 1) in vec2 v_OneTexel;

uniform sampler2D u_Texture;

layout (std140) uniform OutlineData {
    int width;
    float fillOpacity;
    int shapeMode;
    float glowMultiplier;
} u_Outline;

layout (location = 0) out vec4 color;

void main() {
    vec4 center = texture(u_Texture, v_TexCoord);

    if (center.a != 0.0) {
        if (u_Outline.shapeMode == 0) discard;
        center = vec4(center.rgb, center.a * u_Outline.fillOpacity);
    }
    else {
        if (u_Outline.shapeMode == 1) discard;

        float nearestDistanceSquared = 1e20;
        vec4 nearest = vec4(0.0);
        // Include a one-pixel fringe so diagonal edges can fade smoothly.
        int radius = u_Outline.width + 1;
        float radiusSquared = float(radius * radius);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                float distanceSquared = float(x * x + y * y);
                if (distanceSquared > radiusSquared || distanceSquared > nearestDistanceSquared) continue;
                vec4 sampleColor = texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y));
                if (sampleColor.a > 0.0 && (distanceSquared < nearestDistanceSquared
                    || (distanceSquared == nearestDistanceSquared && sampleColor.a > nearest.a))) {
                    nearestDistanceSquared = distanceSquared;
                    nearest = sampleColor;
                }
            }
        }

        if (nearest.a == 0.0) discard;
        float width = float(u_Outline.width);
        float distance = sqrt(nearestDistanceSquared);
        float coverage = 1.0 - smoothstep(width - 0.5, width + 0.5, distance - 0.5);
        float glow = clamp((1.0 - max(distance - 1.0, 0.0) / width) * u_Outline.glowMultiplier, 0.0, 1.0);
        center = vec4(nearest.rgb, nearest.a * coverage * glow);
    }

    color = center;
}
