precision highp float;
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp int isForSurfaceView;
uniform highp vec4 bgColor;
uniform highp float spectrumArray[16];
uniform highp vec3 firstColor;
uniform highp vec3 secondColor;
uniform highp float aspectRatio;

const highp int spectrumCount = 7;

highp vec4 addBgColor(highp vec4 color) {
    return vec4(color.rgb + bgColor.rgb * (1.0 - color.a), 1.0);
}

void main()
{
    highp float yValue = abs(textureCoordinate.y-1.0);
    highp vec4 spectrumColor =  vec4(0.0);
    highp float tintPercent = (yValue > 0.6) ? 1.0 : (yValue / 0.6);
    highp vec4 tintColor = vec4(vec3(firstColor + (secondColor - firstColor) * tintPercent), 1.0);
    
    highp vec4 layer1Color = vec4(0.0);
    highp float layer1Alpha = 0.4;
    for (int layer1Index = 0; layer1Index < spectrumCount; layer1Index ++)
    {
        highp float spectrumValue = spectrumArray[layer1Index];
        highp float layer1Index_x = (float(layer1Index) + 1.0) / (float(spectrumCount) + 1.0);
        highp float layer1Index_percent = (textureCoordinate.x < layer1Index_x) ? (textureCoordinate.x / layer1Index_x) : (1.0 - (textureCoordinate.x - layer1Index_x) / (1.0 - layer1Index_x));
        highp float layer1Index_yTop = pow(layer1Index_percent, 3.0) * spectrumValue;
        highp float layer1Index_step = smoothstep(layer1Index_yTop, layer1Index_yTop-0.01, yValue);
        layer1Color = mix(layer1Color, tintColor, layer1Alpha * layer1Index_step);
    }
    spectrumColor = mix(spectrumColor, layer1Color, layer1Color.a);
    
    highp vec4 layer2Color = vec4(0.0);
    highp float layer2Gap = 1.0 / (float(spectrumCount) + 1.0);
    highp int layer2Index = int(floor(textureCoordinate.x / layer2Gap));
    highp float leftSpectrumValue = (layer2Index == 0) ? 0.0 : spectrumArray[layer2Index - 1];
    highp float rightSpectrumValue = (layer2Index == spectrumCount) ? 0.0 : spectrumArray[layer2Index];
    highp float midSpectrumValue = min(leftSpectrumValue, rightSpectrumValue) * 0.6;
    highp float layer2_xPercent = (textureCoordinate.x - float(layer2Index) * layer2Gap) / layer2Gap;
    highp float layer2_yTop = 0.0;
    if (layer2Index == 0)
    {
        layer2_yTop = leftSpectrumValue + (rightSpectrumValue - leftSpectrumValue) * pow(layer2_xPercent, 2.0);
    }
    else if (layer2Index == spectrumCount)
    {
        layer2_yTop = rightSpectrumValue + (leftSpectrumValue - rightSpectrumValue) * pow(1.0 - layer2_xPercent, 2.0);
    }
    else
    {
        if (layer2_xPercent < 0.5)
        {
            layer2_yTop = midSpectrumValue + (leftSpectrumValue - midSpectrumValue) * pow(1.0 - layer2_xPercent / 0.5, 2.0);
        }
        else
        {
            layer2_yTop = midSpectrumValue + (rightSpectrumValue - midSpectrumValue) * pow((layer2_xPercent - 0.5) / 0.5, 2.0);
        }
    }
    highp float layer2_step = smoothstep(float(layer2_yTop), float(layer2_yTop-0.01), float(yValue));
    highp float layer2Alpha = 0.7 - 0.3 * (yValue / layer2_yTop);
    layer2Color = mix(layer2Color, tintColor, layer2Alpha * layer2_step);
    spectrumColor = mix(spectrumColor, layer2Color, layer2Color.a);

    if (isForSurfaceView == 1) {
        gl_FragColor = addBgColor(spectrumColor);
    } else {
        gl_FragColor = spectrumColor;
    }
}