precision highp float;
varying vec2 textureCoordinate;    // 坐标
uniform float aspectRatio;         // 宽高比
uniform float progress;            // 进度（0.0～1.0）
uniform float duration;            // 时长（单位秒）
uniform vec3 firstColor;           // 第一颜色
uniform vec3 secondColor;          // 第二颜色
uniform vec3 thirdColor;           // 第三颜色

float Shader_ThemeBgEffect_2_N11(float p)
{
    return fract(sin(p)*43758.5453);
}

float Shader_ThemeBgEffect_2_N31(vec3 p)
{
    vec3 a = floor(p);
    vec3 b = fract(p);
    b = b * b * (3.0 - 2.0 * b);
    float n = a.x + a.y * 57.0 + 113.0 * a.z;
    float res = mix(mix(mix(Shader_ThemeBgEffect_2_N11(n + 0.0),   Shader_ThemeBgEffect_2_N11(n + 1.0), b.x),
    mix(Shader_ThemeBgEffect_2_N11(n + 57.0),  Shader_ThemeBgEffect_2_N11(n + 58.0), b.x), b.y),
    mix(mix(Shader_ThemeBgEffect_2_N11(n + 113.0), Shader_ThemeBgEffect_2_N11(n + 114.0), b.x),
    mix(Shader_ThemeBgEffect_2_N11(n + 170.0), Shader_ThemeBgEffect_2_N11(n + 171.0), b.x), b.y), b.z);
    return res;
}

float Shader_ThemeBgEffect_2_Layer(float t, vec2 p)
{
    float n = 1.0 - Shader_ThemeBgEffect_2_N31(vec3(vec2(p.x + sin(t * 3.0) * 0.05, p.y + cos(t * 2.0 + 17.0) * 0.05) * vec2(aspectRatio, 1.0) * 2.0, t));
    float increase = 1.0;
    if (n > 0.5)
    {
        n = (0.5 + pow((n - 0.5), increase));
        n = clamp(0.5 + (n - 0.5) / 0.45, 0.0, 1.0);
    }
    else
    {
        n = (0.5 - pow(0.5 - n, increase));
        n = clamp(0.5 - (0.5 - n) / 0.45, 0.0, 1.0);
    }
    return n;
}

vec4 Shader_ThemeBgEffect_2_ThemeBg()
{
    float p = (progress < 0.5) ? (2.0 * progress) : (1.0 - 2.0 * (progress - 0.5));
    float t = p * duration * 0.2;
    vec4 tc = vec4(0.0);
    float n_1 = Shader_ThemeBgEffect_2_Layer(t, (textureCoordinate + vec2(4.0)) * 1.2);
    vec3 nc_1 = firstColor + (secondColor - firstColor) * n_1;
    tc = vec4(nc_1, 1.0);
    float n_2 = Shader_ThemeBgEffect_2_Layer(t, (textureCoordinate + vec2(10.8)) * 1.0);
    n_2 = pow(n_2, 2.0);
    vec4 nc_2 = vec4(thirdColor, 1.0);
    tc = mix(tc, nc_2, n_2);
    return tc;
}

void main()
{
    gl_FragColor = Shader_ThemeBgEffect_2_ThemeBg();
}
