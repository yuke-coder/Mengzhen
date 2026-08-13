precision highp float;
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform vec4 bgColor;
void main()
{
    vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    gl_FragColor = vec4(mix(bgColor.rgb, textureColor.rgb, textureColor.a), bgColor.a);
}