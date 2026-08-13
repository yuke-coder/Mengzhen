precision highp float;
varying vec2 textureCoordinate;        // 纹理坐标（通用的）
uniform sampler2D inputImageTexture;   // 纹理（通用的）
uniform float trackArray[100];         // 音轨数组{x1,y1,z1,b1,x2,y2,z2,b2,...}
uniform int trackCount;                // 音轨数组数量
uniform float aspectRatio;             // 宽高比
uniform float time;                    // 时间
uniform float spectrumValue;           // 频谱数据（取频谱里数组里最大值）
uniform float increaseSpectrumValue;   // 累积频谱值（increaseSpectrumValue += spectrumValue * 0.01，超过100后归0重来）
uniform float sphereCenterY;           // 球体中心点Y（归一化）
uniform vec3 color1;                   // 颜色1
uniform vec3 color2;                   // 颜色2
uniform mat4 rotationMatrix;           // 旋转矩阵

const float pi = 3.1415926;

vec2 dolbyCoordinate;

vec2 Shader_DolbyEffect_1_DolbyCoordinate()
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
    return vec2(x, y) * 2.0 - vec2(1.0);
}

mat4 transposeMat4(mat4 m)
{
    return mat4(
                m[0][0], m[1][0], m[2][0], m[3][0],
                m[0][1], m[1][1], m[2][1], m[3][1],
                m[0][2], m[1][2], m[2][2], m[3][2],
                m[0][3], m[1][3], m[2][3], m[3][3]
                );
}

vec4 Shader_DolbyEffect_1_Max(vec4 c1, vec4 c2)
{
    return (c1.a > c2.a) ? c1 : c2;
}

vec4 Shader_DolbyEffect_1_Add(vec4 c1, vec4 c2)
{
    return c1 + c2 * c2.a;
}

float Shader_DolbyEffect_1_Gain(float x, float k)
{
    float a = 0.5 * pow(2.0 * ((x < 0.5) ? x : 1.0 - x), k);
    return (x < 0.5) ? a : 1.0 - a;
}

float Shader_DolbyEffect_1_Random(vec2 p)
{
    return fract(sin(p.x * 12.9898 + p.y * 78.233) * 43758.5453123);
}

float Shader_DolbyEffect_1_N21(vec2 p)
{
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = Shader_DolbyEffect_1_Random(i);
    float b = Shader_DolbyEffect_1_Random(i + vec2(1.0, 0.0));
    float c = Shader_DolbyEffect_1_Random(i + vec2(0.0, 1.0));
    float d = Shader_DolbyEffect_1_Random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

vec2 Shader_DolbyEffect_1_N22(vec2 p)
{
    float n = Shader_DolbyEffect_1_N21(p);
    return vec2(n, Shader_DolbyEffect_1_N21(p+n));
}

float Shader_DolbyEffect_1_Hash1(float n)
{
    return fract(sin(n) * 43758.5453123);
}

float Shader_DolbyEffect_1_N31(vec3 p)
{
    vec3 s = vec3(110.0, 241.0, 171.0);
    vec3 i = floor(p);
    vec3 f = fract(p);
    float n = dot(i, s);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(Shader_DolbyEffect_1_Hash1(n),           Shader_DolbyEffect_1_Hash1(n + s.x), u.x),
                   mix(Shader_DolbyEffect_1_Hash1(n + s.y),     Shader_DolbyEffect_1_Hash1(n + s.x + s.y), u.x), u.y),
               mix(mix(Shader_DolbyEffect_1_Hash1(n + s.z),     Shader_DolbyEffect_1_Hash1(n + s.x + s.z), u.x),
                   mix(Shader_DolbyEffect_1_Hash1(n + s.y + s.z), Shader_DolbyEffect_1_Hash1(n + s.x + s.y + s.z), u.x), u.y), u.z);
}

vec2 Shader_DolbyEffect_1_pR(vec2 p, float a)
{
    float c = cos(a);
    float s = sin(a);
    return vec2(c * p.x + s * p.y, -s * p.x + c * p.y);
}

