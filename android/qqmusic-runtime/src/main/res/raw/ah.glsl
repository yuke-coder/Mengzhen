varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp int isForSurfaceView;
uniform highp vec4 bgColor;
uniform highp float spectrumArray[16];
uniform highp vec3 firstColor;
uniform highp vec3 secondColor;
uniform highp float aspectRatio;

const highp float spectrumCount = 16.0;
const highp float spectrumWidth = 1.0/(spectrumCount-1.0);
const highp float borderWidth = 1.0/25.0;

highp float SpectrumShader2D_QM13_6_dataSourceSpectrumValue(highp int dataSourceIndex)
{
    highp float floatIndex = (spectrumCount - 1.0) * 0.5;
    highp float percent = 1.0 - abs(floatIndex - float(dataSourceIndex)) / floatIndex;
    highp float dataSourceSpectrumValue = spectrumArray[dataSourceIndex] * percent;
    return dataSourceSpectrumValue;
}

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp float sepctrumStep = 0.0;
    highp float bgSepctrumStep = 0.0;
    for (highp float index = 0.0; index < spectrumCount; index ++)
    {
        highp float spectrumValue = SpectrumShader2D_QM13_6_dataSourceSpectrumValue(int(index));
        highp float nextSpectrumValue = 0.0;
        if (index < spectrumCount)
        {
            nextSpectrumValue = SpectrumShader2D_QM13_6_dataSourceSpectrumValue(int(index+1.0));
        }
        if (textureCoordinate.x > index * spectrumWidth &&
            textureCoordinate.x < index * spectrumWidth + spectrumWidth)
        {
            highp float percent = (textureCoordinate.x - (index * spectrumWidth)) / spectrumWidth;
            highp float cosValue = (cos(3.14 * (percent + 1.0)) + 1.0) / 2.0;
            highp float middleSpectrumValue = spectrumValue + (nextSpectrumValue - spectrumValue) * cosValue;
            highp float yBottom = middleSpectrumValue;
            highp float yTop = middleSpectrumValue + borderWidth;
            highp float yValue = 1.0 - textureCoordinate.y;
            sepctrumStep = smoothstep(float(yBottom-0.01), float(yBottom+0.01), float(yValue)) * smoothstep(float(yTop+0.01), float(yTop-0.01), float(yValue));
        }
        highp float bgSpectrumValue = SpectrumShader2D_QM13_6_dataSourceSpectrumValue(int(spectrumCount-1.0-index));
        highp float nextBgSpectrumValue = 0.0;
        if (index < spectrumCount)
        {
            nextBgSpectrumValue = SpectrumShader2D_QM13_6_dataSourceSpectrumValue(int(spectrumCount-1.0-index-1.0));
        }
        if (textureCoordinate.x > index * spectrumWidth &&
            textureCoordinate.x < index * spectrumWidth + spectrumWidth)
        {
            highp float bgPercent = (textureCoordinate.x - (index * spectrumWidth)) / spectrumWidth;
            highp float bcCosValue = (cos(3.14 * (bgPercent + 1.0)) + 1.0) / 2.0;
            highp float middleBgSpectrumValue = bgSpectrumValue + (nextBgSpectrumValue - bgSpectrumValue) * bcCosValue;
            highp float yBottom = middleBgSpectrumValue;
            highp float yTop = middleBgSpectrumValue + borderWidth;
            highp float yValue = 1.0 - textureCoordinate.y;
            bgSepctrumStep = smoothstep(float(yBottom-0.01), float(yBottom+0.01), float(yValue)) * smoothstep(float(yTop+0.01), float(yTop-0.01), float(yValue));
        }
    }
    highp vec4 color = vec4(0.0);
    if (sepctrumStep > 0.0)
    {
        color = vec4(firstColor.r * sepctrumStep, firstColor.g * sepctrumStep, firstColor.b * sepctrumStep, sepctrumStep);
    }
    else if (bgSepctrumStep > 0.0)
    {
        color = vec4(firstColor.r * bgSepctrumStep * 0.4, firstColor.g * bgSepctrumStep * 0.4, firstColor.b * bgSepctrumStep * 0.4, bgSepctrumStep * 0.4);
    }
    highp float alpha = (0.5-abs(textureCoordinate.x-0.5)) * 2.0;
    color = color * alpha;

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(color);
    } else {
        gl_FragColor = color;
    }
}