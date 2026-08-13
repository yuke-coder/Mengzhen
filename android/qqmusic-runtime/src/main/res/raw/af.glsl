precision highp float;
varying vec2 textureCoordinate;        // 纹理坐标（通用的）
uniform sampler2D inputImageTexture;   // 纹理（通用的）
uniform float spectrumArray[16];       // 频谱数组
uniform float trackArray[100];         // 音轨数组{x1,y1,z1,x2,y2,z2,...}
uniform int trackCount;                // 音轨数组数量
uniform float aspectRatio;             // 宽高比
uniform float time;                    // 时间
uniform float spectrumValue;           // 频谱数据（取频谱里数组里最大值）
uniform float increaseSpectrumValue;   // 累积频谱值（increaseSpectrumValue += spectrumValue * 0.01，超过100后归0重来）
uniform float sphereCenterY;           // 球体中心点Y（归一化）
uniform float spectrumBottomY;         // 频谱底部Y（归一化）
uniform float spectrumWidth;           // 频谱宽度（归一化）
uniform vec3 tintColor;                // 主题色（传{255/255.0, 170/255.0, 40/255.0}吧）
uniform mat4 rotationMatrix;           // 旋转矩阵

const float pi = 3.1415926;

vec2 Shader_GalaxyEffect_1_GalaxyCoordinate()
{
    float w = 0.9;
    float h = w * aspectRatio;
    float minx = (1.0 - w) / 2.0;
    float maxx = minx + w;
    float miny = sphereCenterY - h / 2.0;
    float maxy = miny + h;
    float x = textureCoordinate.x;
    x = (x < minx) ? minx : x;
    x = (x > maxx) ? maxx : x;
    x = (x - minx) / (maxx - minx);
    float y = textureCoordinate.y;
    y = (y < miny) ? miny : y;
    y = (y > maxy) ? maxy : y;
    y = (y - miny) / (maxy - miny);
    return vec2(x, y) * 2.0 - vec2(1.0);;
}

vec2 Shader_GalaxyEffect_1_SpectrumCoordinate()
{
    float w = spectrumWidth;
    float h = (w * aspectRatio) * (300.0 / 780.0);
    float minx = (1.0 - w) / 2.0;
    float maxx = minx + w;
    float maxy = spectrumBottomY;
    float miny = maxy - h;
    float x = textureCoordinate.x;
    x = (x < minx) ? minx : x;
    x = (x > maxx) ? maxx : x;
    x = (x - minx) / (maxx - minx);
    float y = textureCoordinate.y;
    y = (y < miny) ? miny : y;
    y = (y > maxy) ? maxy : y;
    y = (y - miny) / (maxy - miny);
    return vec2(x, y);
}

vec4 Shader_GalaxyEffect_1_Max(vec4 c1, vec4 c2)
{
    return (c1.a > c2.a) ? c1 : c2;
}

vec4 Shader_GalaxyEffect_1_Add(vec4 c1, vec4 c2)
{
    return c1 + c2 * c2.a;
}

float Shader_GalaxyEffect_1_Gain(float x, float k)
{
    float a = 0.5 * pow(2.0 * ((x < 0.5) ? x : 1.0 - x), k);
    return (x < 0.5) ? a : 1.0 - a;
}

