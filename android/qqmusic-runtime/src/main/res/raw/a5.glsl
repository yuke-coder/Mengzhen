precision highp float;
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform vec4 bgColor;
void main()
{
    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
}