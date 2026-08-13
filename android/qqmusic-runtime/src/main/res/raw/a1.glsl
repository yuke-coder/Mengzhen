precision highp float;
varying vec2 textureCoordinate;        // 画板坐标
uniform sampler2D inputImageTexture;   // 画板
uniform sampler2D inputImageTexture2;  // 烟花纹理1
uniform sampler2D inputImageTexture3;  // 烟花纹理2
uniform sampler2D inputImageTexture4;  // 烟花纹理3
uniform float aspectRatio;             // 屏幕宽高比
uniform float time;                    // 时间
uniform vec3 firstColor;               // 第一颜色
uniform vec3 secondColor;              // 第二颜色
uniform vec3 thirdColor;               // 第三颜色
uniform float fireworkArray[12];       // 烟花数组 [烟花1index, 烟花1开始时间，烟花1图片index, 烟花2index, 烟花2开始时间，烟花2图片index ...]
uniform int fireworkCount;             // 烟花数量

const float k_pi = 3.14159265;
const float k_pi_2 = 3.14159265 * 2.0;
const float k_pi_half = 3.14159265 / 2.0;
const float k_duration = 5.0;

struct FireworkEffectParam
{
    vec2 fragCoordinate;
    float aspectRatio;
    float time;
    float index;
    float start;
    float textureIndex;
    bool haveImage;
    vec3 color;
};

