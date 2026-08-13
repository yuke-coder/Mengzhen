varying highp vec2 textureCoordinate;
uniform highp int isForSurfaceView;
uniform highp vec4 bgColor;
uniform highp float aspectRatio;
uniform highp float time;
uniform highp float expand;
uniform highp float volume;
uniform highp float volumeIncrease;

const highp float k_pi = 3.14159265;

highp vec4 VoiceEffectShader_Multiply(highp vec4 c1, highp vec4 c2)
{
    return c1 * c2 + c1 * (1.0 - c2.a) + c2 * (1.0 - c1.a);
}

highp float VoiceEffectShader_Random(highp vec2 p)
{
    return fract(sin(dot(p.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

highp float VoiceEffectShader_Noise(highp vec2 p)
{
    highp vec2 i = floor(p);
    highp vec2 f = fract(p);
    highp float a = VoiceEffectShader_Random(i);
    highp float b = VoiceEffectShader_Random(i + vec2(1.0, 0.0));
    highp float c = VoiceEffectShader_Random(i + vec2(0.0, 1.0));
    highp float d = VoiceEffectShader_Random(i + vec2(1.0, 1.0));
    highp vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

highp vec4 VoiceEffectShader_LC(highp vec2 p, highp float sy, highp float b, highp vec3 c1, highp vec3 c2, highp float a)
{
    highp float cy = sqrt(1.0 - pow(p.x, 2.0)) * sy;
    highp float yt = max(cy, -cy);
    highp float yb = min(cy, -cy);
    highp float yp = 1.0 - (p.y - yb) / (yt - yb);
    highp float bp = smoothstep(yb - b, yb + b, p.y) * smoothstep(yt + b, yt - b, p.y);
    highp vec4 lc = (bp > 0.0) ? bp * vec4(c1 + (c2 - c1) * yp, a) : vec4(0.0);
    return (abs(p.x) <= 1.0) ? lc : vec4(0.0);
}

highp vec4 VoiceEffectShader_LC1(highp vec2 p, highp float e)
{
    highp float n = (VoiceEffectShader_Noise(vec2((p.x + time * 3.0 + volumeIncrease), p.y)) + 1.0) * 0.04 * expand;
    highp float shape = 1.0 / (pow(p.x, 2.0) * 5.0 + 1.0);
    highp float sxe = 0.4 + 1.2 * e - 0.5 * volume * expand;
    highp float sye = (1.0 - 0.5 * e) * (1.0 + (shape - 1.0) * e) * 0.9;
    highp float sx = p.x * sxe * k_pi + (time + volumeIncrease * 0.2);
    highp float sy = sin(sx) * sye + n;
    highp float b = 0.005 + 0.01 * (1.0 - expand);
    highp vec4 lc = VoiceEffectShader_LC(p, sy, b, vec3(0.8, 0.7, 1.0), vec3(0.3, 0.8, 1.0), 0.9);
    return lc;
}

highp vec4 VoiceEffectShader_LC2(highp vec2 p, highp float e)
{
    highp float n = (VoiceEffectShader_Noise(vec2((p.x + time * 3.0 + volumeIncrease) + 100.0, p.y)) + 1.0) * 0.04 * expand;
    highp float shape = 1.0 / (pow(p.x, 2.0) * 5.0 + 1.0);
    highp float sxe = 0.5 + 1.5 * e - 0.5 * volume * expand;
    highp float sye = (1.0 - 0.5 * e) * (1.0 + (shape - 1.0) * e) * 0.8;
    highp float sx = p.x * sxe * k_pi - (time + volumeIncrease * 0.2);
    highp float sy = sin(sx) * sye + n;
    highp float b = 0.005 + 0.01 * (1.0 - expand);
    highp vec4 lc = VoiceEffectShader_LC(p, sy, b, vec3(0.2, 1.0, 0.6), vec3(0.2, 1.0, 0.6), 0.7);
    return lc;
}

highp vec4 VoiceEffectShader_LC3(highp vec2 p, highp float e)
{
    highp float n = (VoiceEffectShader_Noise(vec2((p.x + time * 3.0 + volumeIncrease) + 200.0, p.y)) + 1.0) * 0.002 * expand;
    highp float shape = 1.0 / (pow(p.x, 2.0) * 5.0 + 1.0);
    highp float sxe = 0.8 * e - 0.5 * volume * expand;
    highp float sye = (1.0 - 0.5 * e) * (1.0 + (shape - 1.0) * e) * 0.2;
    highp float sx = p.x * sxe * k_pi - (time + volumeIncrease * 0.2);
    highp float sy = sin(sx) * sye + n;
    highp float b = 0.005;
    highp vec4 lc = VoiceEffectShader_LC(p, sy, b, vec3(1.0), vec3(1.0), 0.3);
    return lc * expand;
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp float e = 0.5 * pow(2.0 * ((expand < 0.5) ? expand : 1.0 - expand), 5.0);
    e = (expand < 0.5) ? e : 1.0 - e;
    highp float stretch = 4.0;
    highp float minw = 1.0 / stretch;
    highp float maxw = 1.0;
    highp float w = minw + (maxw - minw) * e;
    highp float minh = 1.0 / stretch;
    highp float maxh = minh * e * 2.5;
    highp float h = minh + (maxh - minh) * e * volume;
    highp float x = (1.0 - w) / 2.0;
    highp float y = (1.0 - h) / 2.0;
    highp vec2 p = vec2((textureCoordinate.x - x) / w, (textureCoordinate.y - y) / h) * 2.0 - vec2(1.0);
    highp vec4 color = vec4(0.0);
    highp vec4 lc1 = VoiceEffectShader_LC1(p, e);
    highp vec4 lc2 = VoiceEffectShader_LC2(p, e);
    highp vec4 lc3 = VoiceEffectShader_LC3(p, e);
    color = VoiceEffectShader_Multiply(color, lc1);
    color = VoiceEffectShader_Multiply(color, lc2);
    color = mix(color, lc3, lc3.a);

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(color);
    } else {
        gl_FragColor = color;
    }
}