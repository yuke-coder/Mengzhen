#extension GL_OES_EGL_image_external : require

precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES uOESTexture;

void main()
{
    vec4 color = texture2D(uOESTexture, vTextureCoord);
    gl_FragColor = color;
}