float Shader_DolbyEffect_1_SmoothMin(float a, float b, float k)
{
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

float Shader_DolbyEffect_1_SmoothMax(float a, float b, float k)
{
    return -Shader_DolbyEffect_1_SmoothMin(-a, -b, k);
}

float Shader_DolbyEffect_1_smin(float a, float b, float k)
{
    float f = clamp(0.5 + 0.5 * ((a - b) / k), 0.0, 1.0);
    return (1.0 - f) * a + f * b - f * (1.0 - f) * k;
}

float Shader_DolbyEffect_1_smax(float a, float b, float k)
{
    return -Shader_DolbyEffect_1_smin(-a, -b, k);
}

float Shader_DolbyEffect_1_smin2(float a, float b, float r)
{
    vec2 u = max(vec2(r - a, r - b), vec2(0.0));
    return max(r, min(a, b)) - length(u);
}

float Shader_DolbyEffect_1_smax2(float a, float b, float r)
{
    vec2 u = max(vec2(r + a, r + b), vec2(0.0));
    return min(-r, max(a, b)) + length(u);
}

float Shader_DolbyEffect_1_smin3(float a, float b, float k)
{
    return min(
               Shader_DolbyEffect_1_smin(a, b, k),
               Shader_DolbyEffect_1_smin2(a, b, k)
               );
}

float Shader_DolbyEffect_1_smax3(float a, float b, float k)
{
    return max(
               Shader_DolbyEffect_1_smax(a, b, k),
               Shader_DolbyEffect_1_smax2(a, b, k)
               );
}

float Shader_DolbyEffect_1_fCorner2(vec2 p)
{
    return length(max(p, vec2(0.0))) + min(max(p.x, p.y), 0.0);
}

float Shader_DolbyEffect_1_fDisc(vec3 p, float r)
{
    float l = length(p.xz) - r;
    return (l < 0.0) ? abs(p.y) : length(vec2(p.y, l));
}

vec2 Shader_DolbyEffect_1_pRi(vec2 p, float a)
{
    return Shader_DolbyEffect_1_pR(p, a);
}

float Shader_DolbyEffect_1_fBox(vec3 p, vec3 b)
{
    vec3 d = abs(p) - b;
    return length(max(d, vec3(0.0))) + min(max(d.x, max(d.y, d.z)), 0.0);
}

float Shader_DolbyEffect_1_sdRoundCone(vec3 p, float r1, float r2, float h)
{
    vec2 q = vec2(length(p.xz), p.y);
    float b = (r1 - r2) / h;
    float a = sqrt(1.0 - b * b);
    float k = dot(q, vec2(-b, a));
    if (k < 0.0) return length(q) - r1;
    if (k > a * h) return length(q - vec2(0.0, h)) - r2;
    return dot(q, vec2(a, b)) - r1;
}

float Shader_DolbyEffect_1_RoundedCylinder(vec3 p, float ra, float rb, float h)
{
    vec2 d = vec2(length(p.xz) - ra + rb, abs(p.y) - h + rb);
    return min(max(d.x, d.y), 0.0) + length(max(d, vec2(0.0))) - rb;
}

float Shader_DolbyEffect_1_Ellip(vec3 p, vec3 s)
{
    float r = min(min(s.x, s.y), s.z);
    p *= r / s;
    return length(p) - r;
}

float Shader_DolbyEffect_1_Ellip2(vec2 p, vec2 s)
{
    float r = min(s.x, s.y);
    p *= r / s;
    return length(p) - r;
}

float Shader_DolbyEffect_1_SDNoiseSphere(vec3 p, float r, float t, float isv)
{
    return length(p) - r - 0.2 + 0.2 * (1.0 + Shader_DolbyEffect_1_N31(3.5 * (p + t * 0.5 + isv * 2.0)));
}

vec4 Shader_DolbyEffect_1_BorderLight()
{
    vec4 col = vec4(0.0);
    float scale = 1.1 + 0.1 * spectrumValue;
    vec2 p = dolbyCoordinate;
    p = Shader_DolbyEffect_1_pR(p, mod(time, 100.0) * 0.1);
    float d = length(p);
    float da = 1.6 + 0.7 * Shader_DolbyEffect_1_N21(vec2(60.0 * atan(p.y, p.x)));
    float a = (d < da) ? (1.0 - Shader_DolbyEffect_1_Gain(d / da, 2.0)) : 0.0;
    float aScale = 1.0 - (d - 0.72 * scale) / (0.48 * scale);
    a *= aScale;
    vec3 c = color2 * 0.8;
    vec4 light = vec4(c, a);
    col = mix(col, light, light.a);
    return col;
}

vec3 Shader_DolbyEffect_1_IlluminationColorSphere(vec3 k_a, vec3 k_d, vec3 k_s, float alpha, vec3 p, vec3 eye, vec3 N, float t)
{
    vec3 ambientLight = vec3(1.0, 1.0, 1.0);
    vec3 color = ambientLight * k_a;
    
    vec3 lightPosition1 = vec3(sin(t * 0.2), cos(t * 0.2), 0.9);
    vec3 lightPosition2 = vec3(sin(t * 0.2 + 2.0), cos(t * 0.2 + 2.0), 0.95);
    vec3 lightPosition3 = vec3(sin(t * 0.2 + 4.0), cos(t * 0.2 + 4.0), 0.95);
    vec3 lightIntensity = vec3(1.0, 1.0, 1.0);
    vec3 V = normalize(eye - p);
    
    vec3 L1 = normalize(lightPosition1 - p);
    vec3 R1 = normalize(reflect(-L1, N));
    float NdotL1 = dot(L1, N);
    float diffuse1 = smoothstep(-0.2, 1.0, NdotL1);
    float specular1 = pow(max(dot(R1, V), 0.0), alpha) * smoothstep(0.0, 0.3, NdotL1);
    color += lightIntensity * (k_d * diffuse1 + k_s * specular1);
    
    vec3 L2 = normalize(lightPosition2 - p);
    vec3 R2 = normalize(reflect(-L2, N));
    float NdotL2 = dot(L2, N);
    float diffuse2 = smoothstep(-0.2, 1.0, NdotL2);
    float specular2 = pow(max(dot(R2, V), 0.0), alpha) * smoothstep(0.0, 0.3, NdotL2);
    color += lightIntensity * (k_d * diffuse2 + k_s * specular2);
    
    vec3 L3 = normalize(lightPosition3 - p);
    vec3 R3 = normalize(reflect(-L3, N));
    float NdotL3 = dot(L3, N);
    float diffuse3 = smoothstep(-0.2, 1.0, NdotL3);
    float specular3 = pow(max(dot(R3, V), 0.0), alpha) * smoothstep(0.0, 0.3, NdotL3);
    color += lightIntensity * (k_d * diffuse3 + k_s * specular3);
    
    color = min(color, vec3(1.0));
    return color;
}

vec3 Shader_DolbyEffect_1_NormalSphere(vec3 p, float r, float t, float isv)
{
    return normalize(vec3(
                          Shader_DolbyEffect_1_SDNoiseSphere(vec3(p.x + 0.0001, p.y, p.z), r, t, isv) - Shader_DolbyEffect_1_SDNoiseSphere(vec3(p.x - 0.0001, p.y, p.z), r, t, isv),
                          Shader_DolbyEffect_1_SDNoiseSphere(vec3(p.x, p.y + 0.0001, p.z), r, t, isv) - Shader_DolbyEffect_1_SDNoiseSphere(vec3(p.x, p.y - 0.0001, p.z), r, t, isv),
                          Shader_DolbyEffect_1_SDNoiseSphere(vec3(p.x, p.y, p.z + 0.0001), r, t, isv) - Shader_DolbyEffect_1_SDNoiseSphere(vec3(p.x, p.y, p.z - 0.0001), r, t, isv)
                          ));
}

float Shader_DolbyEffect_1_RaycastSphere(vec3 ro, vec3 rd, float tmax, float r, float t, float isv)
{
    float rOuter = r + 0.05;
    float b = dot(ro, rd);
    float c = dot(ro, ro) - rOuter * rOuter;
    float disc = b * b - c;
    
    if (disc < 0.0) return -1.0;
    
    float t0 = -b - sqrt(disc);
    
    if (t0 > tmax) return -1.0;
    
    float t_val = max(0.0, t0);
    for (int i = 0; i < 8; i++)
    {
        vec3 pos = ro + t_val * rd;
        float h = Shader_DolbyEffect_1_SDNoiseSphere(pos, r, t, isv);
        if (h < 0.0001)
        {
            break;
        }
        t_val += h;
    }
    
    return (t_val < tmax) ? t_val : -1.0;
}

vec4 Shader_DolbyEffect_1_Sphere()
{
    vec4 col = vec4(0.0);
    vec2 p = dolbyCoordinate;
    float screenR = length(p);
    if (screenR < 0.9)
    {
        vec3 ro = vec3(0.0, 0.0, 1.8);
        vec3 rd = normalize(vec3(p.xy, -1.0));
        float r = 1.05 + 0.05 * spectrumValue;
        float t = mod(time, 100.0);
        float isv = mod(increaseSpectrumValue, 100.0);
        float d = Shader_DolbyEffect_1_RaycastSphere(ro, rd, 2.0, r, t, isv);
        if (d > 0.0)
        {
            vec3 pos = ro + d * rd;
            vec3 rp = (rotationMatrix * vec4(pos, 1.0)).xyz;
            vec3 rN = Shader_DolbyEffect_1_NormalSphere(rp, r, t, isv);
            mat4 rotationMatrixInverse = transposeMat4(rotationMatrix);
            vec3 N = normalize((rotationMatrixInverse * vec4(rN, 0.0)).xyz);
            float screenR_max = r / sqrt(1.8 * 1.8 - r * r);
            float alphaPercent = clamp((screenR - screenR_max * 0.3) / (screenR_max * 0.6), 0.0, 1.0);
            alphaPercent = pow(alphaPercent, 2.0);
            float colorPercent = clamp((screenR - screenR_max * 0.4) / (screenR_max * 0.6), 0.0, 1.0);
            colorPercent = pow(colorPercent, 2.0);
            vec3 baseColor = mix(color2 * 0.8, color1, colorPercent);
            float sphereR = length(pos);
            if (sphereR < r - 0.12)
            {
                
            }
            else if (sphereR > r)
            {
                baseColor = baseColor * 1.3;
            }
            else
            {
                baseColor = mix(baseColor, baseColor * 1.3, (sphereR - (r - 0.12)) / 0.12);
            }
            float alpha = alphaPercent;
            vec3 ambientColor = baseColor * 0.5;
            vec3 diffuseColor = baseColor * (0.5 + 0.5 * spectrumValue);
            vec3 specularColor = mix(vec3(0.3), vec3(1.0), spectrumValue);
            float shininess = 6.0 - 4.0 * spectrumValue;
            vec3 illuminationColor = Shader_DolbyEffect_1_IlluminationColorSphere(ambientColor, diffuseColor, specularColor, shininess, pos, ro, N, t);
            col = vec4(illuminationColor, alpha);
        }
        else
        {
            vec4 centerLight = Shader_DolbyEffect_1_BorderLight();
            col = mix(col, centerLight, centerLight.a);
        }
    }
    return col;
}

vec3 Shader_DolbyEffect_1_IlluminationColorNote(vec3 k_a, vec3 k_d, vec3 k_s, float alpha, vec3 p, vec3 eye, vec3 N, float t)
{
    vec3 ambientLight = vec3(1.0, 1.0, 1.0);
    vec3 color = ambientLight * k_a;
    
    vec3 lightPosition1 = vec3(sin(t * 0.2), cos(t * 0.2), 0.9);
    vec3 lightPosition2 = vec3(sin(t * 0.2 + 2.7), cos(t * 0.2 + 2.7), 0.9);
    vec3 lightIntensity = vec3(1.0, 1.0, 1.0);
    vec3 V = normalize(eye - p);
    
    vec3 L1 = normalize(lightPosition1 - p);
    vec3 R1 = normalize(reflect(-L1, N));
    float NdotL1 = dot(L1, N);
    float diffuse1 = smoothstep(-0.2, 1.0, NdotL1);
    float specular1 = pow(max(dot(R1, V), 0.0), alpha) * smoothstep(0.0, 0.3, NdotL1);
    color += lightIntensity * (k_d * diffuse1 + k_s * specular1);
    
    vec3 L2 = normalize(lightPosition2 - p);
    vec3 R2 = normalize(reflect(-L2, N));
    float NdotL2 = dot(L2, N);
    float diffuse2 = smoothstep(-0.2, 1.0, NdotL2);
    float specular2 = pow(max(dot(R2, V), 0.0), alpha) * smoothstep(0.0, 0.3, NdotL2);
    color += lightIntensity * (k_d * diffuse2 + k_s * specular2);
    
    color = min(color, vec3(1.0));
    return color;
}

float Shader_DolbyEffect_1_SDNote(vec3 p)
{
    p.y = -p.y;
    
    float scale = 2.5;
    p *= scale;
    
    vec2 tmp_yz = Shader_DolbyEffect_1_pR(p.yz, -0.1);
    p.y = tmp_yz.x;
    p.z = tmp_yz.y;
    p.y -= 0.11;
    
    vec3 pa = p;
    vec3 ps = p;
    ps.x = sqrt(ps.x * ps.x + 0.0005);
    p.x = abs(p.x);
    vec3 pp = p;
    
    float d = 1e12;
    
    // skull back
    p = pp;
    p += vec3(0.0, -0.135, 0.09);
    d = Shader_DolbyEffect_1_Ellip(p, vec3(0.395, 0.385, 0.395));
    
    // skull base
    p = pp;
    p += vec3(0.0, -0.135, 0.09) + vec3(0.0, 0.1, 0.07);
    d = Shader_DolbyEffect_1_smin(d, Shader_DolbyEffect_1_Ellip(p, vec3(0.38, 0.36, 0.35)), 0.05);
    
    // forehead
    p = pp;
    p += vec3(0.0, -0.145, -0.175);
    d = Shader_DolbyEffect_1_smin(d, Shader_DolbyEffect_1_Ellip(p, vec3(0.315, 0.3, 0.33)), 0.18);
    
    p = pp;
    vec2 p_yz_bb = Shader_DolbyEffect_1_pR(p.yz, -0.5);
    p = vec3(p.x, p_yz_bb.x, p_yz_bb.y);
    float bb = Shader_DolbyEffect_1_fBox(p, vec3(0.5, 0.67, 0.7));
    d = Shader_DolbyEffect_1_smax(d, bb, 0.2);
    
    // face base
    p = pp;
    p += vec3(0.0, 0.25, -0.13);
    d = Shader_DolbyEffect_1_smin(d, length(p) - 0.28, 0.1);
    
    // behind ear
    p = ps;
    p += vec3(-0.15, 0.13, 0.06);
    d = Shader_DolbyEffect_1_smin(d, Shader_DolbyEffect_1_Ellip(p, vec3(0.15, 0.15, 0.15)), 0.15);
    
    p = ps;
    p += vec3(-0.07, 0.18, 0.1);
    d = Shader_DolbyEffect_1_smin(d, length(p) - 0.2, 0.18);
    
    // jaw base
    p = pp;
    p += vec3(0.0, 0.475, -0.16);
    vec2 p_jb_yz = Shader_DolbyEffect_1_pR(p.yz, 0.8);
    p = vec3(p.x, p_jb_yz.x, p_jb_yz.y);
    d = Shader_DolbyEffect_1_smin(d, Shader_DolbyEffect_1_Ellip(p, vec3(0.19, 0.1, 0.2)), 0.1);
    
    // brow
    p = pp;
    p += vec3(0.0, -0.0, -0.18);
    vec3 bp = p;
    float brow = length(p) - 0.36;
    p.x -= 0.37;
    brow = Shader_DolbyEffect_1_smax(brow, dot(p, normalize(vec3(1.0, 0.2, -0.2))), 0.2);
    p = bp;
    brow = Shader_DolbyEffect_1_smax(brow, dot(p, normalize(vec3(0.0, 0.6, 1.0))) - 0.33, 0.25);
    p = bp;
    vec2 bp_yz = Shader_DolbyEffect_1_pR(p.yz, -0.5);
    p = vec3(p.x, bp_yz.x, bp_yz.y);
    float peak = -p.y - 0.165;
    peak += smoothstep(0.0, 0.2, p.x) * 0.01;
    peak -= smoothstep(0.12, 0.29, p.x) * 0.025;
    brow = Shader_DolbyEffect_1_smax(brow, peak, 0.07);
    p = bp;
    vec2 bp_yz2 = Shader_DolbyEffect_1_pR(p.yz, 0.5);
    p = vec3(p.x, bp_yz2.x, bp_yz2.y);
    brow = Shader_DolbyEffect_1_smax(brow, -p.y - 0.18, 0.15);
    d = Shader_DolbyEffect_1_smin(d, brow, 0.06);
    
    // jaw
    vec3 jo = vec3(-0.25, 0.4, -0.07);
    p = ps + jo;
    float jaw = dot(p, normalize(vec3(1.0, -0.2, -0.05))) - 0.069;
    jaw = Shader_DolbyEffect_1_smax(jaw, dot(p, normalize(vec3(0.5, -0.25, 0.35))) - 0.13, 0.12);
    jaw = Shader_DolbyEffect_1_smax(jaw, dot(p, normalize(vec3(0.0, -1.0, -0.8))) - 0.12, 0.15);
    jaw = Shader_DolbyEffect_1_smax(jaw, dot(p, normalize(vec3(0.98, -1.0, 0.15))) - 0.13, 0.08);
    jaw = Shader_DolbyEffect_1_smax(jaw, dot(p, normalize(vec3(0.6, -0.2, -0.45))) - 0.24, 0.15);
    jaw = Shader_DolbyEffect_1_smax(jaw, dot(p, normalize(vec3(0.5, 0.1, -0.5))) - 0.32, 0.15);
    jaw = Shader_DolbyEffect_1_smax(jaw, dot(p, normalize(vec3(1.0, 0.2, -0.3))) - 0.28, 0.15);
    
    p = pp;
    p += vec3(0.0, 0.63, -0.2);
    vec2 p_jc_yz = Shader_DolbyEffect_1_pR(p.yz, 0.15);
    p = vec3(p.x, p_jc_yz.x, p_jc_yz.y);
    float cr = 0.5;
    jaw = Shader_DolbyEffect_1_smax(jaw, length(p.xy - vec2(0.0, cr)) - cr, 0.05);
    
    p = pp + jo;
    jaw = Shader_DolbyEffect_1_smax(jaw, dot(p, normalize(vec3(0.0, -0.4, 1.0))) - 0.42, 0.1);
    jaw = Shader_DolbyEffect_1_smax(jaw, dot(p, normalize(vec3(0.0, 1.5, 2.0))) - 0.38, 0.2);
    jaw = max(jaw, length(pp + vec3(0.0, 0.6, -0.3)) - 0.7);
    
    p = pa;
    p += vec3(0.2, 0.5, -0.1);
    float jb = length(p);
    jb = smoothstep(0.0, 0.4, jb);
    float js = mix(0.0, -0.005, jb);
    jb = mix(0.01, 0.04, jb);
    
    d = Shader_DolbyEffect_1_smin(d, jaw - js, jb);
    
    // chin
    p = pp;
    p += vec3(0.0, 0.55, -0.36);
    p.x *= 0.7;
    d = Shader_DolbyEffect_1_smin(d, Shader_DolbyEffect_1_Ellip(p, vec3(0.028, 0.028, 0.028) * 0.7), 0.15);
    
    // nose
    p = pp;
    p += vec3(0.0, 0.03, -0.45);
    vec2 p_n1_yz = Shader_DolbyEffect_1_pR(p.yz, 3.0);
    p = vec3(p.x, p_n1_yz.x, p_n1_yz.y);
    d = Shader_DolbyEffect_1_smin(d, Shader_DolbyEffect_1_sdRoundCone(p, 0.008, 0.05, 0.18), 0.1);
    
    p = pp;
    p += vec3(0.0, 0.06, -0.47);
    vec2 p_n2_yz = Shader_DolbyEffect_1_pR(p.yz, 2.77);
    p = vec3(p.x, p_n2_yz.x, p_n2_yz.y);
    d = Shader_DolbyEffect_1_smin(d, Shader_DolbyEffect_1_sdRoundCone(p, 0.005, 0.04, 0.225), 0.05);
    
    p = pp;
    p += vec3(-0.26, 0.02, -0.1);
    vec2 p_tp_xz = Shader_DolbyEffect_1_pR(p.xz, 0.13);
    p = vec3(p_tp_xz.x, p.y, p_tp_xz.y);
    vec2 p_tp_yz = Shader_DolbyEffect_1_pR(p.yz, 0.5);
    p = vec3(p.x, p_tp_yz.x, p_tp_yz.y);
    float temple = Shader_DolbyEffect_1_Ellip(p, vec3(0.1, 0.1, 0.15));
    temple = Shader_DolbyEffect_1_smax(temple, p.x - 0.07, 0.1);
    d = Shader_DolbyEffect_1_smin(d, temple, 0.1);
    
    // nostrils base
    p = pp;
    p += vec3(0.0, 0.3, -0.43);
    d = Shader_DolbyEffect_1_smin(d, length(p) - 0.05, 0.07);
    
    // nostrils
    p = pp;
    p += vec3(0.0, 0.27, -0.52);
    vec2 p_ns_yz = Shader_DolbyEffect_1_pR(p.yz, 0.2);
    p = vec3(p.x, p_ns_yz.x, p_ns_yz.y);
    float nostrils = Shader_DolbyEffect_1_Ellip(p, vec3(0.055, 0.05, 0.06));
    
    p = pp;
    p += vec3(-0.043, 0.28, -0.48);
    vec2 p_ns_xy = Shader_DolbyEffect_1_pR(p.xy, 0.15);
    p = vec3(p_ns_xy.x, p_ns_xy.y, p.z);
    p.z *= 0.8;
    nostrils = Shader_DolbyEffect_1_smin(nostrils, Shader_DolbyEffect_1_sdRoundCone(p, 0.042, 0.0, 0.12), 0.02);
    d = Shader_DolbyEffect_1_smin(d, nostrils, 0.02);
    
    // ear
    p = pp;
    p += vec3(-0.405, 0.12, 0.10);
    vec2 pe_xy = Shader_DolbyEffect_1_pR(p.xy, -0.12);
    p = vec3(pe_xy.x, pe_xy.y, p.z);
    vec2 pe_xz = Shader_DolbyEffect_1_pR(p.xz, 0.35);
    p = vec3(pe_xz.x, p.y, pe_xz.y);
    vec2 pe_yz = Shader_DolbyEffect_1_pR(p.yz, -0.3);
    p = vec3(p.x, pe_yz.x, pe_yz.y);
    vec3 pe = p;
    
    // base
    float ear = p.x + smoothstep(-0.05, 0.1, p.y) * 0.015 - 0.005;
    float earback = -ear - mix(0.001, 0.025, smoothstep(0.3, -0.2, p.y));
    
    p = pe;
    
    // outline
    vec2 peo_yz = Shader_DolbyEffect_1_pRi(p.yz, 0.2);
    float outline = Shader_DolbyEffect_1_Ellip2(peo_yz, vec2(0.12, 0.09));
    outline = Shader_DolbyEffect_1_smin(outline, Shader_DolbyEffect_1_Ellip2(p.yz + vec2(0.155, -0.02), vec2(0.035, 0.03)), 0.14);
    
    // edge
    float eedge = p.x + smoothstep(0.2, -0.4, p.y) * 0.06 - 0.03;
    
    vec2 ped_yz = Shader_DolbyEffect_1_pRi(p.yz, 0.1);
    float edgeo = Shader_DolbyEffect_1_Ellip2(ped_yz, vec2(0.095, 0.065));
    edgeo = Shader_DolbyEffect_1_smin(edgeo, length(vec2(p.z, p.y + 0.1)) - 0.03, 0.1);
    vec2 pei2_zy = Shader_DolbyEffect_1_pRi(vec2(p.z, p.y), 0.15);
    float edgeoin = Shader_DolbyEffect_1_smax(abs(pei2_zy.y + 0.035) - 0.01, -p.z - 0.01, 0.01);
    edgeo = Shader_DolbyEffect_1_smax(edgeo, -edgeoin, 0.05);
    
    float eedent = smoothstep(-0.05, 0.05, -p.z) * smoothstep(0.06, 0.0, Shader_DolbyEffect_1_fCorner2(vec2(-p.z, p.y)));
    eedent += smoothstep(0.1, -0.1, -p.z) * 0.2;
    eedent += smoothstep(0.1, -0.1, p.y) * smoothstep(-0.03, 0.0, p.z) * 0.3;
    eedent = min(eedent, 1.0);
    eedge += eedent * 0.06;
    
    eedge = Shader_DolbyEffect_1_smax(eedge, -edgeo, 0.01);
    ear = Shader_DolbyEffect_1_smin(ear, eedge, 0.01);
    ear = max(ear, earback);
    
    ear = Shader_DolbyEffect_1_smax2(ear, outline, 0.015);
    
    d = Shader_DolbyEffect_1_smin(d, ear, 0.015);
    
    p = pp;
    
    vec2 neck_yz = Shader_DolbyEffect_1_pR(p.yz, 0.1);
    p = vec3(p.x, neck_yz.x, neck_yz.y);
    p += vec3(0.0, 0.526, 0.10);
    float neck = Shader_DolbyEffect_1_RoundedCylinder(p, 0.28, 0.064, 0.274);
    d = Shader_DolbyEffect_1_smin(d, neck, 0.08);
    
    return d / scale;
}

vec3 Shader_DolbyEffect_1_NormalNote(vec3 p)
{
    return normalize(vec3(
                          Shader_DolbyEffect_1_SDNote(vec3(p.x + 0.0001, p.y, p.z)) - Shader_DolbyEffect_1_SDNote(vec3(p.x - 0.0001, p.y, p.z)),
                          Shader_DolbyEffect_1_SDNote(vec3(p.x, p.y + 0.0001, p.z)) - Shader_DolbyEffect_1_SDNote(vec3(p.x, p.y - 0.0001, p.z)),
                          Shader_DolbyEffect_1_SDNote(vec3(p.x, p.y, p.z + 0.0001)) - Shader_DolbyEffect_1_SDNote(vec3(p.x, p.y, p.z - 0.0001))
                          ));
}

float Shader_DolbyEffect_1_RaycastNote(vec3 ro, vec3 rd, float tmax, mat4 rotMatrix)
{
    float t = 0.0;
    for (int i = 0; i < 16; i++)
    {
        vec3 pos = ro + t * rd;
        vec3 rp = (rotMatrix * vec4(pos, 1.0)).xyz;
        float h = Shader_DolbyEffect_1_SDNote(rp);
        if (h < 0.0001 || t > tmax)
        {
            break;
        }
        t += h;
    }
    return (t < tmax) ? t : -1.0;
}

vec4 Shader_DolbyEffect_1_Note()
{
    vec4 col = vec4(0.0);
    vec2 p = dolbyCoordinate;
    if (length(p) < 0.2)
    {
        vec3 ro = vec3(0.0, 0.0, 1.8);
        vec3 rd = normalize(vec3(p.xy, -1.0));
        mat4 rotY180 = mat4(
                            -1.0, 0.0, 0.0, 0.0,
                            0.0, 1.0, 0.0, 0.0,
                            0.0, 0.0,-1.0, 0.0,
                            0.0, 0.0, 0.0, 1.0
                            );
        mat4 noteRotationMatrix = rotY180 * rotationMatrix;
        float d = Shader_DolbyEffect_1_RaycastNote(ro, rd, 2.0, noteRotationMatrix);
        if (d > 0.0)
        {
            vec3 pos = ro + d * rd;
            vec3 rpos = (noteRotationMatrix * vec4(pos, 1.0)).xyz;
            vec3 rN = Shader_DolbyEffect_1_NormalNote(rpos);
            mat4 noteRotationMatrixInverse = transposeMat4(noteRotationMatrix);
            vec3 N = normalize((noteRotationMatrixInverse * vec4(rN, 0.0)).xyz);
            vec3 ambientColor = color2 * 0.4;
            vec3 diffuseColor = (color1 * 0.9 + vec3(1.0)) * 0.4;
            vec3 specularColor = vec3(0.15);
            float shininess = 32.0;
            vec3 illuminationColor = Shader_DolbyEffect_1_IlluminationColorNote(ambientColor, diffuseColor, specularColor, shininess, pos, ro, N, time);
            col = vec4(illuminationColor, 1.0);
        }
    }
    return col;
}

vec4 Shader_DolbyEffect_1_TrackCol(vec2 p, vec4 tdata, int i)
{
    vec4 col = vec4(0.0);
    vec3 tp = tdata.xyz;
    tp.x = -tp.x;
    tp.y = -tp.y;
    float tb = tdata.w;
    if (length(tp) > 1.0)
    {
        tp /= length(tp);
    }
    tp *= 0.9;
    tp = (transposeMat4(rotationMatrix) * vec4(tp.xyz, 1.0)).xyz;
    tp.x = -tp.x;
    tp.y = -tp.y;
    if (length(p - tp.xy) < 0.08)
    {
        float cr = 0.8;
        float zp = (- tp.z / cr + 1.0) * 0.5;
        float r = (0.15 + 0.05 * zp) * tb;
        float d = length(p - tp.xy) / r;
        if (d < 1.0)
        {
            float rand = fract(sin(float(i) * 127.1) * 43758.5453);
            vec3 baseColor = mix(color2, color1, rand);
            vec3 centerColor = mix(baseColor * 1.5, vec3(1.0), Shader_DolbyEffect_1_Gain(tb, 2.0));
            vec3 c = mix(centerColor, baseColor, d);
            float a = Shader_DolbyEffect_1_Gain(1.0 - d, 1.5);
            a = ((d > 0.27) && (d < 0.3)) ? 0.9 : a;
            a = ((d > 0.42) && (d < 0.45)) ? 0.8 : a;
            a *= (zp < 0.5) ? 0.7 + 0.3 * (zp / 0.5) : 1.0;
            col = vec4(c, a);
        }
    }
    return col;
}

vec4 Shader_DolbyEffect_1_Track()
{
    vec4 col = vec4(0.0);
    vec2 p = dolbyCoordinate;
    for (int i = 0; i < trackCount; i++)
    {
        vec4 tdata = vec4(trackArray[i * 4 + 0], trackArray[i * 4 + 1], trackArray[i * 4 + 2], trackArray[i * 4 + 3]);
        vec4 tc = Shader_DolbyEffect_1_TrackCol(p, tdata, i);
        col = mix(col, tc, tc.a);
    }
    return col;
}

vec2 Shader_DolbyEffect_1_EllipseCoordinate(float start, float interval, float timePercent, float y)
{
    return vec2(start + interval * timePercent, y);
}

float Shader_DolbyEffect_1_EllipseAlpha(float alpha_min, float alpha_max, float noiseX)
{
    return alpha_min + (alpha_max - alpha_min) * (sin(noiseX) + 1.0) * 0.5;
}

float Shader_DolbyEffect_1_EllipseWidth(float ellipseWidth_min, float ellipseWidth_max, float noiseX)
{
    return ellipseWidth_min + (ellipseWidth_max - ellipseWidth_min) * (cos(noiseX) + 1.0) * 0.5;
}

float Shader_DolbyEffect_1_EllipseDepth_AB(vec2 ellipseCoordinate,
                                           vec2 fragCoordinate,
                                           float ar,
                                           float ellipseA,
                                           float ellipseB)
{
    ellipseA = ellipseA * 3.0;
    ellipseB = ellipseB * 2.0;
    float ellipseDistance = pow((ellipseCoordinate.x - fragCoordinate.x) * ar, 2.0) / pow(ellipseA, 2.0) + pow((ellipseCoordinate.y - fragCoordinate.y), 2.0) / pow(ellipseB, 2.0);
    ellipseDistance = clamp(ellipseDistance, 0.0, 1.0);
    float ellipseDepth = 0.5 * pow(2.0 * ((ellipseDistance < 0.5) ? ellipseDistance : (1.0 - ellipseDistance)), 1.5);
    ellipseDepth = (ellipseDistance < 0.5) ? (1.0 - ellipseDepth) : ellipseDepth;
    ellipseDepth = clamp(ellipseDepth, 0.0, 1.0);
    ellipseDepth = pow(ellipseDepth, 6.0);
    ellipseDepth = ellipseDepth * 0.8;
    return ellipseDepth;
}

vec2 Shader_DolbyEffect_1_ColorAndAlphaPercent(vec2 fragCoord,
                                               float ar,
                                               float t,
                                               float timePercent,
                                               float ellipseCoordinateY,
                                               float ellipseScaleY,
                                               float noiseInterval)
{
    vec2 ellipseCoordinate_1 = Shader_DolbyEffect_1_EllipseCoordinate(   1.1, - 2.565, timePercent, ellipseCoordinateY);
    vec2 ellipseCoordinate_2 = Shader_DolbyEffect_1_EllipseCoordinate(- 0.11,    0.65, timePercent, ellipseCoordinateY);
    vec2 ellipseCoordinate_3 = Shader_DolbyEffect_1_EllipseCoordinate(  0.35,  - 1.04, timePercent, ellipseCoordinateY);
    vec2 ellipseCoordinate_4 = Shader_DolbyEffect_1_EllipseCoordinate(- 0.83,    1.71, timePercent, ellipseCoordinateY);
    float ellipseAlpha_1 = Shader_DolbyEffect_1_EllipseAlpha(0.3, 0.7, t + 1.0 * noiseInterval);
    float ellipseAlpha_2 = Shader_DolbyEffect_1_EllipseAlpha(0.7, 0.9, t + 2.0 * noiseInterval);
    float ellipseAlpha_3 = Shader_DolbyEffect_1_EllipseAlpha(0.4, 0.6, t + 3.0 * noiseInterval);
    float ellipseAlpha_4 = Shader_DolbyEffect_1_EllipseAlpha(0.8, 1.0, t + 4.0 * noiseInterval);
    float ellipseWidthA_1 = Shader_DolbyEffect_1_EllipseWidth(0.24, 0.59, t +  5.0 * noiseInterval);
    float ellipseWidthB_1 = Shader_DolbyEffect_1_EllipseWidth(0.25, 0.87, t +  6.0 * noiseInterval);
    float ellipseWidthA_2 = Shader_DolbyEffect_1_EllipseWidth( 0.5, 0.63, t +  7.0 * noiseInterval);
    float ellipseWidthB_2 = Shader_DolbyEffect_1_EllipseWidth(0.36, 0.54, t +  8.0 * noiseInterval);
    float ellipseWidthA_3 = Shader_DolbyEffect_1_EllipseWidth(0.22, 0.57, t +  9.0 * noiseInterval);
    float ellipseWidthB_3 = Shader_DolbyEffect_1_EllipseWidth(0.35, 0.82, t + 10.0 * noiseInterval);
    float ellipseWidthA_4 = Shader_DolbyEffect_1_EllipseWidth(0.48, 0.51, t + 11.0 * noiseInterval);
    float ellipseWidthB_4 = Shader_DolbyEffect_1_EllipseWidth(0.09, 0.27, t + 12.0 * noiseInterval);
    float ellipseDepth_1 = Shader_DolbyEffect_1_EllipseDepth_AB(ellipseCoordinate_1, fragCoord, ar, ellipseWidthA_1, ellipseWidthB_1 * ellipseScaleY);
    float ellipseDepth_2 = Shader_DolbyEffect_1_EllipseDepth_AB(ellipseCoordinate_2, fragCoord, ar, ellipseWidthA_2, ellipseWidthB_2 * ellipseScaleY);
    float ellipseDepth_3 = Shader_DolbyEffect_1_EllipseDepth_AB(ellipseCoordinate_3, fragCoord, ar, ellipseWidthA_3, ellipseWidthB_3 * ellipseScaleY);
    float ellipseDepth_4 = Shader_DolbyEffect_1_EllipseDepth_AB(ellipseCoordinate_4, fragCoord, ar, ellipseWidthA_4, ellipseWidthB_4 * ellipseScaleY);
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

vec4 Shader_DolbyEffect_1_ColorEffect()
{
    vec2 fragCoordinate = (textureCoordinate * 2.0 - vec2(1.0, 1.0)) * vec2(1.0, - 1.0);
    float duraion = 14.0;
    float t = mod(time, 100.0) * 0.5;
    float timePercent_up = fract((mod(time, 100.0) / duraion) + 0.25);
    timePercent_up = (timePercent_up < 0.5) ? (2.0 * timePercent_up) : (1.0 - 2.0 * (timePercent_up - 0.5));
    float timePercent_down = fract(mod(time, 100.0) / duraion);
    timePercent_down = (timePercent_down < 0.5) ? (2.0 * timePercent_down) : (1.0 - 2.0 * (timePercent_down - 0.5));
    vec2 colorAndAlphaPercent_up = Shader_DolbyEffect_1_ColorAndAlphaPercent(fragCoordinate, aspectRatio, t, timePercent_up, 1.35, 1.0, 150.0);
    vec2 colorAndAlphaPercent_down = Shader_DolbyEffect_1_ColorAndAlphaPercent(fragCoordinate, aspectRatio, t, timePercent_down, - 1.1, 1.4, 100.0);
    float colorPercent = max(colorAndAlphaPercent_up.x, colorAndAlphaPercent_down.x);
    float alphaPercent = max(colorAndAlphaPercent_up.y, colorAndAlphaPercent_down.y);
    vec3 firstColor = color1;
    vec3 secondColor = color2;
    vec3 c = firstColor + (secondColor - firstColor) * colorPercent;
    return vec4(c, 1.0) * alphaPercent;
}

float Shader_DolbyEffect_1_Hash12(vec2 p)
{
    vec3 p3 = fract(vec3(p.x, p.y, p.x) * 0.1031);
    p3 += dot(p3, vec3(p3.y, p3.z, p3.x) + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 Shader_DolbyEffect_1_Hash22(vec2 p)
{
    vec3 p3 = fract(vec3(p.x, p.y, p.x) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, vec3(p3.y, p3.z, p3.x) + 33.33);
    return fract((vec2(p3.x, p3.y) + vec2(p3.y, p3.z)) * vec2(p3.z, p3.x));
}

float Shader_DolbyEffect_1_SnowLayer(vec2 uv, float scale, float t)
{
    uv *= scale;
    vec2 s = floor(uv);
    vec2 f = fract(uv);
    vec2 velocity = Shader_DolbyEffect_1_Hash22(s * 17.31 + scale * 43.17) * 2.0 - 1.0;
    float speed = Shader_DolbyEffect_1_Hash12(s * 31.71 + scale * 57.13) * 2.0 + 0.5;
    uv += velocity * t * speed;
    s = floor(uv);
    f = fract(uv);
    float h = Shader_DolbyEffect_1_Hash12(s + 65.31 * scale * s);
    vec2 p = vec2(h) - f;
    return smoothstep(0.0, length(p), 0.01) * smoothstep(0.5, 0.0, length(f - vec2(0.5)));
}

vec4 Shader_DolbyEffect_1_Snow()
{
    vec4 col = vec4(0.0);
    vec2 p = dolbyCoordinate;
    if (length(p) > 0.55)
    {
        float t = mod(time, 100.0) * 0.5 + mod(increaseSpectrumValue, 100.0) * 13.0;
        float c = 0.0;
        c += Shader_DolbyEffect_1_SnowLayer(p, 8.0, t);
        c += Shader_DolbyEffect_1_SnowLayer(p, 10.0, t);
        c += Shader_DolbyEffect_1_SnowLayer(p, 12.0, t);
        c += Shader_DolbyEffect_1_SnowLayer(p, 14.0, t);
        c += Shader_DolbyEffect_1_SnowLayer(p, 16.0, t);
        c += Shader_DolbyEffect_1_SnowLayer(p, 18.0, t);
        float d = length(p);
        float mask = smoothstep(0.55, 0.7, d) * smoothstep(1.0, 0.85, d);
        col = vec4((color2 + vec3(1.0)) * 0.5, c * mask);
    }
    return col;
}

void main()
{
    dolbyCoordinate = Shader_DolbyEffect_1_DolbyCoordinate();
    
    vec4 color = vec4(0.1, 0.1, 0.16, 1.0);
    vec4 colorEffect = Shader_DolbyEffect_1_ColorEffect();
    color = mix(color, colorEffect, colorEffect.a);
    if (dolbyCoordinate.x > -0.999 && dolbyCoordinate.x < 0.999 &&
        dolbyCoordinate.y > -0.999 && dolbyCoordinate.y < 0.999)
    {
        vec4 note = Shader_DolbyEffect_1_Note();
        vec4 sphere = Shader_DolbyEffect_1_Sphere();
        vec4 snow = Shader_DolbyEffect_1_Snow();
        vec4 track = Shader_DolbyEffect_1_Track();
        color = mix(color, note, note.a);
        color = Shader_DolbyEffect_1_Add(color, sphere);
        color = Shader_DolbyEffect_1_Add(color, snow);
        color = Shader_DolbyEffect_1_Add(color, track);
    }
    color = vec4(color.rgb, 1.0);
    gl_FragColor = color;
}
