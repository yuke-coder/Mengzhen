// MAX_WORDS / MAX_LINES / BLUR_HALF_SAMPLES 由 Java 注入 #define

precision highp float;
precision highp int;

uniform sampler2D u_Texture;
uniform vec2 u_TexSize;

// ★ 圆形窗口模糊参数（新版：固定核 + 自适应步长 + sigma 控制衰减）
// u_BlurStepPx：采样网格步长（物理像素），小字=1，大字>1
// u_BlurSigmaPx：高斯 sigma（物理像素），= 目标半径 / 2
// u_BlurUseBilinear4：> 0.5 时启用 4-tap 双线性弥补稀疏采样
// u_EnableBlur：> 0.5 时启用高斯模糊效果
uniform float u_BlurStepPx;
uniform float u_BlurSigmaPx;
uniform float u_BlurUseBilinear4;
uniform float u_EnableBlur;

// Y 压缩相关
uniform float u_BallMinScaleY;

// 球心位置和半径（NDC 空间）
uniform float u_BallCenterX[MAX_LINES];
uniform float u_BallCenterY[MAX_LINES];
uniform float u_BallRadiusNdc;

// Quad 几何参数（UV → NDC 转换）
uniform float u_QuadLeft;
uniform float u_QuadTop;
uniform float u_QuadWidth;
uniform float u_QuadHeight;

uniform vec4  u_HighlightColor;
uniform vec4  u_DefaultColor;

// 发光（保留）
uniform vec4  u_GlowColor;
uniform float u_GlowIntensity;
uniform float u_LinePlayedU[MAX_LINES];
uniform float u_GlowWidthUv;

uniform vec4  u_WordRanges[MAX_WORDS];
uniform int   u_WordCount;
uniform float u_WordPlayProgress[MAX_WORDS];
uniform float u_WordAnimatable[MAX_WORDS];
uniform float u_WordLineIndex[MAX_WORDS];

uniform int   u_LineCount;
uniform float u_LineUStart[MAX_LINES];
uniform float u_LineUEnd[MAX_LINES];
uniform float u_LineIsMain[MAX_LINES];

uniform float u_HighlightLineHeight;

// ★ 行的 V 范围（UV 空间），用于区分主歌词行和助唱信息行
uniform float u_LineVMin[MAX_LINES];
uniform float u_LineVMax[MAX_LINES];

varying vec2 v_TexCoord;

// ============================================================
//  4-tap 双线性合成：1 次调用获得 2×2 texel 平均
//  用于 stepPx > 1.5 时弥补稀疏采样的颗粒感
// ============================================================
float sampleAlphaBilinear4(vec2 uv, vec2 texelSize) {
    // 偏移 0.5 texel，让 GL_LINEAR 自动在 2×2 块上插值
    vec2 off = texelSize * 0.5;
    float a0 = texture2D(u_Texture, uv + vec2(-off.x, -off.y)).a;
    float a1 = texture2D(u_Texture, uv + vec2( off.x, -off.y)).a;
    float a2 = texture2D(u_Texture, uv + vec2(-off.x,  off.y)).a;
    float a3 = texture2D(u_Texture, uv + vec2( off.x,  off.y)).a;
    return (a0 + a1 + a2 + a3) * 0.25;
}

