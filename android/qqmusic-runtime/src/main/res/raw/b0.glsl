attribute vec4 aPosition;
attribute vec4 aTexCoord;
varying vec2 textureCoordinate;
uniform mat4 uMatrix;
uniform mat4 uSTMatrix;
void main() {
    textureCoordinate = (uSTMatrix * aTexCoord).xy;
    gl_Position = uMatrix * aPosition;
}
