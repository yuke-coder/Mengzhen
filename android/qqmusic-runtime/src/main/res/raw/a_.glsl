precision highp float;
varying highp vec2 textureCoordinate;
uniform highp int isForSurfaceView;
uniform highp vec4 bgColor;
uniform highp vec3 firstColor;
uniform highp vec3 secondColor;
uniform highp float cropMinX;
uniform highp float cropMaxX;
uniform highp float cropMinY;
uniform highp float cropMaxY;
uniform highp float aspectRatio;
uniform highp float duration;
uniform highp float time;

highp vec2 ColorEffectShader_AlphaFlame_EllipseCoordinate_1(highp float time)
{
    return vec2(1.1 - 2.565 * time, -1.1);
}

highp vec2 ColorEffectShader_AlphaFlame_EllipseCoordinate_2(highp float time)
{
    return vec2(-0.11 + 0.65 * time, -1.1);
}

highp vec2 ColorEffectShader_AlphaFlame_EllipseCoordinate_3(highp float time)
{
    return vec2(0.35 - 1.04 * time, -1.1);
}

highp vec2 ColorEffectShader_AlphaFlame_EllipseCoordinate_4(highp float time)
{
    return vec2(-0.83 + 1.71 * time, -1.05);
}

highp float ColorEffectShader_AlphaFlame_EllipseAlpha_1(highp float time)
{
    highp float alpha = 0.0;
    if (time < 0.85)
    {
        alpha = 0.75;
    }
    else
    {
        alpha = 0.75 - 0.75 * ((time - 0.85) / 0.25);
    }
    return alpha;
}

highp float ColorEffectShader_AlphaFlame_EllipseAlpha_2(highp float time)
{
    return 0.9;
}

highp float ColorEffectShader_AlphaFlame_EllipseAlpha_3(highp float time)
{
    highp float alpha = 0.0;
    if (time < 0.3)
    {
        alpha = 0.4;
    }
    else if (time < 0.6)
    {
        alpha = 0.4 + 0.2 * ((time - 0.3) / 0.3);
    }
    else if (time < 0.85)
    {
        alpha = 0.6 - 0.2 * ((time - 0.6) / 0.25);
    }
    else
    {
        alpha = 0.4;
    }
    return alpha;
}

highp float ColorEffectShader_AlphaFlame_EllipseAlpha_4(highp float time)
{
    return 1.0;
}

highp float ColorEffectShader_AlphaFlame_EllipseDepth_AB(highp vec2 ellipseCoordinate,
highp vec2 fragCoordinate,
highp float aspectRatio,
highp float ellipseA,
highp float ellipseB)
{
    ellipseA = ellipseA * 3.0;
    ellipseB = ellipseB * 2.0;
    highp float ellipseDistance = pow((ellipseCoordinate.x - fragCoordinate.x) * aspectRatio, 2.0) / pow(ellipseA, 2.0) + pow((ellipseCoordinate.y - fragCoordinate.y), 2.0) / pow(ellipseB, 2.0);
    ellipseDistance = clamp(ellipseDistance, 0.0, 1.0);
    highp float ellipseDepth = 0.5 * pow(2.0 * ((ellipseDistance < 0.5) ? ellipseDistance : (1.0 - ellipseDistance)), 1.5);
    ellipseDepth = (ellipseDistance < 0.5) ? (1.0 - ellipseDepth) : ellipseDepth;
    ellipseDepth = clamp(ellipseDepth, 0.0, 1.0);
    ellipseDepth = pow(ellipseDepth, 6.0);
    return ellipseDepth;
}

highp float ColorEffectShader_AlphaFlame_EllipseDepth_1(highp vec2 ellipseCoordinate,
highp vec2 fragCoordinate,
highp float aspectRatio,
highp float time)
{
    highp float ellipseA = 0.0;
    highp float ellipseB = 0.0;
    if (time < 0.3)
    {
        ellipseA = 0.27 + 0.01 * (time / 0.3);
        ellipseB = 0.87 - 0.25 * (time / 0.3);
    }
    else if (time < 0.6)
    {
        ellipseA = 0.28 + 0.31 * ((time - 0.3) / 0.3);
        ellipseB = 0.62 - 0.37 * ((time - 0.3) / 0.3);
    }
    else if (time < 0.85)
    {
        ellipseA = 0.59 - 0.35 * ((time - 0.6) / 0.25);
        ellipseB = 0.25 + 0.33 * ((time - 0.6) / 0.25);
    }
    else
    {
        ellipseA = 0.24;
        ellipseB = 0.58;
    }
    return ColorEffectShader_AlphaFlame_EllipseDepth_AB(ellipseCoordinate, fragCoordinate, aspectRatio, ellipseA, ellipseB);
}

