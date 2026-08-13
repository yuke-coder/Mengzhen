precision highp float;
varying vec2 textureCoordinate;        // 纹理坐标（通用的）
uniform sampler2D inputImageTexture;   // 纹理（通用的）
uniform float time;                    // 时间
uniform float volume;                  // 音量（0～1）
uniform float entry;                   // 进场动画进度（0～1），到1后才会做volume动画
uniform vec3 bottomColor;              // 底部衔接AS的背景色

float Shader_AIAgentVoiceEffect_1_WaveY()
{
    vec2 p = textureCoordinate * 2.0 - vec2(1.0);
    float realVolume = (entry < 1.0) ? (1.0 - pow(entry, 0.5)) * 0.5 : volume;
    float waveY = sin(p.x * (5.0 + realVolume * 3.0) + time * 12.0);
    waveY *= ((entry < 1.0) ? 0.4 : 0.2) * realVolume;
    waveY *= pow((1.0 - abs(p.x)), 0.5);
    return waveY;
}

vec3 Shader_AIAgentVoiceEffect_1_RainbowColor()
{
    vec3 rainbowColor = vec3(0.0);
    vec3 col1 = vec3(200.0/255.0, 255.0/255.0, 227.0/255.0);
    vec3 col2 = vec3(131.0/255.0, 165.0/255.0, 255.0/255.0);
    vec3 col3 = vec3(55.0/255.0, 255.0/255.0, 130.0/255.0);
    vec3 col4 = vec3(250.0/255.0, 247.0/255.0, 96.0/255.0);
    float x = textureCoordinate.x;
    x = abs(fract(x - time * 0.3));
    float x1 = 0.0;
    float x2 = 0.25;
    float x3 = 0.5;
    float x4 = 0.75;
    float x5 = 1.0;
    if ((x >= x1) && (x <= x2))
    {
        rainbowColor = col1 + (col2 - col1) * (x - x1) / (x2 - x1);
    }
    else if ((x >= x2) && (x <= x3))
    {
        rainbowColor = col2 + (col3 - col2) * (x - x2) / (x3 - x2);
    }
    else if ((x >= x3) && (x <= x4))
    {
        rainbowColor = col3 + (col4 - col3) * (x - x3) / (x4 - x3);
    }
    else if ((x >= x4) && (x <= x5))
    {
        rainbowColor = col4 + (col1 - col4) * (x - x4) / (x5 - x4);
    }
    return rainbowColor;
}

vec4 Shader_AIAgentVoiceEffect_1_VoiceEffect()
{
    vec4 voiceEffectColor = vec4(0.0);
    vec3 rainbowColor = Shader_AIAgentVoiceEffect_1_RainbowColor();
    vec2 p = textureCoordinate * 2.0 - vec2(1.0);
    float waveY = Shader_AIAgentVoiceEffect_1_WaveY();
    if (p.y < waveY)
    {
        float powValue = 1.5 + pow(entry, 2.0) * (6.0 - 4.0 * volume);
        float alpha = pow(1.0 - (waveY - p.y), powValue);
        voiceEffectColor = vec4(rainbowColor, 1.0) * alpha;
    }
    else
    {
        voiceEffectColor = vec4(bottomColor, 1.0);
    }
    float borderWidth = 0.04;
    float fadeWidth = borderWidth * 0.2;
    if ((p.y > waveY - borderWidth) && (p.y < waveY + borderWidth))
    {
        float d = abs(p.y - waveY);
        float alpha = smoothstep(borderWidth, borderWidth - fadeWidth, d);
        if (alpha < 1.0)
        {
            voiceEffectColor = vec4(mix(voiceEffectColor.rgb, rainbowColor, alpha), 1.0);
        }
        else
        {
            voiceEffectColor = vec4(rainbowColor, 1.0);
        }
    }
    return voiceEffectColor;
}

void main()
{
   vec4 color = vec4(0.0);
   vec4 voiceEffect = Shader_AIAgentVoiceEffect_1_VoiceEffect();
   color = mix(color, voiceEffect, voiceEffect.a);
   gl_FragColor = color;
}
