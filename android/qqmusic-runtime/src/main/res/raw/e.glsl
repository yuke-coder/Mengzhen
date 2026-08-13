precision highp float;

varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTexture2;

void main()
{
   lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
   lowp vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate);
   gl_FragColor = vec4(mix(textureColor.rgb, textureColor2.rgb, textureColor2.a), textureColor.a);
}