highp float ColorEffectShader_AlphaFlame_EllipseDepth_2(highp vec2 ellipseCoordinate,
highp vec2 fragCoordinate,
highp float aspectRatio,
highp float time)
{
    highp float ellipseA = 0.0;
    highp float ellipseB = 0.0;
    if (time < 0.3)
    {
        ellipseA = 0.5 + 0.13 * (time / 0.3);
        ellipseB = 0.36 + 0.18 * (time / 0.3);
    }
    else if (time < 0.6)
    {
        ellipseA = 0.63 - 0.03 * ((time - 0.3) / 0.3);
        ellipseB = 0.54 - 0.07 * ((time - 0.3) / 0.3);
    }
    else
    {
        ellipseA = 0.6 + 0.15 * ((time - 0.6) / 0.4);
        ellipseB = 0.47 - 0.19 * ((time - 0.6) / 0.4);
    }
    return ColorEffectShader_AlphaFlame_EllipseDepth_AB(ellipseCoordinate, fragCoordinate, aspectRatio, ellipseA, ellipseB);
}

highp float ColorEffectShader_AlphaFlame_EllipseDepth_3(highp vec2 ellipseCoordinate,
highp vec2 fragCoordinate,
highp float aspectRatio,
highp float time)
{
    highp float ellipseA = 0.0;
    highp float ellipseB = 0.0;
    if (time < 0.3)
    {
        ellipseA = 0.57 - 0.3 * (time / 0.3);
        ellipseB = 0.63 - 0.02 * (time / 0.3);
    }
    else if (time < 0.6)
    {
        ellipseA = 0.27 - 0.05 * ((time - 0.3) / 0.3);
        ellipseB = 0.61 + 0.21 * ((time - 0.3) / 0.3);
    }
    else if (time < 0.85)
    {
        ellipseA = 0.22 + 0.14 * ((time - 0.6) / 0.25);
        ellipseB = 0.82 - 0.47 * ((time - 0.6) / 0.25);
    }
    else
    {
        ellipseA = 0.36 - 0.12 * ((time - 0.85) / 0.15);
        ellipseB = 0.35 + 0.04 * ((time - 0.85) / 0.15);
    }
    return ColorEffectShader_AlphaFlame_EllipseDepth_AB(ellipseCoordinate, fragCoordinate, aspectRatio, ellipseA, ellipseB);
}

highp float ColorEffectShader_AlphaFlame_EllipseDepth_4(highp vec2 ellipseCoordinate,
highp vec2 fragCoordinate,
highp float aspectRatio,
highp float time)
{
    highp float ellipseA = 0.0;
    highp float ellipseB = 0.0;
    if (time < 0.3)
    {
        ellipseA = 0.48 + 0.03 * (time / 0.3);
        ellipseB = 0.21 - 0.12 * (time / 0.3);
    }
    else if (time < 0.7)
    {
        ellipseA = 0.51 + 0.03 * ((time - 0.3) / 0.4);
        ellipseB = 0.09 + 0.09 * ((time - 0.3) / 0.4);
    }
    else
    {
        ellipseA = 0.54 + 0.09 * ((time - 0.7) / 0.3);
        ellipseB = 0.18 + 0.13 * ((time - 0.7) / 0.3);
    }
    return ColorEffectShader_AlphaFlame_EllipseDepth_AB(ellipseCoordinate, fragCoordinate, aspectRatio, ellipseA, ellipseB);
}

