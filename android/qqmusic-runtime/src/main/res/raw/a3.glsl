precision highp float;
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform float spectrumArray[16];
uniform vec3 firstColor;
uniform vec3 secondColor;
uniform float aspectRatio;

const float k_spectrumCount = 7.0;
const float k_spectrumWidth = 1.0 / (k_spectrumCount - 1.0);
const float k_borderWidth = 0.0025;
const float k_fadeWidth = k_borderWidth * 0.2;

float SpectrumShader2D_QM16_1_SpectrumValue(float x)
{
    float index = floor(x / k_spectrumWidth);
    float leftSpectrumValue = spectrumArray[int(index) * 2];
    float rightSpectrumValue = 0.0;
    if (index < k_spectrumCount)
    {
        rightSpectrumValue = spectrumArray[(int(index) + 1) * 2];
    }
    float percent = (x - (index * k_spectrumWidth)) / k_spectrumWidth;
    float cosValue = (cos(3.14 * (percent + 1.0)) + 1.0) / 2.0;
    float spectrumValue = leftSpectrumValue + (rightSpectrumValue - leftSpectrumValue) * cosValue;
    return spectrumValue;
}

void main()
{
    vec2 p = vec2(textureCoordinate.x, (1.0 - textureCoordinate.y) / aspectRatio - k_borderWidth);
    float mind = 1.0;
    for (float x = p.x - k_borderWidth; x < p.x + k_borderWidth; x += k_borderWidth * 0.1)
    {
        float y = SpectrumShader2D_QM16_1_SpectrumValue(x) / aspectRatio;
        float d = length(vec2(x - p.x, y - p.y));
        if (d < k_borderWidth)
        {
            mind = min(mind, d);
        }
    }
    float alpha = smoothstep(k_borderWidth, k_borderWidth - k_fadeWidth, mind);
    vec4 color = vec4(firstColor, 1.0) * alpha;
    gl_FragColor = color;
}