float Shader_GalaxyEffect_1_Random(vec2 p)
{
    return fract(sin(dot(p.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

float Shader_GalaxyEffect_1_N21(vec2 p)
{
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = Shader_GalaxyEffect_1_Random(i);
    float b = Shader_GalaxyEffect_1_Random(i + vec2(1.0, 0.0));
    float c = Shader_GalaxyEffect_1_Random(i + vec2(0.0, 1.0));
    float d = Shader_GalaxyEffect_1_Random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

vec2 Shader_GalaxyEffect_1_N22(vec2 p)
{
    float n = Shader_GalaxyEffect_1_N21(p);
    return vec2(n, Shader_GalaxyEffect_1_N21(p+n));
}

float Shader_GalaxyEffect_1_N31(vec3 p)
{
    vec3 s = vec3(110.0, 241.0, 171.0);
    vec3 i = floor(p);
    vec3 f = fract(p);
    float n = dot(i, s);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(Shader_GalaxyEffect_1_N21(vec2(n + dot(s, vec3(0.0, 0.0, 0.0)))), Shader_GalaxyEffect_1_N21(vec2(n + dot(s, vec3(1.0, 0.0, 0.0)))), u.x),
                   mix(Shader_GalaxyEffect_1_N21(vec2(n + dot(s, vec3(0.0, 1.0, 0.0)))), Shader_GalaxyEffect_1_N21(vec2(n + dot(s, vec3(1.0, 1.0, 0.0)))), u.x), u.y),
               mix(mix(Shader_GalaxyEffect_1_N21(vec2(n + dot(s, vec3(0.0, 0.0, 1.0)))), Shader_GalaxyEffect_1_N21(vec2(n + dot(s, vec3(1.0, 0.0, 1.0)))), u.x),
                   mix(Shader_GalaxyEffect_1_N21(vec2(n + dot(s, vec3(0.0, 1.0, 1.0)))), Shader_GalaxyEffect_1_N21(vec2(n + dot(s, vec3(1.0, 1.0, 1.0)))), u.x), u.y), u.z);
}

float Shader_GalaxyEffect_1_SDEllipsoid(vec3 p, vec3 r)
{
    float k0 = length(p / r);
    float k1 = length(p / (r * r));
    return k0 * (k0 - 1.0) / k1;
}

float Shader_GalaxyEffect_1_SDRoundBox(vec3 p, vec3 b, float r)
{
    vec3 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0) - r;
}

float Shader_GalaxyEffect_1_SDSphere(vec3 p, float r)
{
    return length(p) - r;
}

float Shader_GalaxyEffect_1_RaycastSphereFront(vec3 ro, vec3 rd, float tmax, float r)
{
    float t = 0.0;
    for (int i = 0; i < 30; i++)
    {
        vec3 pos = ro + t * rd;
        float h = Shader_GalaxyEffect_1_SDSphere(pos, r);
        if (h < 0.0001 || t > tmax)
        {
            break;
        }
        t += h;
    }
    return (t < tmax) ? t : -1.0;
}

float Shader_GalaxyEffect_1_RaycastSphereBack(vec3 ro, vec3 rd, float tmax, float r)
{
    bool hitFrontFace = false;
    float t = 0.0;
    for (int i = 0; i < 30; i++)
    {
        vec3 pos = ro + t * rd;
        float h = Shader_GalaxyEffect_1_SDSphere(pos, r);
        if (hitFrontFace && h > 0.0)
        {
            break;
        }
        if (!hitFrontFace && h < 0.0001)
        {
            hitFrontFace = true;
            t += 0.5;
        }
        if (t > tmax)
        {
            break;
        }
        t += abs(h);
    }
    return (t < tmax) ? t : -1.0;
}

vec4 Shader_GalaxyEffect_1_SphereOuterSwirl(vec3 p, vec3 c, float rmax)
{
    vec4 col = vec4(0.0);
    float r = 0.9;
    float d = length(p - c) / r;
    if (d < 1.0)
    {
        float a = 0.0;
        float l1 = 0.25;
        float l2 = 0.7;
        float a1 = 1.0;
        float a2 = 0.9;
        if (d < l1)
        {
            a = a1 * pow((d / l1), 0.5);
        }
        else if ((d >= l1) && (d <= l2))
        {
            a = a1 - (a1 - a2) * (d - l1) / (l2 - l1);
        }
        else if (d > l2)
        {
            a = a2 * (1.0 - pow((d - l2) / (1.0 - l2), 2.0));
        }
        float count = 7.0;
        float dm = abs(mod(d - (time * 0.05 + increaseSpectrumValue * 2.0), 1.0));
        float i = floor(dm / (1.0 / count));
        float ip = mod((i / count) + (0.5 / count), 1.0);
        float rl = abs(dm - ip);
        rmax *= pow((1.0 - d), 1.2);
        float rp = clamp(1.0 - (rl / rmax), 0.0, 1.0);
        a *= pow(rp, 0.5);
        vec3 bgc1 = vec3(1.0);
        vec3 bgc2 = tintColor;
        vec4 bgc = vec4(bgc1 + (bgc2 - bgc1) * d, a);
        col = mix(col, bgc, bgc.a);
        if (rp > 0.95)
        {
            float lcp = (d < 0.5) ? 0.0 : ((d - 0.5) / (1.0 - 0.5));
            vec3 lc1 = vec3(1.0);
            vec3 lc2 = (vec3(1.0) + tintColor) * 0.5;
            vec4 lc = vec4(lc1 + (lc2 - lc1) * lcp, a);
            col = mix(col, lc, lc.a);
        }
    }
    return col;
}

vec4 Shader_GalaxyEffect_1_SphereOuterCol(vec3 p, float rmax)
{
    vec4 col = vec4(0.0);
    float cs = 0.8;
    vec3 c1 = vec3(cos(pi * 2.0 * (1.0 / 12.0 + 0.0 / 3.0)),  sin(pi * 1.0 / 3.0), sin(pi * 2.0 * (1.0 / 12.0 + 0.0 / 3.0))) * cs;
    vec3 c2 = vec3(cos(pi * 2.0 * (1.0 / 12.0 + 1.0 / 3.0)),  sin(pi * 1.0 / 3.0), sin(pi * 2.0 * (1.0 / 12.0 + 1.0 / 3.0))) * cs;
    vec3 c3 = vec3(cos(pi * 2.0 * (1.0 / 12.0 + 2.0 / 3.0)),  sin(pi * 1.0 / 3.0), sin(pi * 2.0 * (1.0 / 12.0 + 2.0 / 3.0))) * cs;
    vec3 c4 = vec3(cos(pi * 2.0 * (1.0 / 12.0 + 0.0 / 3.0)), -sin(pi * 1.0 / 3.0), sin(pi * 2.0 * (1.0 / 12.0 + 0.0 / 3.0))) * cs;
    vec3 c5 = vec3(cos(pi * 2.0 * (1.0 / 12.0 + 1.0 / 3.0)), -sin(pi * 1.0 / 3.0), sin(pi * 2.0 * (1.0 / 12.0 + 1.0 / 3.0))) * cs;
    vec3 c6 = vec3(cos(pi * 2.0 * (1.0 / 12.0 + 2.0 / 3.0)), -sin(pi * 1.0 / 3.0), sin(pi * 2.0 * (1.0 / 12.0 + 2.0 / 3.0))) * cs;
    vec4 swirlCol1 = Shader_GalaxyEffect_1_SphereOuterSwirl(p, c1, rmax);
    vec4 swirlCol2 = Shader_GalaxyEffect_1_SphereOuterSwirl(p, c2, rmax);
    vec4 swirlCol3 = Shader_GalaxyEffect_1_SphereOuterSwirl(p, c3, rmax);
    vec4 swirlCol4 = Shader_GalaxyEffect_1_SphereOuterSwirl(p, c4, rmax);
    vec4 swirlCol5 = Shader_GalaxyEffect_1_SphereOuterSwirl(p, c5, rmax);
    vec4 swirlCol6 = Shader_GalaxyEffect_1_SphereOuterSwirl(p, c6, rmax);
    col = Shader_GalaxyEffect_1_Max(col, swirlCol1);
    col = Shader_GalaxyEffect_1_Max(col, swirlCol2);
    col = Shader_GalaxyEffect_1_Max(col, swirlCol3);
    col = Shader_GalaxyEffect_1_Max(col, swirlCol4);
    col = Shader_GalaxyEffect_1_Max(col, swirlCol5);
    col = Shader_GalaxyEffect_1_Max(col, swirlCol6);
    return col;
}

vec4 Shader_GalaxyEffect_1_SphereOuterFront(vec2 galaxyCoordinate)
{
    vec4 col = vec4(0.0);
    vec2 p = galaxyCoordinate;
    if (length(p) < 0.9)
    {
        vec3 ro = vec3(0.0, 0.0, 1.8);
        vec3 rd = normalize(vec3(p.xy, -1.0));
        float r = 1.05 + 0.15 * spectrumValue;
        float d = Shader_GalaxyEffect_1_RaycastSphereFront(ro, rd, 2.0, r);
        if (d > 0.0)
        {
            vec3 pos = ro + d * rd;
            vec3 rp = (rotationMatrix * vec4(pos, 1.0)).xyz;
            vec4 frontColor = Shader_GalaxyEffect_1_SphereOuterCol(rp, 0.1);
            frontColor *= 0.85;
            col = mix(col, frontColor, frontColor.a);
        }
    }
    return col;
}

vec4 Shader_GalaxyEffect_1_SphereOuterBack(vec2 galaxyCoordinate)
{
    vec4 col = vec4(0.0);
    vec2 p = galaxyCoordinate;
    if (length(p) < 0.9)
    {
        vec3 ro = vec3(0.0, 0.0, 1.8);
        vec3 rd = normalize(vec3(p.xy, -1.0));
        float r = 1.05 + 0.15 * spectrumValue;
        float d = Shader_GalaxyEffect_1_RaycastSphereBack(ro, rd, 3.0, r);
        if (d > 0.0)
        {
            vec3 pos = ro + d * rd;
            vec3 rp = (rotationMatrix * vec4(pos, 1.0)).xyz;
            vec4 backColor = Shader_GalaxyEffect_1_SphereOuterCol(rp, 0.04);
            backColor *= pow(1.0 - abs(ro + d * rd).z / r, 1.2);
            col = mix(col, backColor, backColor.a);
        }
    }
    return col;
}

vec4 Shader_GalaxyEffect_1_LollipopCol(vec3 p)
{
    vec4 col = vec4(0.0);
    float angle = atan(p.z, p.x);
    angle += sin(p.y + time * 0.2) * 3.0;
    angle = (angle < 0.0) ? (angle + 2.0 * pi) : angle;
    float ap = angle / (2.0 * pi);
    ap = mod(ap * 150.0, 1.0);
    float a = pow((abs(ap - 0.5) / 0.5), 0.5);
    col = vec4(tintColor, a);
    return col;
}

vec4 Shader_GalaxyEffect_1_ParticleCol(vec3 p)
{
    vec4 col = vec4(0.0);
    float angle = atan(p.z, p.x);
    angle = (angle < 0.0) ? (angle + 2.0 * pi) : angle;
    float x = angle / (2.0 * pi);
    float y = ((p.y / 0.85) + 1.0) * 0.5;
    vec2 uv = vec2(x, y);
    uv += (Shader_GalaxyEffect_1_N22(uv * 2.0 + time * 0.1) * 2.0 - 1.0) * 0.05;
    uv *= (100.0 * vec2(2.0, 1.0));
    vec2 gridPosition = fract(uv);
    vec2 gridId = floor(uv);
    vec2 pc = Shader_GalaxyEffect_1_N22(gridId) * 0.5;
    float pr = length(gridPosition - pc);
    float pl = 0.1 + 0.2 * pc.x;
    float a = 0.0;
    a = (pr < pl) ? (1.0 - (pr / pl)) : 0.0;
    float n = Shader_GalaxyEffect_1_N21(p.xy * 3.0 + vec2(time * 0.1));
    n = (n < 0.3) ? 0.0 : ((n > 0.6) ? 1.0 : (n - 0.3) / 0.3);
    a *= n;
    float vp = 1.0 - (abs(y - 0.5) / 0.5);
    vp = (vp > 0.1) ? 0.1 : vp;
    vp = vp / 0.1;
    a *= vp;
    vec3 c = (vec3(1.0) + tintColor) / 2.0;
    col = vec4(c, a);
    return col;
}

vec4 Shader_GalaxyEffect_1_SphereInner(vec2 galaxyCoordinate)
{
    vec4 col = vec4(0.0);
    vec2 p = galaxyCoordinate;
    if (length(p) < 0.7)
    {
        vec3 ro = vec3(0.0, 0.0, 1.8);
        vec3 rd = normalize(vec3(p.xy, -1.0));
        float r = 0.95 + 0.1 * spectrumValue;
        float d = Shader_GalaxyEffect_1_RaycastSphereFront(ro, rd, 2.0, r);
        if (d > 0.0)
        {
            vec3 pos = ro + d * rd;
            vec3 rp = (rotationMatrix * vec4(pos, 1.0)).xyz;
            vec4 lollipopCol = Shader_GalaxyEffect_1_LollipopCol(rp);
            float a1 = Shader_GalaxyEffect_1_N21((p * 2.0 + vec2(time * 0.1 + increaseSpectrumValue * 0.5)));
            a1 = (a1 < 0.5) ? 0.0 : pow((a1 - 0.5) / 0.5, 0.5);
            float a2 = sqrt(pow(pos.x, 2.0) + pow(pos.y, 2.0)) / 0.8;
            float a2min = 0.3;
            float a2max = 0.5;
            a2 = (a2 < a2min) ? a2min : a2;
            a2 = (a2 > a2max) ? a2max : a2;
            a2 = pow((a2 - a2min) / (a2max - a2min), 0.5);
            float a = min(a1, a2);
            a *= 0.7;
            lollipopCol *= a;
            vec4 particleCol = Shader_GalaxyEffect_1_ParticleCol(pos);
            a1 = Shader_GalaxyEffect_1_N21((p * 2.0 + vec2(time * 0.1 + increaseSpectrumValue * 0.5)));
            a1 = (a1 < 0.5) ? 0.0 : pow((a1 - 0.5) / 0.5, 0.5);
            a = a1;
            float np = sqrt(pow(pos.x, 2.0) + pow(pos.y, 2.0)) / 0.8;
            a2 = np;
            float amin1 = 0.3;
            float amin2 = 0.4;
            float amax1 = 0.8;
            float amax2 = 0.9;
            if (a2 < amin1)
            {
                a = 0.0;
            }
            else if ((a2 > amin1) && (a2 < amin2))
            {
                a2 = (a2 - amin1) / (amin2 - amin1);
                a = max(a1, a2);
            }
            else if ((a2 > amin2) && (a2 < amax1))
            {
                a2 = 1.0;
                a = max(a1, a2);
            }
            else if ((a2 > amax1) && (a2 < amax2))
            {
                a2 = 1.0 - (a2 - amax1) / (amax2 - amax1);
                a = max(a1, a2);
            }
            else if (a2 > amax2)
            {
                a = 0.0;
            }
            a *= 0.7;
            particleCol *= a;
            col = Shader_GalaxyEffect_1_Max(lollipopCol, particleCol);
        }
    }
    return col;
}

vec4 Shader_GalaxyEffect_1_CenterLight(vec2 galaxyCoordinate)
{
    vec4 col = vec4(0.0);
    vec2 p = galaxyCoordinate;
    vec4 bg = vec4(tintColor, 1.0) * pow(clamp(1.0 - length(p), 0.0, 1.0), 0.4);
    col = mix(col, bg, bg.a);
    float d = length(p);
    float da = 0.7 + 0.2 * spectrumValue + 0.03 * Shader_GalaxyEffect_1_N21(vec2(30.0 * atan(p.y / p.x)));
    float a = (d < da) ? (1.0 - Shader_GalaxyEffect_1_Gain(d / da, 2.0)) : 0.0;
    float dc = 0.5 + 0.1 * spectrumValue;
    vec3 c = (d < dc) ? (vec3(1.0) + (tintColor - vec3(1.0)) * Shader_GalaxyEffect_1_Gain(d / dc, 2.0)) : tintColor;
    vec4 light = vec4(c, a);
    col = mix(col, light, light.a);
    return col;
}

mat4 Shader_GalaxyEffect_1_RotationMatrixZ(float rotatationZ)
{
    return mat4(cos(rotatationZ), sin(rotatationZ), 0.0, 0.0,
                -sin(rotatationZ), cos(rotatationZ), 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0,
                0.0, 0.0, 0.0, 1.0);
}

float Shader_GalaxyEffect_1_SDNote(vec3 p)
{
    mat4 rotationMatrix = Shader_GalaxyEffect_1_RotationMatrixZ(pi * 20.0 / 180.0);
    float sdEllipsoid = Shader_GalaxyEffect_1_SDEllipsoid((rotationMatrix * vec4(p, 1.0)).xyz + vec3(0.06, -0.12, 0.0), vec3(0.15, 0.12, 0.1));
    float sdRoundBox1 = Shader_GalaxyEffect_1_SDRoundBox((rotationMatrix * vec4(p, 1.0)).xyz + vec3(0.06, -0.12, 0.0), vec3(0.2, 0.2, 0.03), 0.005);
    float sdRoundBox2 = Shader_GalaxyEffect_1_SDRoundBox(p + vec3(-0.103, 0.12, 0.0), vec3(0.03, 0.25, 0.03), 0.005);
    return min(max(sdEllipsoid, sdRoundBox1), sdRoundBox2);
}

vec3 Shader_GalaxyEffect_1_NormalNote(vec3 p)
{
    return normalize(vec3(
                          Shader_GalaxyEffect_1_SDNote(vec3(p.x + 0.0001, p.y, p.z)) - Shader_GalaxyEffect_1_SDNote(vec3(p.x - 0.0001, p.y, p.z)),
                          Shader_GalaxyEffect_1_SDNote(vec3(p.x, p.y + 0.0001, p.z)) - Shader_GalaxyEffect_1_SDNote(vec3(p.x, p.y - 0.0001, p.z)),
                          Shader_GalaxyEffect_1_SDNote(vec3(p.x, p.y, p.z + 0.0001)) - Shader_GalaxyEffect_1_SDNote(vec3(p.x, p.y, p.z - 0.0001))
                          ));
}

vec3 Shader_GalaxyEffect_1_IlluminationColor(vec3 k_a, vec3 k_d, vec3 k_s, float alpha, vec3 p, vec3 eye)
{
    vec3 ambientLight = vec3(1.0, 1.0, 1.0);
    vec3 color = ambientLight * k_a;
    vec3 lightPos = vec3(1.0, 1.0, 1.0);
    vec3 lightIntensity = vec3(1.0, 1.0, 1.0);
    vec3 N = Shader_GalaxyEffect_1_NormalNote(p);
    vec3 L = normalize(lightPos - p);
    vec3 V = normalize(eye - p);
    vec3 R = normalize(reflect(-L, N));
    float dotLN = dot(L, N);
    float dotRV = dot(R, V);
    if (dotLN < 0.0)
    {
        color += k_d * 0.4;
    }
    else if (dotRV < 0.0)
    {
        color +=  lightIntensity * (k_d * dotLN);
    }
    else
    {
        color += lightIntensity * (k_d * dotLN + k_s * pow(dotRV, alpha));
    }
    return color;
}

float Shader_GalaxyEffect_1_RaycastNote(vec3 ro, vec3 rd, float tmax)
{
    float t = 0.0;
    for (int i = 0; i < 30; i++)
    {
        vec3 pos = ro + t * rd;
        vec3 rp = (rotationMatrix * vec4(pos, 1.0)).xyz;
        float h = Shader_GalaxyEffect_1_SDNote(rp);
        if (h < 0.0001 || t > tmax)
        {
            break;
        }
        t += h;
    }
    return (t < tmax) ? t : -1.0;
}

vec4 Shader_GalaxyEffect_1_Note(vec2 galaxyCoordinate)
{
    vec4 col = vec4(0.0);
    vec2 p = galaxyCoordinate;
    if (length(p) < 0.25)
    {
        vec3 ro = vec3(0.0, 0.0, 1.8);
        vec3 rd = normalize(vec3(p.xy, -1.0));
        float d = Shader_GalaxyEffect_1_RaycastNote(ro, rd, 2.0);
        if (d > 0.0)
        {
            vec3 pos = ro + d * rd;
            const vec3 ambientColor = vec3(130.0 / 255.0, 80.0 / 255.0, 30.0 / 255.0);
            vec3 diffuseColor = tintColor * 0.6;
            const vec3 specularColor = vec3(1.0);
            float shininess = 4.0;
            vec3 illuminationColor = Shader_GalaxyEffect_1_IlluminationColor(ambientColor, diffuseColor, specularColor, shininess, pos, ro);
            col = vec4(illuminationColor, 1.0);
        }
    }
    return col;
}

vec4 Shader_GalaxyEffect_1_TrackCol(vec2 p, vec3 tp)
{
    vec4 col = vec4(0.0);
    if (length(tp) > 1.0)
    {
        tp /= length(tp);
    }
    tp *= 0.9;
    tp = (rotationMatrix * vec4(tp, 1.0)).xyz;
    if (length(p - tp.xy) < 0.08)
    {
        float cr = 0.8;
        float zp = (- tp.z / cr + 1.0) * 0.5;
        float r = (0.04 + 0.04 * zp);
        float d = length(p - tp.xy) / r;
        if (d < 1.0)
        {
            vec3 c = vec3(1.0) + (tintColor - vec3(1.0)) * d;
            float a = Shader_GalaxyEffect_1_Gain(1.0 - d, 1.5);
            a = ((d > 0.27) && (d < 0.3)) ? 0.9 : a;
            a = ((d > 0.42) && (d < 0.45)) ? 0.8 : a;
            a *= (zp < 0.5) ? 0.5 + 0.5 * (zp / 0.5) : 1.0;
            col = vec4(c, a);
        }
    }
    return col;
}

vec4 Shader_GalaxyEffect_1_Track(vec2 galaxyCoordinate)
{
    vec4 col = vec4(0.0);
    vec2 p = galaxyCoordinate;
    for (int i = 0; i < trackCount; i ++)
    {
        vec3 tp = vec3(trackArray[i * 3], trackArray[i * 3 + 1], trackArray[i * 3 + 2]);
        vec4 tc = Shader_GalaxyEffect_1_TrackCol(p, tp);
        col = mix(col, tc, tc.a);
    }
    return col;
}

vec2 Shader_GalaxyEffect_1_EllipseCoordinate(float start, float interval, float timePercent, float y)
{
    return vec2(start + interval * timePercent, y);
}

float Shader_GalaxyEffect_1_EllipseAlpha(float alpha_min, float alpha_max, float noiseX)
{
    return alpha_min + (alpha_max - alpha_min) * Shader_GalaxyEffect_1_N21(vec2(noiseX, 0));
}

float Shader_GalaxyEffect_1_EllipseWidth(float ellipseWidth_min, float ellipseWidth_max, float noiseX)
{
    return ellipseWidth_min + (ellipseWidth_max - ellipseWidth_min) * Shader_GalaxyEffect_1_N21(vec2(noiseX, 0));
}

float Shader_GalaxyEffect_1_EllipseDepth_AB(vec2 ellipseCoordinate,
                                            vec2 coordinate,
                                            float ellipseA,
                                            float ellipseB)
{
    ellipseA = ellipseA * 3.0;
    ellipseB = ellipseB * 2.0;
    float ellipseDistance = pow((ellipseCoordinate.x - coordinate.x) * aspectRatio, 2.0) / pow(ellipseA, 2.0) + pow((ellipseCoordinate.y - coordinate.y), 2.0) / pow(ellipseB, 2.0);
    ellipseDistance = clamp(ellipseDistance, 0.0, 1.0);
    float ellipseDepth = 0.5 * pow(2.0 * ((ellipseDistance < 0.5) ? ellipseDistance : (1.0 - ellipseDistance)), 1.5);
    ellipseDepth = (ellipseDistance < 0.5) ? (1.0 - ellipseDepth) : ellipseDepth;
    ellipseDepth = clamp(ellipseDepth, 0.0, 1.0);
    ellipseDepth = pow(ellipseDepth, 6.0);
    ellipseDepth = ellipseDepth * 0.8;
    return ellipseDepth;
}

vec2 Shader_GalaxyEffect_1_ColorAndAlphaPercent(vec2 coordinate,
                                                float colorEffectTime,
                                                float timePercent,
                                                float ellipseCoordinateY,
                                                float ellipseScaleY,
                                                float noiseInterval)
{
    vec2 ellipseCoordinate_1 = Shader_GalaxyEffect_1_EllipseCoordinate(   1.1, - 2.565, timePercent, ellipseCoordinateY);
    vec2 ellipseCoordinate_2 = Shader_GalaxyEffect_1_EllipseCoordinate(- 0.11,    0.65, timePercent, ellipseCoordinateY);
    vec2 ellipseCoordinate_3 = Shader_GalaxyEffect_1_EllipseCoordinate(  0.35,  - 1.04, timePercent, ellipseCoordinateY);
    vec2 ellipseCoordinate_4 = Shader_GalaxyEffect_1_EllipseCoordinate(- 0.83,    1.71, timePercent, ellipseCoordinateY);
    float ellipseAlpha_1 = Shader_GalaxyEffect_1_EllipseAlpha(0.3, 0.7, colorEffectTime + 1.0 * noiseInterval);
    float ellipseAlpha_2 = Shader_GalaxyEffect_1_EllipseAlpha(0.7, 0.9, colorEffectTime + 2.0 * noiseInterval);
    float ellipseAlpha_3 = Shader_GalaxyEffect_1_EllipseAlpha(0.4, 0.6, colorEffectTime + 3.0 * noiseInterval);
    float ellipseAlpha_4 = Shader_GalaxyEffect_1_EllipseAlpha(0.8, 1.0, colorEffectTime + 4.0 * noiseInterval);
    float ellipseWidthA_1 = Shader_GalaxyEffect_1_EllipseWidth(0.24, 0.59, colorEffectTime +  5.0 * noiseInterval);
    float ellipseWidthB_1 = Shader_GalaxyEffect_1_EllipseWidth(0.25, 0.87, colorEffectTime +  6.0 * noiseInterval);
    float ellipseWidthA_2 = Shader_GalaxyEffect_1_EllipseWidth( 0.5, 0.63, colorEffectTime +  7.0 * noiseInterval);
    float ellipseWidthB_2 = Shader_GalaxyEffect_1_EllipseWidth(0.36, 0.54, colorEffectTime +  8.0 * noiseInterval);
    float ellipseWidthA_3 = Shader_GalaxyEffect_1_EllipseWidth(0.22, 0.57, colorEffectTime +  9.0 * noiseInterval);
    float ellipseWidthB_3 = Shader_GalaxyEffect_1_EllipseWidth(0.35, 0.82, colorEffectTime + 10.0 * noiseInterval);
    float ellipseWidthA_4 = Shader_GalaxyEffect_1_EllipseWidth(0.48, 0.51, colorEffectTime + 11.0 * noiseInterval);
    float ellipseWidthB_4 = Shader_GalaxyEffect_1_EllipseWidth(0.09, 0.27, colorEffectTime + 12.0 * noiseInterval);
    float ellipseDepth_1 = Shader_GalaxyEffect_1_EllipseDepth_AB(ellipseCoordinate_1, coordinate, ellipseWidthA_1, ellipseWidthB_1 * ellipseScaleY);
    float ellipseDepth_2 = Shader_GalaxyEffect_1_EllipseDepth_AB(ellipseCoordinate_2, coordinate, ellipseWidthA_2, ellipseWidthB_2 * ellipseScaleY);
    float ellipseDepth_3 = Shader_GalaxyEffect_1_EllipseDepth_AB(ellipseCoordinate_3, coordinate, ellipseWidthA_3, ellipseWidthB_3 * ellipseScaleY);
    float ellipseDepth_4 = Shader_GalaxyEffect_1_EllipseDepth_AB(ellipseCoordinate_4, coordinate, ellipseWidthA_4, ellipseWidthB_4 * ellipseScaleY);
    float ellipsePercent_1 = ellipseAlpha_1 * ellipseDepth_1;
    float ellipsePercent_2 = ellipseAlpha_2 * ellipseDepth_2 * (1.0 - ellipsePercent_1);
    float ellipsePercent_3 = ellipseAlpha_3 * ellipseDepth_3 * (1.0 - ellipsePercent_1 - ellipsePercent_2);
    float ellipsePercent_4 = ellipseAlpha_4 * ellipseDepth_4 * (1.0 - ellipsePercent_1 - ellipsePercent_2 - ellipsePercent_3);
    float ellipseColor_1 = 0.9;
    float ellipseColor_2 = 0.1;
    float ellipseColor_3 = 0.9;
    float ellipseColor_4 = 0.1;
    float colorPercent = ellipseColor_1 * ellipsePercent_1 + ellipseColor_2 * ellipsePercent_2 + ellipseColor_3 * ellipsePercent_3 + ellipseColor_4 * ellipsePercent_4;
    float alphaPercent = ellipsePercent_1 + ellipsePercent_2 + ellipsePercent_3 + ellipsePercent_4;
    return vec2(colorPercent, alphaPercent);
}

vec4 Shader_GalaxyEffect_1_ColorEffect()
{
    vec2 coordinate = (textureCoordinate * 2.0 - vec2(1.0, 1.0)) * vec2(1.0, - 1.0);
    float duraion = 14.0;
    float colorEffectTime = time * 0.5;
    float timePercent_up = fract((time / duraion) + 0.25);
    timePercent_up = (timePercent_up < 0.5) ? (2.0 * timePercent_up) : (1.0 - 2.0 * (timePercent_up - 0.5));
    float timePercent_down = fract(time / duraion);
    timePercent_down = (timePercent_down < 0.5) ? (2.0 * timePercent_down) : (1.0 - 2.0 * (timePercent_down - 0.5));
    vec2 colorAndAlphaPercent_up = Shader_GalaxyEffect_1_ColorAndAlphaPercent(coordinate, colorEffectTime, timePercent_up, 1.35, 1.0, 150.0);
    vec2 colorAndAlphaPercent_down = Shader_GalaxyEffect_1_ColorAndAlphaPercent(coordinate, colorEffectTime, timePercent_down, - 1.1, 1.4, 100.0);
    float colorPercent = max(colorAndAlphaPercent_up.x, colorAndAlphaPercent_down.x);
    float alphaPercent = max(colorAndAlphaPercent_up.y, colorAndAlphaPercent_down.y);
    vec3 firstColor = (vec3(1.0) + tintColor) * 0.5;
    vec3 secondColor = tintColor * 1.1;
    vec3 color = firstColor + (secondColor - firstColor) * colorPercent;
    return vec4(color, 1.0) * alphaPercent;
}

float Shader_GalaxyEffect_1_DataSourceSpectrumValue(int dataSourceIndex, float dataSourceCount)
{
    float halfIndex = (dataSourceCount - 1.0) * 0.5;
    float percent = 1.0 - abs(halfIndex - float(dataSourceIndex)) / halfIndex;
    float dataSourceSpectrumValue = spectrumArray[dataSourceIndex] * percent;
    return dataSourceSpectrumValue;
}

float Shader_GalaxyEffect_1_VisualSpectrumValue(int visualIndex, float insertCount, float dataSourceCount)
{
    int dataSourceIndex = int(floor(float(visualIndex) / (insertCount + 1.0)));
    int insertIndex = int(mod(float(visualIndex), insertCount + 1.0));
    float leftSpectrumValue = Shader_GalaxyEffect_1_DataSourceSpectrumValue(dataSourceIndex, dataSourceCount);
    float rightSpectrumValue = Shader_GalaxyEffect_1_DataSourceSpectrumValue(dataSourceIndex + 1, dataSourceCount);
    float outputValue_Min = min(leftSpectrumValue, rightSpectrumValue);
    float outputValue_Max = max(leftSpectrumValue, rightSpectrumValue);
    float insertPercent = (leftSpectrumValue > rightSpectrumValue) ? (1.0 - (float(insertIndex) / (insertCount + 1.0))) : (float(insertIndex) / (insertCount + 1.0));
    float visualSpectrumValue = outputValue_Min + (outputValue_Max-outputValue_Min) * (sin(-pi/2.0 + pi * insertPercent) + 1.0) / 2.0;
    return visualSpectrumValue;
}

vec4 Shader_GalaxyEffect_1_Spectrum(vec2 spectrumCoordinate)
{
    float dataSourceCount = 16.0;
    float insertCount = 5.0;
    float spectrumCount = dataSourceCount + (dataSourceCount - 1.0) * insertCount;
    float spectrumGapScale = 1.0;
    float spectrumWidth = 1.0 / (spectrumCount * (1.0 + spectrumGapScale) - spectrumGapScale);
    float spectrumGap = spectrumWidth * spectrumGapScale;
    int visualIndex = int(spectrumCoordinate.x/(spectrumWidth+spectrumGap));
    float visualSpectrumValue = Shader_GalaxyEffect_1_VisualSpectrumValue(visualIndex, insertCount, dataSourceCount);
    float visualBgSpectrumValue = Shader_GalaxyEffect_1_VisualSpectrumValue(int(spectrumCount - 1.0 - float(visualIndex)), insertCount, dataSourceCount);
    float xStep = step(float(visualIndex) * (spectrumWidth+spectrumGap), spectrumCoordinate.x) * step(spectrumCoordinate.x, float(visualIndex) * (spectrumWidth+spectrumGap) + spectrumWidth);
    float yStep = step((1.0-spectrumCoordinate.y), visualSpectrumValue);
    float bgYStep = step((1.0-spectrumCoordinate.y), visualBgSpectrumValue);
    vec4 spectrumColor = vec4(1.0) * xStep * yStep;
    vec4 bgSpectrumColor = vec4(1.0) * 0.6 * xStep * bgYStep;
    vec4 color = mix(bgSpectrumColor, spectrumColor, xStep * yStep);
    float alpha = (0.5-abs(spectrumCoordinate.x-0.5)) * 2.0;
    alpha = pow(alpha, 0.5);
    color = color * alpha;
    return color;
}

void main()
{
    vec4 color = vec4(0.1, 0.1, 0.1, 1.0);
    vec4 colorEffect = Shader_GalaxyEffect_1_ColorEffect();
    color = mix(color, colorEffect, colorEffect.a);
    vec2 galaxyCoordinate = Shader_GalaxyEffect_1_GalaxyCoordinate();
    if (galaxyCoordinate.x > -0.999 && galaxyCoordinate.x < 0.999 &&
        galaxyCoordinate.y > -0.999 && galaxyCoordinate.y < 0.999)
    {
        vec4 sphereOuterBack = Shader_GalaxyEffect_1_SphereOuterBack(galaxyCoordinate);
        vec4 sphereOuterFront = Shader_GalaxyEffect_1_SphereOuterFront(galaxyCoordinate);
        vec4 sphereInner = Shader_GalaxyEffect_1_SphereInner(galaxyCoordinate);
        vec4 centerLight = Shader_GalaxyEffect_1_CenterLight(galaxyCoordinate);
        vec4 note = Shader_GalaxyEffect_1_Note(galaxyCoordinate);
        vec4 track = Shader_GalaxyEffect_1_Track(galaxyCoordinate);
        color = mix(color, sphereOuterBack, sphereOuterBack.a);
        color = mix(color, sphereInner, sphereInner.a);
        color = mix(color, centerLight, centerLight.a);
        color = mix(color, note, note.a);
        color = Shader_GalaxyEffect_1_Add(color, sphereOuterFront);
        color = Shader_GalaxyEffect_1_Add(color, track);
    }
    vec2 spectrumCoordinate = Shader_GalaxyEffect_1_SpectrumCoordinate();
    if (spectrumCoordinate.x > 0.001 && spectrumCoordinate.x < 0.999 &&
        spectrumCoordinate.y > 0.001 && spectrumCoordinate.y < 0.999)
    {
        vec4 spectrum = Shader_GalaxyEffect_1_Spectrum(spectrumCoordinate);
        color = mix(color, spectrum, spectrum.a);
    }
    color = vec4(color.rgb, 1.0);
    gl_FragColor = color;
}
