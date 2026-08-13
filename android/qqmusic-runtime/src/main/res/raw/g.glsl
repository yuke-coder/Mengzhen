precision highp float;

varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;

uniform highp float scaleRatio;
uniform highp float offsetX;
uniform highp float offsetY;
const highp vec2 centerCoord = vec2(1, 0);  // 以右下为基准

void main()
{
    highp vec2 targetCoord = centerCoord + (textureCoordinate - centerCoord) / scaleRatio;
    targetCoord.x -= (offsetX / scaleRatio);
    targetCoord.y += (offsetY / scaleRatio);
    if (targetCoord.x < 0.0 || targetCoord.y < 0.0 || targetCoord.x > 1.0 || targetCoord.y > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        gl_FragColor = texture2D(inputImageTexture, targetCoord);
    }
}