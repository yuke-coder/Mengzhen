#extension GL_OES_EGL_image_external : require
precision highp float;
varying vec2 textureCoordinate;
uniform samplerExternalOES inputImageTexture;
void main() {
    highp vec4 color = texture2D(inputImageTexture, textureCoordinate);

    highp vec2 maskSrcPos = vec2(textureCoordinate.x * 0.5, textureCoordinate.y);
    highp vec2 maskMaskPos = vec2(0.5 + textureCoordinate.x * 0.5, textureCoordinate.y);

    highp vec4 textureSrcColor = texture2D(inputImageTexture, maskSrcPos);
    highp vec4 textureMaskColor = texture2D(inputImageTexture, maskMaskPos);
    textureSrcColor.a = textureMaskColor.r;

    gl_FragColor = textureSrcColor;
}