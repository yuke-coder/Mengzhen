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

const highp float pi = 3.1415926;

highp vec2 ColorEffectShader_RoundMoveBall_BallCoordinate(highp float progressTime)
{
    return 0.95 * vec2(cos(2.0 * pi * progressTime), sin(2.0 * pi * progressTime));
}

highp float ColorEffectShader_RoundMoveBall_BallDepth(highp vec2 ballCoordinate,
                                                      highp vec2 fragCoordinate)
{
    highp float ballDistance = 0.0;
    if (aspectRatio >= 1.0)
    {
        ballDistance = sqrt(pow((ballCoordinate.x - fragCoordinate.x), 2.0) + pow((ballCoordinate.y - fragCoordinate.y) / aspectRatio, 2.0));
    }
    else
    {
        ballDistance = sqrt(pow((ballCoordinate.x - fragCoordinate.x) * aspectRatio, 2.0) + pow((ballCoordinate.y - fragCoordinate.y), 2.0));
    }
    highp float ballDepth = 1.0 - pow(ballDistance, 2.0);
    return clamp(ballDepth, 0.0, 1.0);
}

highp float ColorEffectShader_RoundMoveBall_ColorPercent(highp vec2 fragCoordinate,
                                                         highp float progressTime)
{
    highp vec2 ballCoordinate_1 = ColorEffectShader_RoundMoveBall_BallCoordinate(progressTime);
    highp vec2 ballCoordinate_2 = ColorEffectShader_RoundMoveBall_BallCoordinate(progressTime + 0.25);
    highp vec2 ballCoordinate_3 = ColorEffectShader_RoundMoveBall_BallCoordinate(progressTime + 0.5);
    highp vec2 ballCoordinate_4 = ColorEffectShader_RoundMoveBall_BallCoordinate(progressTime + 0.75);
    highp float ballDepth_1 = ColorEffectShader_RoundMoveBall_BallDepth(ballCoordinate_1, fragCoordinate);
    highp float ballDepth_2 = ColorEffectShader_RoundMoveBall_BallDepth(ballCoordinate_2, fragCoordinate);
    highp float ballDepth_3 = ColorEffectShader_RoundMoveBall_BallDepth(ballCoordinate_3, fragCoordinate);
    highp float ballDepth_4 = ColorEffectShader_RoundMoveBall_BallDepth(ballCoordinate_4, fragCoordinate);
    highp float ballColor_1 = 0.0;
    highp float ballColor_2 = 1.0;
    highp float ballColor_3 = 0.0;
    highp float ballColor_4 = 1.0;
    highp float ballWeight_1 = 1.0 / (1.0 - ballDepth_1);
    highp float ballWeight_2 = 1.0 / (1.0 - ballDepth_2);
    highp float ballWeight_3 = 1.0 / (1.0 - ballDepth_3);
    highp float ballWeight_4 = 1.0 / (1.0 - ballDepth_4);
    highp float ballPercent_1 = ballWeight_1 / (ballWeight_1 + ballWeight_2 + ballWeight_3 + ballWeight_4);
    highp float ballPercent_2 = ballWeight_2 / (ballWeight_1 + ballWeight_2 + ballWeight_3 + ballWeight_4);
    highp float ballPercent_3 = ballWeight_3 / (ballWeight_1 + ballWeight_2 + ballWeight_3 + ballWeight_4);
    highp float ballPercent_4 = ballWeight_4 / (ballWeight_1 + ballWeight_2 + ballWeight_3 + ballWeight_4);
    highp float colorPercent = ballColor_1 * ballPercent_1 + ballColor_2 * ballPercent_2 + ballColor_3 * ballPercent_3 + ballColor_4 * ballPercent_4;
    return colorPercent;
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp vec2 cropCoordinate = vec2(cropMinX + textureCoordinate.x * (cropMaxX - cropMinX), cropMinY + textureCoordinate.y * (cropMaxY - cropMinY));
    highp vec2 fragCoordinate = (cropCoordinate * 2.0 - vec2(1.0, 1.0)) * vec2(1.0, - 1.0);
    highp float progressTime = fract(time / duration);
    highp float colorPercent = ColorEffectShader_RoundMoveBall_ColorPercent(fragCoordinate, progressTime);
    highp vec3 color = firstColor + (secondColor - firstColor) * colorPercent;
    highp vec4 alphaColor = vec4(color, 1.0);

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(alphaColor);
    } else {
        gl_FragColor = alphaColor;
    }
}