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

const highp float pi = 2.0 * asin(1.0);
const highp float rotateRadius = 0.05;
const highp float fadeInputRange = 0.3;
const highp float fadeOutputRange = 0.7;
const highp vec2 rotateCenter = vec2(0.5, 0.5);

highp vec2 ColorEffectShader_QM13_2_CircleCoordinate(highp float progressTime)
{
    highp float angle = 2.0 * pi * progressTime;
    highp vec2 circleCoordinate = rotateCenter + rotateRadius * vec2(cos(angle), sin(angle));
    return circleCoordinate;
}

highp float ColorEffectShader_QM13_2_Circle(highp vec2 fragCoordinate,
                                            highp vec2 circleCoordinate,
                                            highp float circleRadius,
                                            highp float circleAlpha)
{
    
    highp float circleDistance = distance(fragCoordinate, circleCoordinate) / circleRadius;
    circleDistance = clamp(circleDistance, 0.0, 1.0);
    highp float circleDepth = (circleDistance < fadeInputRange) ? (1.0 - (1.0 - fadeOutputRange) * (circleDistance / fadeInputRange)) : (fadeOutputRange - fadeOutputRange * ((circleDistance - fadeInputRange) / (1.0 - fadeInputRange)));
    return circleDepth * circleAlpha;
}

highp float ColorEffectShader_QM13_2_circleDepth_1(highp vec2 fragCoordinate, highp float progressTime)
{
    highp vec2 circleCoordinate = ColorEffectShader_QM13_2_CircleCoordinate(progressTime);
    highp float circleRadius = 0.4;
    highp float circleAlpha = 0.9;
    highp float circleDepth = ColorEffectShader_QM13_2_Circle(fragCoordinate, circleCoordinate, circleRadius, circleAlpha);
    return circleDepth;
}

highp float ColorEffectShader_QM13_2_circleDepth_2(highp vec2 fragCoordinate, highp float progressTime)
{
    highp vec2 circleCoordinate = ColorEffectShader_QM13_2_CircleCoordinate(-progressTime);
    highp float circleRadius = 0.4;
    highp float circleAlpha = 0.6;
    highp float circleDepth = ColorEffectShader_QM13_2_Circle(fragCoordinate, circleCoordinate, circleRadius, circleAlpha);
    return circleDepth;
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp vec2 cropCoordinate = vec2(cropMinX + textureCoordinate.x * (cropMaxX - cropMinX), cropMinY + textureCoordinate.y * (cropMaxY - cropMinY));
    highp vec2 fragCoordinate = vec2(cropCoordinate.x * aspectRatio, abs(cropCoordinate.y-1.0));
    highp float progressTime = fract(time / duration);
    highp float circleDepth_1 = ColorEffectShader_QM13_2_circleDepth_1(fragCoordinate, progressTime);
    highp float circleDepth_2 = ColorEffectShader_QM13_2_circleDepth_2(fragCoordinate, progressTime);
    highp vec4 circleColor_1 = vec4(firstColor, 1.0);
    highp vec4 circleColor_2 = vec4(secondColor, 1.0);
    highp vec4 color = vec4(0.0, 0.0, 0.0, 0.0);
    color = mix(color, circleColor_2, circleDepth_2);
    color = mix(color, circleColor_1, circleDepth_1);

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(color);
    } else {
        gl_FragColor = color;
    }
}