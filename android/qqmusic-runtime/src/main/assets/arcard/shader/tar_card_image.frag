precision mediump float;
uniform sampler2D u_texture;
uniform sampler2D u_overlayTexture;
uniform int u_hasOverlay;
uniform sampler2D u_iconTexture;
uniform int u_hasIcon;
// icon 在纹理坐标系中的尺寸比例（宽/卡片宽, 高/卡片高）
uniform vec2 u_iconScale;
varying vec2 v_TexCoordinate;
uniform float uCornerRadius;
uniform vec2 uResolution;

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

    vec4 baseColor = texture2D(u_texture, v_TexCoordinate);
    if (u_hasOverlay == 1) {
        vec4 overlayColor = texture2D(u_overlayTexture, v_TexCoordinate);
        // 使用叠加图的 alpha 通道进行混合：overlay 在上层
        baseColor = mix(baseColor, overlayColor, overlayColor.a);
    }

    // icon 图放置在左下角，完全贴边，叠加在最上层
    if (u_hasIcon == 1) {
        // 计算 icon 在左下角的纹理坐标范围（无边距，完全贴边）
        float left = 0.0;
        float right = u_iconScale.x;
        float bottom = 1.0;
        float top = 1.0 - u_iconScale.y;
        if (v_TexCoordinate.x >= left && v_TexCoordinate.x <= right &&
            v_TexCoordinate.y >= top && v_TexCoordinate.y <= bottom) {
            // 将当前片元坐标映射到 icon 纹理的 [0,1] 范围
            vec2 iconUV = vec2(
                (v_TexCoordinate.x - left) / (right - left),
                (v_TexCoordinate.y - top) / (bottom - top)
            );
            vec4 iconColor = texture2D(u_iconTexture, iconUV);
            baseColor = mix(baseColor, iconColor, iconColor.a);
        }
    }

    gl_FragColor = baseColor;
}
