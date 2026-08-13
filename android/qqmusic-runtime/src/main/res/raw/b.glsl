precision highp float;
varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform sampler2D maskImageTexture;
uniform sampler2D overlayImageTexture;
uniform highp float overlayAreaX1;
uniform highp float overlayAreaY1;
uniform highp float overlayAreaX2;
uniform highp float overlayAreaY2;
uniform highp float inputImageValid;
varying highp vec2 invertAffineTextureCoordinate;

highp vec2 parseOverlayCoordinate(highp float invertX, highp float invertY) {
    highp float width = overlayAreaX2 - overlayAreaX1;
    highp float height = overlayAreaY2 - overlayAreaY1;
    highp float overlayX = (invertX - overlayAreaX1) / width;
    highp float overlayY = (invertY - overlayAreaY1) / height;
    return vec2(overlayX, overlayY);
}

highp vec4 mixTextureColor(highp vec4 frameColor, highp vec4 maskColor, highp vec4 overlayColor) {
    highp vec4 maskComplement = 1.0 - maskColor;
    highp vec4 mixFrameColor = frameColor * maskComplement;
    highp vec4 mixOverlayColor = overlayColor * maskColor;
    highp vec4 targetColor = mixFrameColor + mixOverlayColor;
    return vec4(targetColor.r, targetColor.g, targetColor.b, frameColor.a);
}

void main() {
    if(inputImageValid == 0.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else if(overlayAreaX1 == 0.0 && overlayAreaX2 == 0.0 && overlayAreaY1 == 0.0 && overlayAreaY2 == 0.0) {
        gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
    } else {
        vec4 frameColor = texture2D(inputImageTexture, textureCoordinate);
        float invertX = invertAffineTextureCoordinate.x;
        float invertY = invertAffineTextureCoordinate.y;
        if(invertX < overlayAreaX1 || invertX > overlayAreaX2 || invertY < overlayAreaY1 || invertY > overlayAreaY2) {
            gl_FragColor = frameColor;
        } else {
            highp vec4 maskColor = texture2D(maskImageTexture, textureCoordinate);
            highp vec2 overlayCoordinate = parseOverlayCoordinate(invertX, invertY);
            highp vec4 overlayColor = texture2D(overlayImageTexture, overlayCoordinate);
            gl_FragColor = mixTextureColor(frameColor, maskColor, overlayColor);
        }
    }

}