// ============================================================
//  ★ 高斯模糊（只算 alpha，对齐 iOS）
//  - 固定核大小：(2*BLUR_HALF_SAMPLES+1)²，圆形裁剪
//  - 步长由 u_BlurStepPx 控制
//  - 模糊衰减由 sigma 控制（sigmaPx 已包含 ballIntensity 调制）
//  - 区域限制：仅采样球所在行的 V 范围内（防止跨行污染）
// ============================================================
float gaussianBlurAlpha(vec2 uv, vec2 texelSize, float stepPx, float sigmaPx,
                        float vMin, float vMax) {
    float invTwoSigma2 = 0.5 / max(sigmaPx * sigmaPx, 0.01);

    // 圆形裁剪：dist² > halfSamples² 的采样点跳过
    const float halfSamplesF = float(BLUR_HALF_SAMPLES);
    float halfSamplesSq = halfSamplesF * halfSamplesF;

    float sum = 0.0;
    float weightSum = 0.0;

    for (int x = -BLUR_HALF_SAMPLES; x <= BLUR_HALF_SAMPLES; x++) {
        for (int y = -BLUR_HALF_SAMPLES; y <= BLUR_HALF_SAMPLES; y++) {
            // 圆形裁剪
            float dist2Idx = float(x * x + y * y);
            if (dist2Idx > halfSamplesSq) continue;

            // 物理像素距离平方（用于高斯权重）
            float distPx2 = dist2Idx * stepPx * stepPx;
            float weight = exp(-distPx2 * invTwoSigma2);

            // 权重太小直接跳过（性能优化 + 避免数值误差）
            if (weight < 0.01) continue;

            // 采样位置
            vec2 offsetPx = vec2(float(x), float(y)) * stepPx;
            vec2 sampleUv = uv + offsetPx * texelSize;
            sampleUv.y = clamp(sampleUv.y, vMin, vMax);

            // 根据 stepPx 选择采样方式
            float a;
            if (u_BlurUseBilinear4 > 0.5) {
                a = sampleAlphaBilinear4(sampleUv, texelSize);
            } else {
                a = texture2D(u_Texture, sampleUv).a;
            }

            sum       += a * weight;
            weightSum += weight;
        }
    }

    return sum / max(weightSum, 0.0001);
}

// ============================================================
//  辅助：计算当前像素的 highlightMask（按 UV 在字内 X 进度插值）
//  使用硬边界（发光带已提供平滑过渡）
// ============================================================
float computeHighlightMaskAt(vec2 uv) {
    for (int i = 0; i < MAX_WORDS; i++) {
        if (i >= u_WordCount) break;
        vec4 range = u_WordRanges[i];

        // ★ 使用严格比较（无容差），防止边界像素被多行共享
        if (uv.x >= range.x - 0.0001 && uv.x <= range.y + 0.0001 &&
            uv.y >= range.z && uv.y <= range.w) {
            if (u_WordAnimatable[i] > 0.5) {
                float progress = u_WordPlayProgress[i];
                
                // 硬边界：直接使用字的进度，无平滑过渡
                // 发光带已提供平滑过渡，不需要额外的软边界
                float playedX = range.x + (range.y - range.x) * progress;
                
                if (uv.x <= playedX) return 1.0;
                else return 0.0;
            }
            return 0.0;
        }
    }
    return 0.0;
}

// ============================================================
//  ★ 辅助：获取当前像素所属的行索引
//    返回 -1 表示不属于任何主歌词行
// ============================================================
int getPixelLineIdx(float v) {
    for (int li = 0; li < MAX_LINES; li++) {
        if (li >= u_LineCount) break;
        if (u_LineIsMain[li] > 0.5) {
            // 使用半开区间 [min, max) 确保像素只属于一行
            // 最后一行的 v 可以等于 max，其他行只能小于 max
            if (li == u_LineCount - 1) {
                // 最后一行：使用闭区间
                if (v >= u_LineVMin[li] && v <= u_LineVMax[li]) {
                    return li;
                }
            } else {
                // 非最后一行：使用半开区间 [min, max)
                // 这样 v == max 时属于下一行，不归属当前行
                if (v >= u_LineVMin[li] && v < u_LineVMax[li]) {
                    return li;
                }
            }
        }
    }
    return -1;
}

