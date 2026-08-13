precision highp float;
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform float spectrumArray[16];
uniform float spectrum2Array[16];
uniform float aspectRatio;
uniform int showSpectrum;

const float k_spectrumCount = 16.0;
const float k_spectrumWidth = 1.0 / (k_spectrumCount - 1.0);

float SpectrumShader2D_QMSS_1_SpectrumValue(float inputSpectrumArray[16], float x)
{
    float index = floor(x / k_spectrumWidth);
    float leftSpectrumValue = inputSpectrumArray[int(index)];
    float rightSpectrumValue = 0.0;
    if (index < k_spectrumCount)
    {
        rightSpectrumValue = inputSpectrumArray[(int(index) + 1)];
    }
    float percent = (x - (index * k_spectrumWidth)) / k_spectrumWidth;
    float cosValue = (cos(3.14 * (percent + 1.0)) + 1.0) / 2.0;
    float spectrumValue = leftSpectrumValue + (rightSpectrumValue - leftSpectrumValue) * cosValue;
    spectrumValue = 0.143 + 0.857 * spectrumValue;
    return spectrumValue;
}

vec4 SpectrumShader2D_QMSS_1_SpectrumColor(vec2 p, float inputSpectrumArray[16], vec3 spectrumColor, float spectrumAlpha)
{
    float spectrumY = SpectrumShader2D_QMSS_1_SpectrumValue(inputSpectrumArray, p.x) / aspectRatio;
    vec4 color = (p.y < spectrumY) ? vec4(spectrumColor, spectrumAlpha * pow(p.y, 0.7)) : vec4(0.0);
    return color;
}

void main()
{
    vec2 p = vec2(textureCoordinate.x, (1.0 - textureCoordinate.y) / aspectRatio);
    vec4 color = vec4(0.0);
    vec4 spectrum2Color = SpectrumShader2D_QMSS_1_SpectrumColor(p, spectrum2Array, vec3(0.8, 1.0, 0.82), 0.5);
    color = mix(color, spectrum2Color, spectrum2Color.a);
    if (showSpectrum > 0)
    {
        vec4 spectrumColor = SpectrumShader2D_QMSS_1_SpectrumColor(p, spectrumArray, vec3(0.0, 1.0, 0.63), 0.7);
        color = mix(color, spectrumColor, spectrumColor.a);
    }
    gl_FragColor = color;
}