float Shader_FireworkEffect_1_Random(vec2 p)
{
    return fract(sin(dot(p.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

float Shader_FireworkEffect_1_Noise(vec2 p)
{
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = Shader_FireworkEffect_1_Random(i);
    float b = Shader_FireworkEffect_1_Random(i + vec2(1.0, 0.0));
    float c = Shader_FireworkEffect_1_Random(i + vec2(0.0, 1.0));
    float d = Shader_FireworkEffect_1_Random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float Shader_FireworkEffect_1_DotDepth(vec2 p, float r)
{
    float d = length(p) - r;
    if ((d < 0.0))
    {
        return 1.0 - (d + r) / r;
    }
    return 0.0;
}

vec4 Shader_FireworkEffect_1_Dot(vec3 tintColor, vec2 p, float r)
{
    float dd = Shader_FireworkEffect_1_DotDepth(p, r);
    dd = pow(dd, 3.0);
    vec4 dcolor = vec4(tintColor + (vec3(1.0) - tintColor) * dd, dd);
    return dcolor;
}

float Shader_FireworkEffect_1_SparkDepth(vec2 p, float r1, float r2, float h)
{
    p.x = abs(p.x);
    float b = (r1 - r2) / h;
    float a = sqrt(1.0 - b * b);
    float k = dot(p, vec2(-b, a));
    float d = 0.0;
    if (k < 0.0)
    {
        d = length(p) - r1;
    }
    else if (k > a * h)
    {
        d = length(p - vec2(0.0, h)) - r2;
    }
    else
    {
        d = dot(p, vec2(a, b)) - r1;
    }
    if ((d < 0.0))
    {
        return 1.0 - (d + r1) / r1;
    }
    return 0.0;
}

vec4 Shader_FireworkEffect_1_Spark(vec3 tintColor, vec2 p, float r1, float r2, float h, float sdp)
{
    float sd = Shader_FireworkEffect_1_SparkDepth(p, r1, r2, h);
    float powValue = (sd > sdp) ? (1.0 + 4.0 * (1.0 - ((sd - sdp) / 0.1))) : (1.5 + 3.5 * (sd / sdp));
    sd = pow(sd, powValue);
    vec4 scolor = vec4(tintColor + (vec3(1.0) - tintColor) * sd, sd);
    return scolor;
}

float Shader_FireworkEffect_1_rt(FireworkEffectParam param)
{
    float pt = (param.time - param.start) / k_duration;
    float rt = pt;
    float rts = 0.0;
    float rte = 0.1;
    rt = ((rt > rts) && (pt < rte)) ? ((pt - rts) / (rte - rts)) : 1.0;
    rt = 1.0 - pow(1.0 - rt, 2.0);
    return rt;
}

vec2 Shader_FireworkEffect_1_rc(FireworkEffectParam param, float rt)
{
    float fin = Shader_FireworkEffect_1_Noise(vec2(param.index));
    float rcxmin = param.haveImage ? 0.5 : 0.2;
    float rcxmax = param.haveImage ? 0.8 : 0.7;
    float rcx = rcxmin + (rcxmax - rcxmin) * fin;
    float rcys = 0.0;
    float rcyemin = param.haveImage ? 0.6 : 0.55;
    float rcyemax = param.haveImage ? 0.8 : 0.9;
    float rcye = rcyemin + (rcyemax - rcyemin) * fin;
    float rcy = rcys + (rcye - rcys) * rt;
    rcy = 1.0 - rcy;
    vec2 rc = vec2(rcx, rcy);
    rc = rc * 2.0 - 1.0;
    return rc;
}

float Shader_FireworkEffect_1_rda(float di, float dcount)
{
    float as = 1.0;
    float ae = 0.2;
    float a = as + (ae - as) * (di / dcount);
    return a;
}

float Shader_FireworkEffect_1_rdr(float di, float dcount)
{
    float rs = 0.03;
    float re = 0.01;
    float r = rs + (re - rs) * (di / dcount);
    return r;
}

vec2 Shader_FireworkEffect_1_rdc(vec2 rc, float di, float dcount)
{
    vec2 c = rc + vec2(0.0, pow((di / dcount), 1.5) * 0.8);
    return c;
}

vec2 Shader_FireworkEffect_1_rdm(float dmragne, float di, float dcount)
{
    vec2 dn = vec2(Shader_FireworkEffect_1_Noise(vec2(di + 154.0)), Shader_FireworkEffect_1_Noise(vec2(di + 248.0)));
    vec2 dm = (di / dcount) * dmragne * (dn * 2.0 - 1.0);
    return dm;
}

vec4 Shader_FireworkEffect_1_Rise(FireworkEffectParam param)
{
    vec4 color = vec4(0.0);
    float rt = Shader_FireworkEffect_1_rt(param);
    if (rt < 1.0)
    {
        vec2 p = (param.fragCoordinate * 2.0 - 1.0) * vec2(param.aspectRatio, 1.0);
        float pxn = (Shader_FireworkEffect_1_Noise(vec2(param.index * 7.0 + p * 8.0)) * 2.0 - 1.0) * 0.03;
        vec2 rc = Shader_FireworkEffect_1_rc(param, rt);
        float dcount = 80.0;
        for (float di = 0.0; di < dcount; di += 1.0)
        {
            vec2 dp = p;
            dp.x += pxn * (((di / dcount) < 0.3) ? 0.0 : pow(((di / dcount) - 0.3) / 0.7, 0.7));
            vec2 dc = Shader_FireworkEffect_1_rdc(rc, di, dcount);
            float dr = Shader_FireworkEffect_1_rdr(di, dcount);
            float dmragne = 0.01;
            if (length(dp - dc) < (dr + dmragne))
            {
                dc += Shader_FireworkEffect_1_rdm(dmragne, di, dcount);
                dp = dp - dc;
                float da = Shader_FireworkEffect_1_rda(di, dcount);
                vec4 dcolor = Shader_FireworkEffect_1_Dot(param.color, dp, dr) * da;
                color = (dcolor.a >= color.a) ? dcolor : color;
            }
        }
    }
    return color;
}

float Shader_FireworkEffect_1_lt(FireworkEffectParam param)
{
    float pt = (param.time - param.start) / k_duration;
    float lt = pt;
    float lts = 0.1;
    float lte = 1.0;
    lt = ((lt > lts) && (lt < lte)) ? ((lt - lts) / (lte - lts)) : 1.0;
    lt = 1.0 - pow(1.0 - lt, 3.0);
    return lt;
}

vec2 Shader_FireworkEffect_1_lc(FireworkEffectParam param)
{
    vec2 lc = Shader_FireworkEffect_1_rc(param, 1.0);
    return lc;
}

float Shader_FireworkEffect_1_la(float lt)
{
    float la = 0.0;
    float las = 0.0;
    float lam = 1.0;
    float lae = 0.0;
    float t1 = 0.3;
    if (lt < t1)
    {
        la = las + (lam - las) * pow((lt / t1), 0.5);
    }
    else
    {
        la = lam + (lae - lam) * pow(((lt - t1) / (1.0 - t1)), 2.0);
    }
    return la;
}

vec4 Shader_FireworkEffect_1_Light(FireworkEffectParam param)
{
    vec4 color = vec4(0.0);
    float lt = Shader_FireworkEffect_1_lt(param);
    if (lt < 1.0)
    {
        vec2 p = (param.fragCoordinate * 2.0 - 1.0) * vec2(param.aspectRatio, 1.0);
        vec2 lc = Shader_FireworkEffect_1_lc(param);
        vec2 lp = p - lc;
        if (length(lp) < 1.0)
        {
            float ld = (1.0 - length(lp));
            float la = Shader_FireworkEffect_1_la(lt);
            vec4 lcolor = vec4(param.color, 0.3) * ld * la;
            color = lcolor;
        }
    }
    return color;
}

mat2 Shader_FireworkEffect_1_RotationMatrix(float angle)
{
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    return rotationMatrix;
}

float Shader_FireworkEffect_1_bt(FireworkEffectParam param)
{
    float pt = (param.time - param.start) / k_duration;
    float bt = pt;
    float bts = 0.1;
    float bte = 0.8;
    bt = ((bt > bts) && (bt < bte)) ? ((bt - bts) / (bte - bts)) : 1.0;
    bt = 1.0 - pow(1.0 - bt, 3.0);
    return bt;
}

vec2 Shader_FireworkEffect_1_bc(FireworkEffectParam param)
{
    vec2 bc = Shader_FireworkEffect_1_rc(param, 1.0);
    return bc;
}

float Shader_FireworkEffect_1_br(FireworkEffectParam param)
{
    float fin = Shader_FireworkEffect_1_Noise(vec2(param.index));
    float brmin = param.haveImage ? 0.4 : 0.2;
    float brmax = param.haveImage ? 0.5 : 0.4;
    float br = brmin + (brmax - brmin) * fin;
    return br;
}

float Shader_FireworkEffect_1_bsb(float st)
{
    float sb = 1.0;
    float sbs = 1.0;
    float sbm1 = 5.0;
    float sbm2 = 1.0;
    float sbe = 0.1;
    float t1 = 0.1;
    float t2 = 0.2;
    float t3 = 0.7;
    if (st < t1)
    {
        sb = sbs + (sbm1 - sbs) * (st / t1);
    }
    else if (st < t2)
    {
        sb = sbm1;
    }
    else if (st < t3)
    {
        sb = sbm1 + (sbm2 - sbm1) * pow(((st - t2) / (t3 - t2)), 0.5);
    }
    else
    {
        sb = sbm2 + (sbe - sbm2) * ((st - t3) / (1.0 - t3));
    }
    return sb;
}

float Shader_FireworkEffect_1_bsh(float st, float shs, float shm, float she)
{
    float sh = 0.0;
    float t1 = 0.1;
    float t2 = 0.7;
    float t3 = 0.9;
    if (st < t1)
    {
        sh = shs + (shm - shs) * (st / t1);
    }
    else if (st < t2)
    {
        sh = shm;
    }
    else if (st < t3)
    {
        sh = shm + (she - shm) * ((st - t2) / (t3 - t2));
    }
    else
    {
        sh = she;
    }
    return sh;
}

float Shader_FireworkEffect_1_bsa(float st)
{
    float sa = (st < 0.95) ? 1.0 : ((1.0 - st) / 0.05);
    return sa;
}

float Shader_FireworkEffect_1_angle(vec2 p)
{
    float angle = atan(p.y, p.x);
    angle = (angle >= 0.0) ? angle : (k_pi_2 + angle);
    angle = k_pi_2 - angle;
    return angle;
}

vec4 Shader_FireworkEffect_1_Boom(FireworkEffectParam param)
{
    vec4 color = vec4(0.0);
    float bt = Shader_FireworkEffect_1_bt(param);
    if (bt < 1.0)
    {
        vec2 p = (param.fragCoordinate * 2.0 - 1.0) * vec2(param.aspectRatio, 1.0);
        vec2 bc = Shader_FireworkEffect_1_bc(param);
        float br = Shader_FireworkEffect_1_br(param);
        float lcount = 5.0;
        for (float li = 0.0; li < lcount; li += 1.0)
        {
            float scount = 100.0 - li * 10.0;
            vec2 sp = p - bc;
            float st = pow(bt, 0.5 + li * 0.01);
            float ss = cos(k_pi_half * 0.65 * pow((li / lcount), 0.5));
            sp.y -= pow(st, 0.5) * pow(min(length(sp * ((sp.y < 0.0) ? 1.0 : 0.7)), 1.0), 2.0);
            sp /= ss;
            float pangle = Shader_FireworkEffect_1_angle(sp);
            float si = floor(scount * (pangle / k_pi_2));
            float sn = Shader_FireworkEffect_1_Noise(vec2(param.index + 17.0 * (li + 1.0) * (si + 1.0))) * 2.0 - 1.0;
            float sangle = (si / scount) * k_pi_2 + (1.0 / (scount * 2.0)) * k_pi_2 + sn * 0.05;
            float sm = 0.9 * br + 0.05 * sn;
            sp += st * sm * vec2(-cos(sangle), sin(sangle));
            sp *= Shader_FireworkEffect_1_RotationMatrix(sangle + k_pi_half + k_pi);
            float sr1 = (0.01 + 0.02 * (li / lcount)) / ss;
            float sr2 = (0.003 + 0.01 * (li / lcount)) / ss;
            float sh = Shader_FireworkEffect_1_bsh(st, sr1, sm * 1.5, sr1 * 1.5);
            float sa = Shader_FireworkEffect_1_bsa(st);
            float sdp = 0.99 - 0.1 * (li / lcount);
            float sb = Shader_FireworkEffect_1_bsb(st);
            vec4 scolor = Shader_FireworkEffect_1_Spark(param.color, sp, sr1 * sb, sr2 * sb, sh * sb, sdp) * sa;
            color = (scolor.a >= color.a) ? scolor : color;
        }
    }
    return color;
}

float Shader_FireworkEffect_1_tt(FireworkEffectParam param)
{
    float pt = (param.time - param.start) / k_duration;
    float tt = pt;
    float tts = 0.15;
    float tte = 1.0;
    tt = ((tt > tts) && (tt < tte)) ? ((tt - tts) / (tte - tts)) : 1.0;
    tt = 1.0 - pow(1.0 - tt, 3.0);
    return tt;
}

vec2 Shader_FireworkEffect_1_tc(FireworkEffectParam param, float tt)
{
    vec2 tcs = Shader_FireworkEffect_1_bc(param);
    vec2 tce = Shader_FireworkEffect_1_bc(param) + vec2(0.0, 0.1);
    vec2 tc = tcs;
    float t1 = 0.2;
    if (tt < t1)
    {
        tc = tcs;
    }
    else
    {
        tc = tcs + (tce - tcs) * pow(((tt - t1) / (1.0 - t1)), 3.0);
    }
    return tc;
}

float Shader_FireworkEffect_1_tr(FireworkEffectParam param, float tt)
{
    float tr = 0.0;
    float t1 = 0.25;
    if (tt < t1)
    {
        tr = Shader_FireworkEffect_1_br(param) * pow((tt / t1), 2.0);
    }
    else
    {
        tr = Shader_FireworkEffect_1_br(param);
    }
    return tr;
}

float Shader_FireworkEffect_1_ta(float tt)
{
    float ta = 1.0;
    float t1 = 0.7;
    if (tt < t1)
    {
        ta = 1.0;
    }
    else
    {
        ta = 1.0 - 1.0 * pow(((tt - t1) / (1.0 - t1)), 2.0);
    }
    return ta;
}

float Shader_FireworkEffect_1_noffset(float tt)
{
    float noffsets = 0.005;
    float noffsetm = 0.03;
    float noffsete = 0.2;
    float noffset = 0.0;
    float t1 = 0.3;
    float t2 = 0.7;
    if (tt < t1)
    {
        noffset = noffsets;
    }
    else if (tt < t2)
    {
        noffset = noffsets + (noffsetm - noffsets) * pow((tt - t1) / (t2 - t1), 2.0);
    }
    else
    {
        noffset = noffsetm + (noffsete - noffsetm) * pow(((tt - t2) / (1.0 - t2)), 2.0);
    }
    return noffset;
}

vec4 Shader_FireworkEffect_1_Texture(FireworkEffectParam param)
{
    vec4 color = vec4(0.0);
    float tt = Shader_FireworkEffect_1_tt(param);
    if (tt < 1.0)
    {
        vec2 p = (param.fragCoordinate * 2.0 - 1.0) * vec2(param.aspectRatio, 1.0);
        vec2 tc = Shader_FireworkEffect_1_tc(param, tt);
        vec2 tp = p - tc;
        float tr = Shader_FireworkEffect_1_tr(param, tt);
        float ta = Shader_FireworkEffect_1_ta(tt);
        float lcount = 3.0;
        for (float li = 0.0; li < lcount; li += 1.0)
        {
            vec3 tintColor = param.color + (vec3(1.0) - param.color) * (li / lcount);
            float dcount = 400.0 - 100.0 * (li / (lcount - 1.0));
            float noffset = Shader_FireworkEffect_1_noffset(tt) + 0.1 * (1.0 - (li / (lcount - 1.0)));
            vec2 tnp = floor(tp * dcount) / dcount;
            vec2 tn = noffset * vec2(Shader_FireworkEffect_1_Noise(tnp * 501.0) * 2.0 - 1.0, - Shader_FireworkEffect_1_Noise(tnp * 517.0));
            vec2 ttexCoord = (tnp + tn - vec2(-tr)) / vec2(tr * 2.0);
            ttexCoord = clamp(ttexCoord, vec2(0.0), vec2(1.0));
            float filterp = (0.2 + 0.4 * (li / (lcount - 1.0))) * ta;
            float filtera = step(Shader_FireworkEffect_1_Noise(tp * dcount), filterp);
            float ttexa = 0.0;
            if (param.textureIndex == 0.0)
            {
                ttexa = texture2D(inputImageTexture2, ttexCoord).a;
            }
            else if (param.textureIndex == 1.0)
            {
                ttexa = texture2D(inputImageTexture3, ttexCoord).a;
            }
            else if (param.textureIndex == 2.0)
            {
                ttexa = texture2D(inputImageTexture4, ttexCoord).a;
            }
            float da = ta * ttexa * filtera;
            vec4 dcolor = vec4(tintColor, 1.0) * da;
            color = (dcolor.a >= color.a) ? dcolor : color;
        }
    }
    return color;
}

void main()
{
    vec4 color = vec4(0.0);
    for (int fi = 0; fi < fireworkCount; fi ++)
    {
        FireworkEffectParam param;
        param.fragCoordinate = textureCoordinate;
        param.aspectRatio = aspectRatio;
        param.time = time;
        param.index = fireworkArray[fi*3+0];
        param.start = fireworkArray[fi*3+1];
        param.textureIndex = fireworkArray[fi*3+2];
        param.haveImage = (param.textureIndex >= 0.0) ? true : false;
        param.color = (mod(param.index, 3.0) == 0.0) ? firstColor : ((mod(param.index, 3.0) == 1.0) ? secondColor : thirdColor);
        if ((param.time > param.start) && (param.time < (param.start + k_duration)))
        {
            vec4 rise = Shader_FireworkEffect_1_Rise(param);
            color = mix(color, rise, rise.a);
            vec4 light = Shader_FireworkEffect_1_Light(param);
            color = mix(color, light, light.a);
            vec4 boom = Shader_FireworkEffect_1_Boom(param);
            color = mix(color, boom, boom.a);
            if (param.haveImage)
            {
                vec4 texture = Shader_FireworkEffect_1_Texture(param);
                color = mix(color, texture, texture.a);
            }
        }
    }
    gl_FragColor = color;
}