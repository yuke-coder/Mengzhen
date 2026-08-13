attribute vec2 v_Position;
attribute vec4 a_TexCoordinate;
uniform mat4 u_Matrix;
uniform mat4 u_textureTransform;
varying vec2 v_TexCoordinate;

void main() {
    gl_Position = u_Matrix * vec4(v_Position.xy, 0.0, 1.0);
    gl_Position.z = 1.0;
    v_TexCoordinate = (u_textureTransform * a_TexCoordinate).xy;
}