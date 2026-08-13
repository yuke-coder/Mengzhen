#extension GL_OES_EGL_image_external : require

precision mediump float;
uniform samplerExternalOES u_texture;
uniform sampler2D u_bgTexture;
uniform float uHasBgTexture;
varying vec2 v_TexCoordinate;
uniform float uCornerRadius;
uniform vec2 uResolution;
// 视频宽高比 / 卡片宽高比，用于 contain 模式计算
uniform float uVideoAspect;
uniform float uCardAspect;
// SurfaceTexture transform 后的有效纹理坐标范围 (minX, minY, maxX, maxY)
uniform vec4 uTextureBounds;

// 5-tap 十字高斯模糊（性能优先：5次采样替代9次）
vec4 gaussianBlur(sampler2D tex, vec2 uv) {
    vec2 texelSize = vec2(1.0 / uResolution.x, 1.0 / uResolution.y) * 6.0;
    // 权重：中心0.4 + 上下左右各0.15 = 1.0
    vec4 color = texture2D(tex, uv) * 0.4;
    color += texture2D(tex, uv + vec2( texelSize.x, 0.0)) * 0.15;
    color += texture2D(tex, uv + vec2(-texelSize.x, 0.0)) * 0.15;
    color += texture2D(tex, uv + vec2(0.0,  texelSize.y)) * 0.15;
    color += texture2D(tex, uv + vec2(0.0, -texelSize.y)) * 0.15;
    return color;
}

void main() {
    vec2 position = v_TexCoordinate * uResolution;
    vec2 topLeftRadius = vec2(uCornerRadius, uCornerRadius);
    vec2 bottomLeftRadius = vec2(uCornerRadius, uResolution.y - uCornerRadius);
    vec2 topRightRadius = vec2(uResolution.x - uCornerRadius, uCornerRadius);
    vec2 bottomRightRadius = vec2(uResolution.x - uCornerRadius, uResolution.y - uCornerRadius);

    bool inTopLeft = position.x < uCornerRadius && position.y < uCornerRadius && distance(position, topLeftRadius) > uCornerRadius;
    bool inTopRight = position.x > uResolution.x - uCornerRadius && position.y < uCornerRadius && distance(position, topRightRadius) > uCornerRadius;
    bool inBottomLeft = position.x < uCornerRadius && position.y > uResolution.y - uCornerRadius && distance(position, bottomLeftRadius) > uCornerRadius;
    bool inBottomRight = position.x > uResolution.x - uCornerRadius && position.y > uResolution.y - uCornerRadius && distance(position, bottomRightRadius) > uCornerRadius;

    if (inTopLeft || inTopRight || inBottomLeft || inBottomRight) {
        discard;
    }

    // 安全检查：当 uniform 未正确传入时，直接用纹理坐标渲染视频
    if (uVideoAspect <= 0.0 || uCardAspect <= 0.0) {
        gl_FragColor = texture2D(u_texture, v_TexCoordinate);
        return;
    }

    float aspectRatio = uVideoAspect / uCardAspect;
    bool isVideoArea = true;
    vec2 texCoord = v_TexCoordinate;
    if (aspectRatio > 1.0) {
        // 视频比卡片更宽，上下留黑边
        float scaleY = 1.0 / aspectRatio;
        float offsetY = (1.0 - scaleY) * 0.5;
        if (v_TexCoordinate.y < offsetY || v_TexCoordinate.y > 1.0 - offsetY) {
            isVideoArea = false;
        } else {
            texCoord = vec2(v_TexCoordinate.x, (v_TexCoordinate.y - offsetY) / scaleY);
        }
    } else {
        // 视频比卡片更高（或相等），左右留黑边
        float scaleX = aspectRatio;
        float offsetX = (1.0 - scaleX) * 0.5;
        if (v_TexCoordinate.x < offsetX || v_TexCoordinate.x > 1.0 - offsetX) {
            isVideoArea = false;
        } else {
            texCoord = vec2((v_TexCoordinate.x - offsetX) / scaleX, v_TexCoordinate.y);
        }
    }

    if (isVideoArea) {
        // 使用从 SurfaceTexture transform 矩阵中提取的有效纹理范围做 clamp，
        // 避免采样到解码器 stride 对齐的绿色填充像素
        vec2 boundsMin = uTextureBounds.xy;
        vec2 boundsMax = uTextureBounds.zw;
        // 额外内缩 1 像素作为安全边距
        vec2 pixelMargin = vec2(1.0 / uResolution.x, 1.0 / uResolution.y);
        vec2 safeMin = min(boundsMin, boundsMax) + pixelMargin;
        vec2 safeMax = max(boundsMin, boundsMax) - pixelMargin;
        texCoord = clamp(texCoord, safeMin, safeMax);
        gl_FragColor = texture2D(u_texture, texCoord);
    } else {
        // 非视频区域：优先显示高斯模糊背景，没有背景纹理则直接丢弃（不拉伸视频）
        if (uHasBgTexture > 0.5) {
            vec4 blurred = gaussianBlur(u_bgTexture, v_TexCoordinate);
            gl_FragColor = vec4(blurred.rgb * 0.4, 1.0);
        } else {
            discard;
        }
    }

}