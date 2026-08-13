varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp int isForSurfaceView;
uniform highp vec4 bgColor;
uniform highp float spectrumArray[16];
uniform highp vec3 firstColor;
uniform highp vec3 secondColor;
uniform highp float aspectRatio;

const highp float pi = 3.1415926;
const highp float dataSourceCount = 16.0;
const highp float insertCount = 5.0;
const highp float spectrumCount = dataSourceCount + (dataSourceCount - 1.0) * insertCount;
const highp float spectrumGapScale = 1.0;
const highp float spectrumWidth = 1.0 / (spectrumCount * (1.0 + spectrumGapScale) - spectrumGapScale);
const highp float spectrumGap = spectrumWidth * spectrumGapScale;

highp float SpectrumShader2D_QM13_5_dataSourceSpectrumValue(highp int dataSourceIndex)
{
    highp float halfIndex = (dataSourceCount - 1.0) * 0.5;
    highp float percent = 1.0 - abs(halfIndex - float(dataSourceIndex)) / halfIndex;
    highp float dataSourceSpectrumValue = spectrumArray[dataSourceIndex] * percent;
    return dataSourceSpectrumValue;
}

highp float SpectrumShader2D_QM13_5_VisualSpectrumValue(highp int visualIndex)
{
    highp int dataSourceIndex = int(floor(float(visualIndex) / (insertCount + 1.0)));
    highp int insertIndex = int(mod(float(visualIndex), insertCount + 1.0));
    highp float leftSpectrumValue = SpectrumShader2D_QM13_5_dataSourceSpectrumValue(dataSourceIndex);
    highp float rightSpectrumValue = SpectrumShader2D_QM13_5_dataSourceSpectrumValue(dataSourceIndex + 1);
    highp float outputValue_Min = min(leftSpectrumValue, rightSpectrumValue);
    highp float outputValue_Max = max(leftSpectrumValue, rightSpectrumValue);
    highp float insertPercent = (leftSpectrumValue > rightSpectrumValue) ? (1.0 - (float(insertIndex) / (insertCount + 1.0))) : (float(insertIndex) / (insertCount + 1.0));
    highp float visualSpectrumValue = outputValue_Min + (outputValue_Max-outputValue_Min) * (sin(-pi/2.0 + pi * insertPercent) + 1.0) / 2.0;
    return visualSpectrumValue;
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp int visualIndex = int(textureCoordinate.x/(spectrumWidth+spectrumGap));
    highp float visualSpectrumValue = SpectrumShader2D_QM13_5_VisualSpectrumValue(visualIndex);
    highp float visualBgSpectrumValue = SpectrumShader2D_QM13_5_VisualSpectrumValue(int(spectrumCount - 1.0 - float(visualIndex)));
    highp float xStep = step(float(visualIndex) * (spectrumWidth+spectrumGap), textureCoordinate.x) * step(textureCoordinate.x, float(visualIndex) * (spectrumWidth+spectrumGap) + spectrumWidth);
    highp float yStep = step((1.0-textureCoordinate.y), visualSpectrumValue);
    highp float bgYStep = step((1.0-textureCoordinate.y), visualBgSpectrumValue);
    highp vec4 spectrumColor = vec4(firstColor.r, firstColor.g, firstColor.b, 1.0) * xStep * yStep;
    highp vec4 bgSpectrumColor = vec4(firstColor.r * 0.3, firstColor.g * 0.3, firstColor.b * 0.3, 0.3) * xStep * bgYStep;
    highp vec4 color = mix(bgSpectrumColor, spectrumColor, xStep * yStep);
    highp float alpha = (0.5-abs(textureCoordinate.x-0.5)) * 2.0;
    color = color * alpha;

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(color);
    } else {
        gl_FragColor = color;
    }
}