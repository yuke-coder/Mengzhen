#ifdef GL_ES
  precision highp float;
  precision highp sampler2D;
#endif
varying vec2 vTexCoord;
uniform sampler2D sInputImg;
uniform float cTime;

vec2 uVerticalJump = vec2(0.0);

float nrand(float x, float y)
{
    return fract(sin(dot(vec2(x, y), vec2(12.9898, 78.233))) * 43758.5453);
}

void main()
{
    vec2 uv = vTexCoord;
    float u = vTexCoord.x;
    float v = vTexCoord.y;

    // 顺序Jitter_x，Jitter_y, Drift_x, Drift_y
    vec4 pars[72];
    pars[0] = vec4(0.03, 0.9, 0.025, 0.05);  pars[1] = vec4(0.01, 0.77, 0.035, 0.04);  pars[2] = vec4(0.02, 0.8, 0.05, 0.03);
    pars[3] = vec4(0.06, 0.65, 0.025, 0.08); pars[4] = vec4(0.065, 0.45, 0.04, 0.07);  pars[5] = vec4(0.05, 0.7, 0.035, 0.06);
    pars[6] = vec4(0.03, 0.35, 0.03, 0.05);  pars[7] = vec4(0.04, 0.65, 0.02, 0.02);   pars[8] = vec4(0.0, 0.0, 0.0, 0.0);
    pars[9] = vec4(0.0,0.0, 0.0, 0.0);
    pars[10] = vec4(0.0, 0.0, 0.0, 0.0);      pars[11] = vec4(0.0, 0.0, 0.0, 0.0);     pars[12] = vec4(0.04, 0.9, 0.035, 0.04);
    pars[13] = vec4(0.07, 0.68, 0.055, 0.06); pars[14] = vec4(0.06, 0.7, 0.04, 0.15);  pars[15] = vec4(0.075, 0.85, 0.02, 0.1);
    pars[16] = vec4(0.03, 0.44, 0.025, 0.2);  pars[17] = vec4(0.035, 0.56, 0.07, 0.1); pars[18] = vec4(0.05, 0.78, 0.006, 0.04);
    pars[19] = vec4(0.065, 0.89, 0.04, 0.03);
    pars[20] = vec4(0.0, 0.0, 0.0, 0.0);       pars[21] = vec4(0.0, 0.0, 0.0, 0.0);       pars[22] = vec4(0.0, 0.0, 0.0, 0.0);
    pars[23] = vec4(0.0, 0.0, 0.0, 0.0);       pars[24] = vec4(0.073, 0.94, 0.025, 0.08); pars[25] = vec4(0.05, 0.88, 0.025, 0.05);
    pars[26] = vec4(0.08, 0.65, 0.015, 0.07);  pars[27] = vec4(0.05, 0.32, 0.025, 0.03);  pars[28] = vec4(0.07, 0.63, 0.0075, 0.09);
    pars[29] = vec4(0.035, 0.85, 0.075, 0.07);
    pars[30] = vec4(0.017, 0.0, 0.0175, 0.06); pars[31] = vec4(0.04, 0.015, 0.0, 0.15);  pars[32] = vec4(0.0, 0.0, 0.0, 0.0);
    pars[33] = vec4(0.0, 0.0, 0.0, 0.0);       pars[34] = vec4(0.0, 0.0, 0.0, 0.0);      pars[35] = vec4(0.0, 0.0, 0.0, 0.0);
    pars[36] = vec4(0.038, 0.82, 0.04, 0.03);  pars[37] = vec4(0.02, 0.95, 0.035, 0.07); pars[38] = vec4(0.025, 0.82, 0.025, 0.09);
    pars[39] = vec4(0.08, 0.72, 0.035, 0.08);
    pars[40] = vec4(0.055, 0.35, 0.02, 0.05); pars[41] = vec4(0.025, 0.25, 0.045, 0.02); pars[42] = vec4(0.06, 0.4, 0.03, 0.01);
    pars[43] = vec4(0.02, 0.74, 0.025, 0.04); pars[44] = vec4(0.0, 0.0, 0.0, 0.0);       pars[45] = vec4(0.0, 0.0, 0.0, 0.0);
    pars[46] = vec4(0.0, 0.0, 0.0, 0.0);      pars[47] = vec4(0.0, 0.0, 0.0, 0.0);       pars[48] = vec4(0.022, 0.87, 0.015, 0.05);
    pars[49] = vec4(0.04, 0.6, 0.02, 0.02);
    pars[50] = vec4(0.03, 0.39, 0.035, 0.04); pars[51] = vec4(0.016, 0.24, 0.0275, 0.06); pars[52] = vec4(0.028, 0.49, 0.02, 0.08);
    pars[53] = vec4(0.02, 0.6, 0.015, 0.05);  pars[54] = vec4(0.045, 0.88, 0.01, 0.02);   pars[55] = vec4(0.03, 0.85, 0.008, 0.01);
    pars[56] = vec4(0.0, 0.0, 0.0, 0.0);      pars[57] = vec4(0.0, 0.0, 0.0, 0.0);        pars[58] = vec4(0.0, 0.0, 0.0, 0.0);
    pars[59] = vec4(0.0, 0.0, 0.0, 0.0);
    pars[60] = vec4(0.014, 0.95, 0.02, 0.04);  pars[61] = vec4(0.03, 0.88, 0.026, 0.06);   pars[62] = vec4(0.022, 0.64, 0.046, 0.09);
    pars[63] = vec4(0.034, 0.28, 0.032, 0.08); pars[64] = vec4(0.075, 0.37, 0.016, 0.06);  pars[65] = vec4(0.056, 0.54, 0.015, 0.02);
    pars[66] = vec4(0.012, 0.66, 0.04, 0.01);  pars[67] = vec4(0.034, 0.82, 0.02, 0.03);   pars[68] = vec4(0.0, 0.0, 0.0, 0.0);
    pars[69] = vec4(0.0, 0.0, 0.0, 0.0);       pars[70] = vec4(0.0, 0.0, 0.0, 0.0);        pars[71] = vec4(0.0, 0.0, 0.0, 0.0);

    float intensity = 1.0;
    float uHorizontalShake = 0.01;
    float horzIntensity = 0.05;
    float vertIntensity = 0.0;
    float findex = mod(cTime * (0.5 + 0.33 * 30.0), 72.0);
    int ind = int(findex);
    float jitter = nrand(v, cTime) * 2.0 - 1.0;
    jitter *= step(pars[ind].y, abs(jitter)) * pars[ind].x * intensity;
    float jump = mix(v, fract(v + uVerticalJump.y), uVerticalJump.x);
    float shake = (nrand(cTime, 2.0) - 0.5) * uHorizontalShake;

    // Color drift
    float drift = sin(jump + pars[ind].w) * pars[ind].z * horzIntensity;

    vec4 src1 = texture2D(sInputImg, fract(vec2(u + jitter + shake, jump)));
    vec4 src2 = texture2D(sInputImg, (vec2(u + jitter + shake + drift, jump + pars[ind].w * vertIntensity)));
    vec4 resultColor = vec4(src1.r, src2.g, src1.b, clamp(src1.a + src2.a, 0.0, 1.0));
    gl_FragColor = resultColor;
}
