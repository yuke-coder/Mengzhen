precision highp float;
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp int isForSurfaceView;
uniform highp vec4 bgColor;
uniform highp float spectrumArray[16];
uniform highp vec3 firstColor;
uniform highp vec3 secondColor;
uniform highp float aspectRatio;

const highp float pi = 3.1415926;
const highp float dataSourceCount = 12.0;
const highp float insertCount = 6.0;
const highp float spectrumCount = dataSourceCount + (dataSourceCount - 1.0) * insertCount;
const highp float spectrumGapScale = 0.75;
const highp float spectrumWidth = 1.0 / (spectrumCount * (1.0 + spectrumGapScale) - spectrumGapScale);
const highp float spectrumGap = spectrumWidth * spectrumGapScale;

highp float visualSpectrumValue(int visualindex)
{
    highp int index = int(floor(float(visualindex) / (insertCount + 1.0)));
    highp int insertIndex = int(mod(float(visualindex), insertCount + 1.0));
    highp float leftSpectrumValue = spectrumArray[index];
    highp float rightSpectrumValue = spectrumArray[index + 1];
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
    highp vec4 spectrumColor = vec4(0.0);
    highp vec4 tintColor = vec4(vec3(firstColor + (secondColor - firstColor) * textureCoordinate.x), 1.0);
    highp int visualIndex = int(textureCoordinate.x / (spectrumWidth + spectrumGap));
    highp float visualSpectrumValue = visualSpectrumValue(visualIndex);
    highp float xLeft = float(visualIndex) * (spectrumWidth+spectrumGap);
    highp float xRight = float(visualIndex) * (spectrumWidth+spectrumGap) + spectrumWidth;
    highp float xStep = step(float(xLeft), textureCoordinate.x) * step(textureCoordinate.x, float(xRight));
    highp float xLenght = textureCoordinate.x - float(visualIndex) * (spectrumWidth+spectrumGap);
    highp float yTop = - spectrumWidth * aspectRatio + visualSpectrumValue * (1.0 - spectrumWidth  * aspectRatio) + sqrt((spectrumWidth/2.0) * (spectrumWidth/2.0) - (spectrumWidth/2.0 - xLenght) * (spectrumWidth/2.0 - xLenght)) * aspectRatio;
    highp float yValue = abs(textureCoordinate.y-1.0);
    highp float yStep = smoothstep(float(yTop), float(yTop-0.01), float(yValue));
    highp float sepctrumStep = xStep * yStep;
    highp float spectrumAlpha = 0.7 - 0.7 * (yValue / yTop);
    spectrumColor = mix(spectrumColor, tintColor, spectrumAlpha * sepctrumStep);

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(spectrumColor);
    } else {
        gl_FragColor = spectrumColor;
    }
}
