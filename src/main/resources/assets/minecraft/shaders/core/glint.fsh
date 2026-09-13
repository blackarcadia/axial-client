#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 sampleColor = texture(Sampler0, texCoord0);
    vec4 color;
    if (ColorModulator.a < 0.0) {
        // Preserve the animated texture's brightness without its baked-in purple tint.
        float brightness = max(sampleColor.r, max(sampleColor.g, sampleColor.b));
        color = vec4(vec3(brightness) * ColorModulator.rgb, sampleColor.a);
    } else {
        color = sampleColor * ColorModulator;
    }
    if (color.a < 0.1) {
        discard;
    }
    float fade = (1.0 - total_fog_value(sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd)) * GlintAlpha;
    fragColor = vec4(color.rgb * fade, color.a);
}
