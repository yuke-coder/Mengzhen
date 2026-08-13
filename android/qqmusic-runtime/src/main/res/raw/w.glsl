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

highp float ColorEffectShader_QM13_4_Circle(highp vec2 fragCoordinate,
                                            highp vec2 circleCoordinate,
                                            highp float circleRadius,
                                            highp float circleAlpha)
{
    circleRadius = circleRadius * 3.0;
    highp vec2 aspectRatioFragCoord = vec2(fragCoordinate.x, fragCoordinate.y / aspectRatio);
    highp vec2 aspectRatioCircleCoord = vec2(circleCoordinate.x, circleCoordinate.y / aspectRatio);
    highp float circleDistance = distance(aspectRatioFragCoord, aspectRatioCircleCoord) / circleRadius;
    circleDistance = clamp(circleDistance, 0.0, 1.0);
    highp float circleDepth = 0.5 * pow(2.0 * ((circleDistance < 0.5) ? circleDistance : (1.0 - circleDistance)), 1.5);
    circleDepth = (circleDistance < 0.5) ? (1.0 - circleDepth) : circleDepth;
    circleDepth = clamp(circleDepth, 0.0, 1.0);
    return circleDepth * circleAlpha;
}

highp float ColorEffectShader_QM13_4_circleDepth_1(highp vec2 fragCoordinate,
                                                   highp float progressTime)
{
    highp vec2 circleCoordinate = vec2(0.85 - 0.45 * progressTime, 0.0);
    highp float circleRadius = 0.3 + 0.2 * progressTime;
    highp float circleAlpha = 0.4;
    highp float circleDepth = ColorEffectShader_QM13_4_Circle(fragCoordinate, circleCoordinate, circleRadius, circleAlpha);
    return circleDepth;
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp vec2 cropCoordinate = vec2(cropMinX + textureCoordinate.x * (cropMaxX - cropMinX), cropMinY + textureCoordinate.y * (cropMaxY - cropMinY));
    highp vec2 fragCoordinate = vec2(cropCoordinate.x, abs(cropCoordinate.y-1.0));
    highp float progressTime = fract(time / duration);
    progressTime = (progressTime < 0.5) ? (2.0 * progressTime) : (1.0 - 2.0 * (progressTime - 0.5));
    highp float circleDepth_1 = ColorEffectShader_QM13_4_circleDepth_1(fragCoordinate, progressTime);
    highp vec4 circleColor_1 = vec4(firstColor, 1.0);
    highp vec4 color = vec4(0.0);
    color = mix(color, circleColor_1, circleDepth_1);

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(color);
    } else {
        gl_FragColor = color;
    }
}
