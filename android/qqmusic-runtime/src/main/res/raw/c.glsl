precision highp float;
attribute vec4 position;
attribute vec2 inputTextureCoordinate;
varying vec2 textureCoordinate;
varying vec2 invertAffineTextureCoordinate;


uniform highp float affineA;
uniform highp float affineB;
uniform highp float affineM;
uniform highp float affineC;
uniform highp float affineD;
uniform highp float affineN;
uniform highp float frameSize;

highp vec2 invertAffine(highp float coordinateX, highp float coordinateY) {
    if(affineA == 0.0 && affineB == 0.0 && affineM == 0.0 && affineC == 0.0 && affineD == 0.0 && affineN == 0.0) {
        return vec2(coordinateX, coordinateY);
    }
    highp float bd = affineB / affineD;
    highp float x = coordinateX * frameSize;
    highp float y = coordinateY * frameSize;
    highp float invertX = (x - bd * y + bd * affineN - affineM) / (affineA - bd * affineC);
    highp float invertY = (y - affineN - invertX * affineC) / affineD;
    return vec2(invertX, invertY);
}

void main() {
    gl_Position = position;
    textureCoordinate = vec2(inputTextureCoordinate.x,inputTextureCoordinate.y);
    invertAffineTextureCoordinate = invertAffine(textureCoordinate.x, textureCoordinate.y);
}