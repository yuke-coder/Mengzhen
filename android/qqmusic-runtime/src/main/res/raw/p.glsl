precision highp float;
varying vec2 textureCoordinate;    // 坐标
uniform float aspectRatio;         // 宽高比
uniform float progress;            // 进度（0.0～1.0）
uniform float duration;            // 时长（单位秒）
uniform vec3 firstColor;           // 第一颜色
uniform vec3 secondColor;          // 第二颜色
uniform vec3 thirdColor;           // 第三颜色

float Shader_ThemeBgEffect_1_N11(float p)
{
    return fract(sin(p)*43758.5453);
}

float Shader_ThemeBgEffect_1_N31(vec3 p)
{
    vec3 a = floor(p);
    vec3 b = fract(p);
    b = b * b * (3.0 - 2.0 * b);
    float n = a.x + a.y * 57.0 + 113.0 * a.z;
    float res = mix(mix(mix(Shader_ThemeBgEffect_1_N11(n + 0.0),   Shader_ThemeBgEffect_1_N11(n + 1.0), b.x),
    mix(Shader_ThemeBgEffect_1_N11(n + 57.0),  Shader_ThemeBgEffect_1_N11(n + 58.0), b.x), b.y),
    mix(mix(Shader_ThemeBgEffect_1_N11(n + 113.0), Shader_ThemeBgEffect_1_N11(n + 114.0), b.x),
    mix(Shader_ThemeBgEffect_1_N11(n + 170.0), Shader_ThemeBgEffect_1_N11(n + 171.0), b.x), b.y), b.z);
    return res;
}

float Shader_ThemeBgEffect_1_Noise(float t, vec2 p)
{
    float n = 1.0 - Shader_ThemeBgEffect_1_N31(vec3(vec2(p.x + sin(t * 3.0) * 0.05, p.y + cos(t * 2.0 + 17.0) * 0.05) * vec2(aspectRatio, 1.0) * 2.0, t));
    float increase = 0.8;
    n = (n > 0.5) ? (0.5 + pow((n - 0.5), increase)) : (0.5 - pow(0.5 - n, increase));
    return n;
}

vec4 Shader_ThemeBgEffect_1_ThemeBg()
{
    float p = (progress < 0.5) ? (2.0 * progress) : (1.0 - 2.0 * (progress - 0.5));
    float t = p * duration * 0.2;
    float n = Shader_ThemeBgEffect_1_Noise(t, (textureCoordinate + vec2(7.0)) * 1.2);
    vec3 nc = firstColor + (secondColor - firstColor) * n;
    vec4 tc = vec4(nc, 1.0);
    return tc;
}

void main()
{
    gl_FragColor = Shader_ThemeBgEffect_1_ThemeBg();
}
