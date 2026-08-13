varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp int isForSurfaceView;
uniform highp vec4 bgColor;
uniform highp float spectrumArray[16];
uniform highp vec3 firstColor;
uniform highp vec3 secondColor;
uniform highp float aspectRatio;

const highp float pi = 2.0 * asin(1.0);
const highp float trackCenterY = 0.5;
const highp float trackOffset = 0.1;
const highp float dataSourceCount = 10.0;
const highp float insertCount = 4.0;
const highp float spectrumCount = dataSourceCount + (dataSourceCount - 1.0) * insertCount;
const highp float spectrumGapScale = 2.0;
const highp float spectrumWidth = 1.0 / (spectrumCount * (1.0 + spectrumGapScale) - spectrumGapScale);
const highp float spectrumGap = spectrumWidth * spectrumGapScale;

highp float SpectrumShader2D_QM13_4_VisualSpectrumValue(highp int index)
{
    highp float centerPercent = (0.5-abs(float(index)/spectrumCount-0.5)) * 2.0;
    highp float outputValueMin = 0.2 + 0.3 * centerPercent;
    highp float outputValueMax = 0.5 + 0.8 * centerPercent;
    highp int dataSourceIndex = int(floor(float(index) / (insertCount + 1.0)));
    highp int insertIndex = int(mod(float(index), insertCount + 1.0));
    highp float leftSpectrumValue = spectrumArray[dataSourceIndex];
    highp float rightSpectrumValue = spectrumArray[dataSourceIndex + 1];
    highp float minSpectrumValue = min(leftSpectrumValue, rightSpectrumValue);
    highp float maxSpectrumValue = max(leftSpectrumValue, rightSpectrumValue);
    highp float insertPercent = (leftSpectrumValue > rightSpectrumValue) ? (1.0 - (float(insertIndex) / (insertCount + 1.0))) : (float(insertIndex) / (insertCount + 1.0));
    highp float insertSpectrumValue = minSpectrumValue + (maxSpectrumValue-minSpectrumValue) * (sin(-pi/2.0 + pi * insertPercent) + 1.0) / 2.0;
    highp float visualSpectrumValue = outputValueMin + (outputValueMax - outputValueMin) * insertSpectrumValue;
    return visualSpectrumValue;
}

highp float SpectrumShader2D_QM13_4_SphereCenterY(highp int index)
{
    highp float indexPercent = float(index) / spectrumCount;
    highp float sphereCenterY = trackCenterY + trackOffset * sin(- 0.5 * pi + indexPercent * 2.5 * pi);
    return sphereCenterY;
}

highp vec4 SpectrumShader2D_QM13_4_SphereColor(highp vec2 fragCoordinate)
{
    highp int index = int(fragCoordinate.x / (spectrumWidth + spectrumGap));
    highp float visualSpectrumValue = SpectrumShader2D_QM13_4_VisualSpectrumValue(index);
    highp float center_x = float(index) * (spectrumWidth + spectrumGap) + spectrumWidth / 2.0;
    highp float center_y = SpectrumShader2D_QM13_4_SphereCenterY(index);
    highp float s_radius_x = spectrumWidth / 2.0;
    highp float s_radius_y = visualSpectrumValue / 2.0;
    highp float s_distance = pow(float(fragCoordinate.x - center_x), 2.0) / pow(float(s_radius_x), 2.0) + pow(float(fragCoordinate.y - center_y), 2.0) / pow(float(s_radius_y), 2.0);
    highp float s_step = smoothstep(1.0, 0.4, float(s_distance));
    highp float s_alpha = pow(clamp((1.0 - abs(fragCoordinate.y - center_y) / s_radius_y), 0.0, 1.0), 0.1);
    highp float s_colorPercent = 1.0 - (clamp(((fragCoordinate.y - center_y) / s_radius_y), -1.0, 1.0) + 1.0) * 0.5;
    highp vec4 s_tintColor = vec4(firstColor, 1.0) + (vec4(secondColor, 1.0) - vec4(firstColor, 1.0)) * s_colorPercent;
    highp vec4 sphereColor = s_tintColor * s_step * s_alpha;
    return sphereColor;
}

highp vec4 SpectrumShader2D_QM13_4_BlurColor(highp vec2 fragCoordinate)
{
    highp int index = int(fragCoordinate.x / (spectrumWidth + spectrumGap));
    highp float visualSpectrumValue = SpectrumShader2D_QM13_4_VisualSpectrumValue(index);
    highp float center_x = float(index) * (spectrumWidth + spectrumGap) + spectrumWidth / 2.0;
    highp float center_y = SpectrumShader2D_QM13_4_SphereCenterY(index);
    highp float b_radius_x = spectrumWidth / 2.0 + 0.2;
    highp float b_radius_y = visualSpectrumValue / 2.0 + 0.4;
    highp float b_distance = pow(float(fragCoordinate.x - center_x), 2.0) / pow(float(b_radius_x), 2.0) + pow(float(fragCoordinate.y - center_y), 2.0) / pow(float(b_radius_y), 2.0);
    highp float b_percent = 1.0 - clamp(b_distance, 0.0, 1.0);
    highp float b_step = pow(b_percent, 5.0);
    highp float b_alpha = pow(clamp((1.0 - abs(fragCoordinate.y - center_y) / b_radius_y), 0.0, 1.0), 0.2) * 0.8;
    highp float b_colorPercent = 1.0 - (clamp(((fragCoordinate.y - center_y) / b_radius_y), -1.0, 1.0) + 1.0) * 0.5;
    highp vec4 b_tintColor = vec4(firstColor, 1.0) + (vec4(secondColor, 1.0) - vec4(firstColor, 1.0)) * b_colorPercent;
    highp vec4 blurColor = b_tintColor * b_step * b_alpha;
    return blurColor;
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp vec2 fragCoordinate = vec2(textureCoordinate.x * aspectRatio, abs(textureCoordinate.y-1.0));
    highp vec4 spectrumColor = vec4(0.0);
    highp vec4 blurColor = SpectrumShader2D_QM13_4_BlurColor(fragCoordinate);
    highp vec4 sphereColor = SpectrumShader2D_QM13_4_SphereColor(fragCoordinate);
    spectrumColor = mix(spectrumColor, blurColor, blurColor.a);
    spectrumColor = mix(spectrumColor, sphereColor, sphereColor.a);
    spectrumColor = spectrumColor * 0.5;

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(spectrumColor);
    } else {
        gl_FragColor = spectrumColor;
    }
}
