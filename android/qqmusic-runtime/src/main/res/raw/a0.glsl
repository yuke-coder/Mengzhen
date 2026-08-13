// MAX_WORDS / MAX_LINES 由 Java 注入 #define

precision highp float;
precision highp int;

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVPMatrix;

uniform vec4  u_WordRanges[MAX_WORDS];
uniform int   u_WordCount;

// 抬升量（NDC 实际值 = 配置值 × 高亮行高度，Java 侧已预乘）
uniform float u_LiftAmount;

// 播放后 Y 方向缩放倍率（相对于原始字高，无量纲）
uniform float u_PlayedScale;

uniform float u_WordLiftProgress[MAX_WORDS];
uniform float u_WordCenterY[MAX_WORDS];
uniform float u_WordLineIndex[MAX_WORDS];
uniform float u_WordAnimatable[MAX_WORDS];

uniform int   u_LineCount;
uniform float u_LineUStart[MAX_LINES];
uniform float u_LineUEnd[MAX_LINES];
uniform float u_LineCenterY[MAX_LINES];
uniform float u_LineIsMain[MAX_LINES];

// 高亮行在 NDC 坐标系中的高度
uniform float u_HighlightLineHeight;

varying vec2 v_TexCoord;

void main() {
    vec4 pos = a_Position;
    float u = a_TexCoord.x;
    float v = a_TexCoord.y;

    float wordCenterY = pos.y;
    bool foundWord = false;
    int matchedLine = 0;
    float liftProgress = 0.0;
    float isAnimatable = 0.0;

    for (int i = 0; i < MAX_WORDS; i++) {
        if (i >= u_WordCount) break;
        vec4 range = u_WordRanges[i];

        if (u >= range.x - 0.0001 && u <= range.y + 0.0001) {
            float vCenter = (range.z + range.w) * 0.5;
            float vHalfHeight = (range.w - range.z) * 0.5;
            float vDist = abs(v - vCenter);

            if (vDist <= vHalfHeight + 0.0002) {
                wordCenterY = u_WordCenterY[i];
                matchedLine = int(u_WordLineIndex[i] + 0.5);
                foundWord = true;
                isAnimatable = u_WordAnimatable[i];
                liftProgress = max(liftProgress, u_WordLiftProgress[i] * isAnimatable);
            }
        }
    }

    float lineIsMain = 0.0;
    if (foundWord) {
        for (int li = 0; li < MAX_LINES; li++) {
            if (li == matchedLine) {
                lineIsMain = u_LineIsMain[li];
            }
        }
    }

    // 抬升：u_LiftAmount 已是 NDC 实际值（= 配置值 × 行高），直接乘以进度
    float liftY = u_LiftAmount * liftProgress;

    // 缩放：从 1.0 过渡到 u_PlayedScale（相对于原始字高的倍率）
    float playedScale = mix(1.0, u_PlayedScale, liftProgress);

    pos.y = wordCenterY + (pos.y - wordCenterY) * playedScale;
    pos.y += liftY;

    v_TexCoord = a_TexCoord;
    gl_Position = u_MVPMatrix * pos;
}