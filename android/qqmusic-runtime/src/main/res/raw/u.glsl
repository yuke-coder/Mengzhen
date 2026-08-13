varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp int isForSurfaceView;
uniform highp vec4 bgColor;
uniform highp float spectrumArray[16];
uniform highp vec3 firstColor;
uniform highp vec3 secondColor;
uniform highp float aspectRatio;

const highp float pi = 2.0 * asin(1.0);
const highp float spectrumCount = 16.0;
const highp float circleAngleGap = (2.0 * pi) / spectrumCount;
const highp vec2 circleCoordinate = vec2(0.5, 0.5);

highp float SpectrumShader2D_QM13_3_Angle(highp vec2 fragCoordinate, highp vec2 circleCoordinate, highp float circleDistance, highp float offsetPercent)
{
    highp float asinValue = asin((fragCoordinate.y - circleCoordinate.y) / circleDistance);
    highp float angle = (fragCoordinate.x <= circleCoordinate.x) ? (pi - asinValue) : (fragCoordinate.y >= circleCoordinate.y) ? asinValue : 2.0 * pi + asinValue;
    highp float offsetAngle = 2.0 * pi * offsetPercent;
    highp float outputAngle = (angle + offsetAngle) < (2.0 * pi) ? (angle + offsetAngle) : (angle + offsetAngle - 2.0 * pi);
    return outputAngle;
}

highp float SpectrumShader2D_QM13_3_VisualSpectrumValue(highp int spectrumIndex, highp float spectrumCount, highp float percent)
{
    highp float spectrumValue_Start = spectrumArray[spectrumIndex];
    highp float spectrumValue_End = (spectrumIndex + 1 < int(spectrumCount)) ? spectrumArray[spectrumIndex + 1] : spectrumArray[0];
    highp float spectrumValue_Min = min(spectrumValue_Start, spectrumValue_End);
    highp float spectrumValue_Max = max(spectrumValue_Start, spectrumValue_End);
    highp float spectrumPercent = (spectrumValue_Start > spectrumValue_End) ? (1.0 - percent) : percent;
    highp float spectrumValue = spectrumValue_Min + (spectrumValue_Max-spectrumValue_Min) * (sin(-pi/2.0 + pi * spectrumPercent) + 1.0) / 2.0;
    return spectrumValue;
}

highp float SpectrumShader2D_QM13_3_Circle(highp vec2 fragCoordinate, highp float circleBorderWidth, highp float circleRadiusMin, highp float circleRadiusMax, highp float offsetPercent)
{
    highp float circleDistance = distance(fragCoordinate, circleCoordinate);
    highp float angle = SpectrumShader2D_QM13_3_Angle(fragCoordinate, circleCoordinate, circleDistance, offsetPercent);
    highp int spectrumIndex = int(floor(angle / circleAngleGap));
    highp float percent = (angle - float(spectrumIndex) * circleAngleGap) / circleAngleGap;
    highp float spectrumValue = SpectrumShader2D_QM13_3_VisualSpectrumValue(spectrumIndex, spectrumCount, percent);
    highp float circleRadius = circleRadiusMin + (circleRadiusMax - circleRadiusMin) * spectrumValue;
    highp float circleStep = smoothstep(float(circleRadius-circleBorderWidth*0.5-0.001), float(circleRadius-circleBorderWidth*0.5), float(circleDistance)) * smoothstep(float(circleRadius+circleBorderWidth*0.5+0.001), float(circleRadius+circleBorderWidth*0.5), float(circleDistance));
    return circleStep;
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp vec2 fragCoordinate = vec2(textureCoordinate.x * aspectRatio, abs(textureCoordinate.y-1.0));
    highp vec4 spectrumColor = vec4(0.0, 0.0, 0.0, 0.0);
    highp vec4 circleColor = vec4(firstColor, 1.0);
    highp float circle_1_step = SpectrumShader2D_QM13_3_Circle(fragCoordinate, 0.0025, 0.5 * 0.86, 0.5 * 1.01, 0.0);
    highp float circle_1_alpha = 1.0;
    highp float circle_2_step = SpectrumShader2D_QM13_3_Circle(fragCoordinate, 0.0025, 0.5 * 0.828, 0.5 * 0.99, 0.4);
    highp float circle_2_alpha = 0.3;
    spectrumColor = mix(spectrumColor, circleColor, circle_2_alpha * circle_2_step);
    spectrumColor = mix(spectrumColor, circleColor, circle_1_alpha * circle_1_step);

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(spectrumColor);
    } else {
        gl_FragColor = spectrumColor;
    }
}