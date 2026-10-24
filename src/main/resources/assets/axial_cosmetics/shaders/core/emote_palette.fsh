#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
uniform sampler2D Sampler0;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec4 source = texture(Sampler0, texCoord0);
    if (source.a == 0.0) discard;
    // Preserve the original texture's shape, shading and alpha. The supplied
    // vertex colour is the shared menu border (idle) or purple brand (hover).
    float high = max(source.r, max(source.g, source.b));
    float low = min(source.r, min(source.g, source.b));
    float edge = max(smoothstep(0.10, 0.42, high - low), smoothstep(0.50, 0.90, high));
    vec3 charcoal = vec3(27.0, 31.0, 36.0) / 255.0;
    float shading = clamp(dot(source.rgb, vec3(0.2126, 0.7152, 0.0722)) / 0.14, 0.70, 1.20);
    fragColor = vec4(mix(charcoal * shading, vertexColor.rgb, edge), source.a * vertexColor.a) * ColorModulator;
}
