precision highp float;

varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;

uniform highp float aspectRatio;
uniform highp float textureRatio;

void main()
{
    highp vec2 targetCoord = textureCoordinate;
    if (aspectRatio < textureRatio) { // 纹理视频比较长
//        targetCoord.x = targetCoord.x * textureRatio / aspectRatio; 这是以左下角为基准
        targetCoord.x = 1.0 - ((1.0 - targetCoord.x) * textureRatio / aspectRatio); // 以右下角为基准
    } else {
        targetCoord.y = targetCoord.y * aspectRatio / textureRatio;
    }

    if (targetCoord.x < 0.0 || targetCoord.y < 0.0 || targetCoord.x > 1.0 || targetCoord.y > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        gl_FragColor = texture2D(inputImageTexture, targetCoord);
    }
}