highp vec2 ColorEffectShader_AlphaFlame_ColorAndAlphaPercent(highp vec2 fragCoordinate,
highp float aspectRatio,
highp float time)
{
    highp vec2 ellipseCoordinate_1 = ColorEffectShader_AlphaFlame_EllipseCoordinate_1(time);
    highp vec2 ellipseCoordinate_2 = ColorEffectShader_AlphaFlame_EllipseCoordinate_2(time);
    highp vec2 ellipseCoordinate_3 = ColorEffectShader_AlphaFlame_EllipseCoordinate_3(time);
    highp vec2 ellipseCoordinate_4 = ColorEffectShader_AlphaFlame_EllipseCoordinate_4(time);
    highp float ellipseAlpha_1 = ColorEffectShader_AlphaFlame_EllipseAlpha_1(time);
    highp float ellipseAlpha_2 = ColorEffectShader_AlphaFlame_EllipseAlpha_2(time);
    highp float ellipseAlpha_3 = ColorEffectShader_AlphaFlame_EllipseAlpha_3(time);
    highp float ellipseAlpha_4 = ColorEffectShader_AlphaFlame_EllipseAlpha_4(time);
    highp float ellipseDepth_1 = ColorEffectShader_AlphaFlame_EllipseDepth_1(ellipseCoordinate_1, fragCoordinate, aspectRatio, time);
    highp float ellipseDepth_2 = ColorEffectShader_AlphaFlame_EllipseDepth_2(ellipseCoordinate_2, fragCoordinate, aspectRatio, time);
    highp float ellipseDepth_3 = ColorEffectShader_AlphaFlame_EllipseDepth_3(ellipseCoordinate_3, fragCoordinate, aspectRatio, time);
    highp float ellipseDepth_4 = ColorEffectShader_AlphaFlame_EllipseDepth_4(ellipseCoordinate_4, fragCoordinate, aspectRatio, time);
    highp float ellipsePercent_1 = ellipseAlpha_1 * ellipseDepth_1;
    highp float ellipsePercent_2 = ellipseAlpha_2 * ellipseDepth_2 * (1.0 - ellipsePercent_1);
    highp float ellipsePercent_3 = ellipseAlpha_3 * ellipseDepth_3 * (1.0 - ellipsePercent_1 - ellipsePercent_2);
    highp float ellipsePercent_4 = ellipseAlpha_4 * ellipseDepth_4 * (1.0 - ellipsePercent_1 - ellipsePercent_2 - ellipsePercent_3);
    highp float ellipseColor_1 = 0.6;
    highp float ellipseColor_2 = 0.1;
    highp float ellipseColor_3 = 0.1;
    highp float ellipseColor_4 = 0.6;
    highp float colorPercent = ellipseColor_1 * ellipsePercent_1 + ellipseColor_2 * ellipsePercent_2 + ellipseColor_3 * ellipsePercent_3 + ellipseColor_4 * ellipsePercent_4;
    highp float alphaPercent = ellipsePercent_1 + ellipsePercent_2 + ellipsePercent_3 + ellipsePercent_4;
    return vec2(colorPercent, alphaPercent);
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp vec2 cropCoordinate = vec2(cropMinX + textureCoordinate.x * (cropMaxX - cropMinX), cropMinY + textureCoordinate.y * (cropMaxY - cropMinY));
    highp vec2 fragCoordinate = (cropCoordinate * 2.0 - vec2(1.0, 1.0)) * vec2(1.0, -1.0);
    highp float time = fract(time / duration);
    time = (time < 0.5) ? (2.0 * time) : (1.0 - 2.0 * (time - 0.5));
    highp vec2 colorAndAlphaPercent = ColorEffectShader_AlphaFlame_ColorAndAlphaPercent(fragCoordinate, aspectRatio, time);
    highp float colorPercent = colorAndAlphaPercent.x;
    highp float alphaPercent = colorAndAlphaPercent.y;
    highp vec3 color = firstColor + (secondColor - firstColor) * colorPercent;
    color.r = clamp(color.r, 0.0001, 0.9999);
    color.g = clamp(color.g, 0.0001, 0.9999);
    color.b = clamp(color.b, 0.0001, 0.9999);
    alphaPercent = clamp(alphaPercent, 0.0001, 0.9999);
    highp vec4 alphaColor = vec4(0.0);
    alphaColor = vec4(color, alphaPercent);

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(alphaColor);
    } else {
        gl_FragColor = alphaColor;
    }
}