// ============================================================
//  主函数
// ============================================================
void main() {
    vec2 texelSize = 1.0 / u_TexSize;

    // ===== 0. 首先确定当前像素属于哪一行 =====
    // ★ 关键修复：先确定像素所属行，然后只应用该行的动画
    //    这样可以彻底防止跨行影响
    int pixelLineIdx = getPixelLineIdx(v_TexCoord.y);
    
    // 当前像素所属行的 V 范围
    float pixelLineVMin = -1.0;
    float pixelLineVMax = -1.0;
    if (pixelLineIdx >= 0) {
        pixelLineVMin = u_LineVMin[pixelLineIdx];
        pixelLineVMax = u_LineVMax[pixelLineIdx];
    }

    // ===== 1. 圆形窗口判定（只检查当前像素所属行的球）=====
    float pixelNdcX = u_QuadLeft + v_TexCoord.x * u_QuadWidth;
    float pixelNdcY = u_QuadTop  - v_TexCoord.y * u_QuadHeight;

    float ballIntensity = 0.0;
    bool  inActiveLine = false;
    
    if (u_BallRadiusNdc > 0.0001 && pixelLineIdx >= 0) {
        // ★ 只检查当前像素所属行的球
        int li = pixelLineIdx;
        
        if (u_LineIsMain[li] > 0.5) {
            float ballCenterXNdc = u_BallCenterX[li];
            if (ballCenterXNdc > -900.0) {
                float dxNdc = pixelNdcX - ballCenterXNdc;
                float dyNdc = pixelNdcY - u_BallCenterY[li];
                float dist2 = dxNdc * dxNdc + dyNdc * dyNdc;
                float radiusNdc = u_BallRadiusNdc;

                if (dist2 < radiusNdc * radiusNdc) {
                    float normDist = sqrt(dist2) / max(radiusNdc, 1e-6);
                    float t = 1.0 - clamp(normDist, 0.0, 1.0);
                    ballIntensity = t * t * (3.0 - 2.0 * t);
                    
                    // 标记当前像素在 active line 内
                    inActiveLine = true;
                }
            }
        }
    }
    
    // ===== 2. Y 压缩（采样坐标）=====
    // ★ 严格控制：只在当前行内生效
    // ★ 关键修复：将采样坐标严格限制在当前像素所属的行内
    vec2 sampleCoord = v_TexCoord;
    
    if (ballIntensity > 0.02 && inActiveLine) {
        float relativeV = pixelLineVMax - sampleCoord.y;
        float minYScale = u_BallMinScaleY;
        float shrinkFactor = relativeV / max(1.0 - (1.0 - minYScale) * ballIntensity, 0.01);
        sampleCoord.y = pixelLineVMax - shrinkFactor;
        
        // ★ 关键修复：将采样坐标严格限制在当前像素所属的行内
        //    这样可以彻底防止采样到其他行的内容
        if (pixelLineIdx >= 0) {
            // 使用半开区间 [min, max) 进行 clamp
            // 确保采样坐标不会指向其他行
            float clampMinV = pixelLineVMin;
            float clampMaxV = pixelLineVMax;
            
            // ★ 关键修复：对于非最后一行，将 max 减去一个 texel 高度
            //    确保采样坐标不会指向下一行
            //    这是防止跨行影响的关键
            if (pixelLineIdx < u_LineCount - 1) {
                // 减去一个 texel 的高度，确保不会指向下一行
                float texelHeight = 1.0 / u_TexSize.y;
                clampMaxV = pixelLineVMax - texelHeight;
                // 确保 clampMaxV 不会小于 clampMinV
                if (clampMaxV < clampMinV) {
                    clampMaxV = clampMinV;
                }
            }
            
            sampleCoord.y = clamp(sampleCoord.y, clampMinV, clampMaxV);
        }
    }

    // ===== 3. 原始字形 alpha 采样 =====
    float glyphAlpha = texture2D(u_Texture, sampleCoord).a;

    // ===== 4. 球区模糊：只算 alpha，颜色用主像素的 textColor =====
    // ★ 使用当前像素所属行的 V 范围进行模糊，防止跨行污染
    if (ballIntensity > 0.02 && inActiveLine && u_EnableBlur > 0.5) {
        // sigma 随 ballIntensity 调制（球边自然退化为锐利）
        float effectiveSigma = u_BlurSigmaPx * ballIntensity;
        
        // 模糊范围限制在行的 V 范围内
        float blurVMin = pixelLineVMin;
        float blurVMax = pixelLineVMax;
        
        float blurredAlpha = gaussianBlurAlpha(sampleCoord, texelSize,
                                               u_BlurStepPx, effectiveSigma,
                                               blurVMin, blurVMax);
        // 在原始 alpha 和模糊 alpha 之间按 ballIntensity 混合
        glyphAlpha = mix(glyphAlpha, blurredAlpha, ballIntensity);
    }

    // ===== 5. 主像素颜色 =====
    // 使用硬边界（发光带已提供平滑过渡）
    float currentMask = computeHighlightMaskAt(v_TexCoord);
    vec4 textColor    = mix(u_DefaultColor, u_HighlightColor, currentMask);

    vec3  finalColor = textColor.rgb;
    // ★ 主歌词行：应用 textColor.a（即 defaultColor/defaultColor 的透明度）
    //    助唱信息行：保持纹理原始 alpha，不受 defaultColor 透明度影响
    float finalAlpha;
    if (pixelLineIdx >= 0) {
        // 主歌词行：应用 textColor.a
        finalAlpha = glyphAlpha * textColor.a;
    } else {
        // 助唱信息行：保持纹理原始 alpha
        finalAlpha = glyphAlpha;
    }

    // ===== 6. 发光段 =====
    // 效果：高亮色 ｜ 发光带 ｜ 未高亮色
    // 发光带颜色渐变：高亮色 → 发光色 → 未高亮色
    // - 高亮色和发光色是不透明的
    // - 未高亮色是带透明度的
    //
    // 实现：直接在发光带区域内进行颜色插值，不叠加
    float glowT = -1.0;  // 发光带内的归一化位置 [0, 1]，< 0 表示不在发光带内

    if (u_GlowWidthUv > 0.0001 && u_GlowIntensity > 0.01 && glyphAlpha > 0.01) {
        for (int li = 0; li < MAX_LINES; li++) {
            if (li >= u_LineCount) break;
            if (u_LineIsMain[li] < 0.5) continue;
            if (u_LinePlayedU[li] < 0.0) continue;

            float lineVMin = -1.0;
            float lineVMax = -1.0;
            for (int i = 0; i < MAX_WORDS; i++) {
                if (i >= u_WordCount) break;
                if (u_WordAnimatable[i] < 0.5) continue;
                int wordLine = int(u_WordLineIndex[i] + 0.5);
                if (wordLine != li) continue;
                vec4 r = u_WordRanges[i];
                lineVMin = r.z;
                lineVMax = r.w;
                break;
            }
            if (lineVMin < 0.0) continue;
            // ★ 使用严格比较（无容差），防止跨行发光
            if (v_TexCoord.y < lineVMin || v_TexCoord.y > lineVMax) continue;

            float playedU = u_LinePlayedU[li];
            float glowW = u_GlowWidthUv;

            // 发光带范围：[playedU - 0.82 * glowW, playedU + 0.18 * glowW]
            float glowStart = playedU - 0.82 * glowW;
            float glowEnd   = playedU + 0.18 * glowW;

            if (v_TexCoord.x < glowStart || v_TexCoord.x > glowEnd) continue;
            if (v_TexCoord.x > u_LineUEnd[li] + 0.001) continue;
            if (v_TexCoord.x < u_LineUStart[li] - 0.001) continue;

            // 计算在发光带内的归一化位置 t ∈ [0, 1]
            // t = 0 → glowStart (高亮色)
            // t = 0.82 → playedU (发光色)
            // t = 1.0 → glowEnd (未高亮色)
            glowT = (v_TexCoord.x - glowStart) / (glowEnd - glowStart);
            glowT = clamp(glowT, 0.0, 1.0);
            
            break;  // 找到匹配行后立即退出
        }
    }

    // ★ 应用发光效果（直接颜色插值，不叠加）
    if (glowT >= 0.0) {
        // 三段颜色插值：
        // [0, 0.82]：高亮色 → 发光色
        // [0.82, 1.0]：发光色 → 未高亮色
        
        vec3 glowRgb;
        float glowAlpha;
        
        if (glowT <= 0.82) {
            // 左段：高亮色 → 发光色
            float t = glowT / 0.82;  // 归一化到 [0, 1]
            t = smoothstep(0.0, 1.0, t);  // 平滑过渡
            
            glowRgb = mix(u_HighlightColor.rgb, u_GlowColor.rgb, t);
            glowAlpha = mix(u_HighlightColor.a, u_GlowColor.a, t);
        } else {
            // 右段：发光色 → 未高亮色
            float t = (glowT - 0.82) / 0.18;  // 归一化到 [0, 1]
            t = smoothstep(0.0, 1.0, t);  // 平滑过渡
            
            glowRgb = mix(u_GlowColor.rgb, u_DefaultColor.rgb, t);
            glowAlpha = mix(u_GlowColor.a, u_DefaultColor.a, t);
        }
        
        // 使用 glyphAlpha 作为遮罩，只在有字形的地方显示发光
        float maskAlpha = glowAlpha * glyphAlpha;
        
        // 直接赋值颜色，不叠加
        finalColor = glowRgb;
        finalAlpha = maskAlpha;
    }

    gl_FragColor = vec4(finalColor, finalAlpha);
}