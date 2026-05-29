/**
 * 地址层级数据 - 星球/国家/省份/城市/区县
 */

// 星球类型
export interface Planet {
  code: string;
  name: string;
}

// 国家类型
export interface Country {
  code: string;
  name: string;
  planetCode: string;
}

// 省份类型
export interface Province {
  code: string;
  name: string;
  countryCode: string;
}

// 城市类型
export interface City {
  code: string;
  name: string;
  provinceCode: string;
}

// 区县类型
export interface District {
  code: string;
  name: string;
  cityCode: string;
}

// 星球数据
export const planets: Planet[] = [
  { code: 'earth', name: '地球' },
  { code: 'mars', name: '火星' },
  { code: 'moon', name: '月球' },
];

// 国家数据（以地球为主）
export const countries: Country[] = [
  // 地球 - 中国
  { code: 'CN', name: '中国', planetCode: 'earth' },
  { code: 'US', name: '美国', planetCode: 'earth' },
  { code: 'JP', name: '日本', planetCode: 'earth' },
  { code: 'KR', name: '韩国', planetCode: 'earth' },
  { code: 'GB', name: '英国', planetCode: 'earth' },
  { code: 'DE', name: '德国', planetCode: 'earth' },
  { code: 'FR', name: '法国', planetCode: 'earth' },
  { code: 'AU', name: '澳大利亚', planetCode: 'earth' },
  { code: 'CA', name: '加拿大', planetCode: 'earth' },
  { code: 'SG', name: '新加坡', planetCode: 'earth' },
  // 地球 - 其他国家
  { code: 'HK', name: '香港', planetCode: 'earth' },
  { code: 'TW', name: '台湾', planetCode: 'earth' },
  { code: 'MO', name: '澳门', planetCode: 'earth' },
];

// 省份数据
export const provinces: Province[] = [
  // 中国省份
  { code: 'BJ', name: '北京市', countryCode: 'CN' },
  { code: 'TJ', name: '天津市', countryCode: 'CN' },
  { code: 'HE', name: '河北省', countryCode: 'CN' },
  { code: 'SX', name: '山西省', countryCode: 'CN' },
  { code: 'NM', name: '内蒙古', countryCode: 'CN' },
  { code: 'LN', name: '辽宁省', countryCode: 'CN' },
  { code: 'JL', name: '吉林省', countryCode: 'CN' },
  { code: 'HL', name: '黑龙江省', countryCode: 'CN' },
  { code: 'SH', name: '上海市', countryCode: 'CN' },
  { code: 'JS', name: '江苏省', countryCode: 'CN' },
  { code: 'ZJ', name: '浙江省', countryCode: 'CN' },
  { code: 'AH', name: '安徽省', countryCode: 'CN' },
  { code: 'FJ', name: '福建省', countryCode: 'CN' },
  { code: 'JX', name: '江西省', countryCode: 'CN' },
  { code: 'SD', name: '山东省', countryCode: 'CN' },
  { code: 'HA', name: '河南省', countryCode: 'CN' },
  { code: 'HB', name: '湖北省', countryCode: 'CN' },
  { code: 'HN', name: '湖南省', countryCode: 'CN' },
  { code: 'GD', name: '广东省', countryCode: 'CN' },
  { code: 'GX', name: '广西', countryCode: 'CN' },
  { code: 'HI', name: '海南省', countryCode: 'CN' },
  { code: 'CQ', name: '重庆市', countryCode: 'CN' },
  { code: 'SC', name: '四川省', countryCode: 'CN' },
  { code: 'GZ', name: '贵州省', countryCode: 'CN' },
  { code: 'YN', name: '云南省', countryCode: 'CN' },
  { code: 'XZ', name: '西藏', countryCode: 'CN' },
  { code: 'SN', name: '陕西省', countryCode: 'CN' },
  { code: 'GS', name: '甘肃省', countryCode: 'CN' },
  { code: 'QH', name: '青海省', countryCode: 'CN' },
  { code: 'NX', name: '宁夏', countryCode: 'CN' },
  { code: 'XJ', name: '新疆', countryCode: 'CN' },
  { code: 'TW_PROV', name: '台湾省', countryCode: 'TW' },
  // 美国州
  { code: 'CA_US', name: '加利福尼亚州', countryCode: 'US' },
  { code: 'NY', name: '纽约州', countryCode: 'US' },
  { code: 'TX', name: '德克萨斯州', countryCode: 'US' },
  { code: 'FL', name: '佛罗里达州', countryCode: 'US' },
  // 日本
  { code: 'TK', name: '东京都', countryCode: 'JP' },
  { code: 'OS', name: '大阪府', countryCode: 'JP' },
  { code: 'KT', name: '神奈川县', countryCode: 'JP' },
  // 韩国
  { code: 'SG', name: '首尔', countryCode: 'KR' },
  { code: 'PG', name: '釜山', countryCode: 'KR' },
];

// 城市数据
export const cities: City[] = [
  // 北京
  { code: 'BJ_CITY', name: '北京市', provinceCode: 'BJ' },
  // 天津
  { code: 'TJ_CITY', name: '天津市', provinceCode: 'TJ' },
  // 河北
  { code: 'SJZ', name: '石家庄市', provinceCode: 'HE' },
  { code: 'TS', name: '唐山市', provinceCode: 'HE' },
  { code: 'BD', name: '保定市', provinceCode: 'HE' },
  // 山西
  { code: 'TY', name: '太原市', provinceCode: 'SX' },
  { code: 'DT', name: '大同市', provinceCode: 'SX' },
  // 内蒙古
  { code: 'HHHT', name: '呼和浩特市', provinceCode: 'NM' },
  { code: 'BY', name: '包头市', provinceCode: 'NM' },
  // 辽宁
  { code: 'SY', name: '沈阳市', provinceCode: 'LN' },
  { code: 'DL', name: '大连市', provinceCode: 'LN' },
  { code: 'AS', name: '鞍山市', provinceCode: 'LN' },
  // 吉林
  { code: 'CC', name: '长春市', provinceCode: 'JL' },
  { code: 'JL_CITY', name: '吉林市', provinceCode: 'JL' },
  // 黑龙江
  { code: 'HRB', name: '哈尔滨市', provinceCode: 'HL' },
  { code: 'JQ', name: '齐齐哈尔市', provinceCode: 'HL' },
  // 上海
  { code: 'SH_CITY', name: '上海市', provinceCode: 'SH' },
  // 江苏
  { code: 'NJ', name: '南京市', provinceCode: 'JS' },
  { code: 'WX', name: '无锡市', provinceCode: 'JS' },
  { code: 'SZ', name: '苏州市', provinceCode: 'JS' },
  { code: 'XZ', name: '徐州市', provinceCode: 'JS' },
  { code: 'CZ', name: '常州市', provinceCode: 'JS' },
  { code: 'NT', name: '南通市', provinceCode: 'JS' },
  // 浙江
  { code: 'HZ', name: '杭州市', provinceCode: 'ZJ' },
  { code: 'NB', name: '宁波市', provinceCode: 'ZJ' },
  { code: 'WZ', name: '温州市', provinceCode: 'ZJ' },
  { code: 'JX', name: '嘉兴市', provinceCode: 'ZJ' },
  { code: 'SX', name: '绍兴市', provinceCode: 'ZJ' },
  { code: 'JH', name: '金华市', provinceCode: 'ZJ' },
  // 安徽
  { code: 'HF', name: '合肥市', provinceCode: 'AH' },
  { code: 'WH', name: '芜湖市', provinceCode: 'AH' },
  { code: 'BB', name: '蚌埠市', provinceCode: 'AH' },
  // 福建
  { code: 'FZ', name: '福州市', provinceCode: 'FJ' },
  { code: 'XM', name: '厦门市', provinceCode: 'FJ' },
  { code: 'QZ', name: '泉州市', provinceCode: 'FJ' },
  // 江西
  { code: 'NC', name: '南昌市', provinceCode: 'JX' },
  { code: 'JDZ', name: '景德镇市', provinceCode: 'JX' },
  // 山东
  { code: 'JN', name: '济南市', provinceCode: 'SD' },
  { code: 'QD', name: '青岛市', provinceCode: 'SD' },
  { code: 'WH', name: '威海市', provinceCode: 'SD' },
  { code: 'ZB', name: '淄博市', provinceCode: 'SD' },
  // 河南
  { code: 'ZZ', name: '郑州市', provinceCode: 'HA' },
  { code: 'LY', name: '洛阳市', provinceCode: 'HA' },
  { code: 'KF', name: '开封市', provinceCode: 'HA' },
  // 湖北
  { code: 'WH_HB', name: '武汉市', provinceCode: 'HB' },
  { code: 'YC', name: '宜昌市', provinceCode: 'HB' },
  { code: 'XY', name: '襄阳市', provinceCode: 'HB' },
  // 湖南
  { code: 'CS', name: '长沙市', provinceCode: 'HN' },
  { code: 'ZZ_HN', name: '株洲市', provinceCode: 'HN' },
  { code: 'YY', name: '岳阳市', provinceCode: 'HN' },
  // 广东
  { code: 'GZ', name: '广州市', provinceCode: 'GD' },
  { code: 'SZ', name: '深圳市', provinceCode: 'GD' },
  { code: 'DG', name: '东莞市', provinceCode: 'GD' },
  { code: 'FS', name: '佛山市', provinceCode: 'GD' },
  { code: 'ZH', name: '珠海市', provinceCode: 'GD' },
  { code: 'ST', name: '汕头市', provinceCode: 'GD' },
  { code: 'FS_CN', name: '佛山市', provinceCode: 'GD' },
  // 广西
  { code: 'NN', name: '南宁市', provinceCode: 'GX' },
  { code: 'GL', name: '桂林市', provinceCode: 'GX' },
  { code: 'LZ', name: '柳州市', provinceCode: 'GX' },
  // 海南
  { code: 'HK_CITY', name: '海口市', provinceCode: 'HI' },
  { code: 'SM', name: '三亚市', provinceCode: 'HI' },
  // 重庆
  { code: 'CQ_CITY', name: '重庆市', provinceCode: 'CQ' },
  // 四川
  { code: 'CD', name: '成都市', provinceCode: 'SC' },
  { code: 'MY', name: '绵阳市', provinceCode: 'SC' },
  { code: 'DY', name: '德阳市', provinceCode: 'SC' },
  // 贵州
  { code: 'GY', name: '贵阳市', provinceCode: 'GZ' },
  { code: 'LZ_GZ', name: '六盘水市', provinceCode: 'GZ' },
  // 云南
  { code: 'KM', name: '昆明市', provinceCode: 'YN' },
  { code: 'DL', name: '大理市', provinceCode: 'YN' },
  // 西藏
  { code: 'LS', name: '拉萨市', provinceCode: 'XZ' },
  // 陕西
  { code: 'XA', name: '西安市', provinceCode: 'SN' },
  { code: 'XY', name: '咸阳', provinceCode: 'SN' },
  // 甘肃
  { code: 'LX', name: '兰州市', provinceCode: 'GS' },
  { code: 'JW', name: '嘉峪关', provinceCode: 'GS' },
  // 青海
  { code: 'XN', name: '西宁市', provinceCode: 'QH' },
  // 宁夏
  { code: 'YC_NX', name: '银川市', provinceCode: 'NX' },
  // 新疆
  { code: 'UR', name: '乌鲁木齐市', provinceCode: 'XJ' },
  { code: 'KS', name: '喀什市', provinceCode: 'XJ' },
  // 香港
  { code: 'HK_REGION', name: '香港岛', provinceCode: 'HK' },
  { code: 'KL', name: '九龙', provinceCode: 'HK' },
  { code: 'NT_HK', name: '新界', provinceCode: 'HK' },
  // 台湾
  { code: 'TPE', name: '台北市', provinceCode: 'TW_PROV' },
  { code: 'TCH', name: '桃园市', provinceCode: 'TW_PROV' },
  { code: 'KHH', name: '高雄市', provinceCode: 'TW_PROV' },
  // 美国城市
  { code: 'LA', name: '洛杉矶', provinceCode: 'CA_US' },
  { code: 'SF', name: '旧金山', provinceCode: 'CA_US' },
  { code: 'SD_US', name: '圣地亚哥', provinceCode: 'CA_US' },
  { code: 'NYC', name: '纽约市', provinceCode: 'NY' },
  { code: 'BUF', name: '布法罗', provinceCode: 'NY' },
  { code: 'HST', name: '休斯顿', provinceCode: 'TX' },
  { code: 'DAL', name: '达拉斯', provinceCode: 'TX' },
  // 日本城市
  { code: 'TKY', name: '东京', provinceCode: 'TK' },
  { code: 'YKH', name: '横滨', provinceCode: 'KT' },
  { code: 'OSA', name: '大阪', provinceCode: 'OS' },
  // 韩国城市
  { code: 'SEL', name: '首尔特别市', provinceCode: 'SG' },
  { code: 'PUS', name: '釜山广域市', provinceCode: 'PG' },
];

// 区县数据
export const districts: District[] = [
  // 北京
  { code: 'CY', name: '朝阳区', cityCode: 'BJ_CITY' },
  { code: 'HD', name: '海淀区', cityCode: 'BJ_CITY' },
  { code: 'XC', name: '西城区', cityCode: 'BJ_CITY' },
  { code: 'DC', name: '东城区', cityCode: 'BJ_CITY' },
  { code: 'CW', name: '城六区', cityCode: 'BJ_CITY' },
  { code: 'FS', name: '房山区', cityCode: 'BJ_CITY' },
  { code: 'SY', name: '顺义区', cityCode: 'BJ_CITY' },
  { code: 'TH', name: '通州区', cityCode: 'BJ_CITY' },
  { code: 'DX', name: '大兴区', cityCode: 'BJ_CITY' },
  { code: 'MT', name: '门头沟区', cityCode: 'BJ_CITY' },
  // 上海
  { code: 'HP', name: '黄浦区', cityCode: 'SH_CITY' },
  { code: 'PD', name: '浦东新区', cityCode: 'SH_CITY' },
  { code: 'XH', name: '徐汇区', cityCode: 'SH_CITY' },
  { code: 'HK', name: '虹口区', cityCode: 'SH_CITY' },
  { code: 'MD', name: '静安区', cityCode: 'SH_CITY' },
  { code: 'PT', name: '普陀区', cityCode: 'SH_CITY' },
  { code: 'LC', name: '杨浦区', cityCode: 'SH_CITY' },
  { code: 'MH', name: '闵行区', cityCode: 'SH_CITY' },
  { code: 'BS', name: '宝山区', cityCode: 'SH_CITY' },
  { code: 'SJ', name: '松江区', cityCode: 'SH_CITY' },
  { code: 'JV', name: '嘉定区', cityCode: 'SH_CITY' },
  { code: 'QP', name: '青浦区', cityCode: 'SH_CITY' },
  // 广东 - 广州
  { code: 'TH_GZ', name: '天河区', cityCode: 'GZ' },
  { code: 'ZS', name: '增城区', cityCode: 'GZ' },
  { code: 'BY', name: '白云区', cityCode: 'GZ' },
  { code: 'HL', name: '黄埔区', cityCode: 'GZ' },
  { code: 'PY', name: '番禺区', cityCode: 'GZ' },
  { code: 'YW', name: '越秀区', cityCode: 'GZ' },
  { code: 'LW', name: '荔湾区', cityCode: 'GZ' },
  { code: 'HC', name: '海珠区', cityCode: 'GZ' },
  { code: 'NS', name: '南沙区', cityCode: 'GZ' },
  { code: 'CX', name: '从化区', cityCode: 'GZ' },
  // 广东 - 深圳
  { code: 'NS_SZ', name: '南山区', cityCode: 'SZ' },
  { code: 'FG', name: '福田区', cityCode: 'SZ' },
  { code: 'LW_SZ', name: '龙华区', cityCode: 'SZ' },
  { code: 'BA', name: '宝安区', cityCode: 'SZ' },
  { code: 'LG', name: '龙岗区', cityCode: 'SZ' },
  { code: 'YL', name: '盐田区', cityCode: 'SZ' },
  // 浙江 - 杭州
  { code: 'SC', name: '上城区', cityCode: 'HZ' },
  { code: 'XC_ZJ', name: '下城区', cityCode: 'HZ' },
  { code: 'JW', name: '江干区', cityCode: 'HZ' },
  { code: 'XC_HZ', name: '西湖区', cityCode: 'HZ' },
  { code: 'GongShu', name: '拱墅区', cityCode: 'HZ' },
  { code: 'BH', name: '滨江区', cityCode: 'HZ' },
  { code: 'XC_HZ2', name: '萧山区', cityCode: 'HZ' },
  { code: 'YH', name: '余杭区', cityCode: 'HZ' },
  { code: 'FTH', name: '富阳区', cityCode: 'HZ' },
  // 江苏 - 南京
  { code: 'XW', name: '玄武区', cityCode: 'NJ' },
  { code: 'QL', name: '秦淮区', cityCode: 'NJ' },
  { code: 'JY', name: '建邺区', cityCode: 'NJ' },
  { code: 'GZ', name: '鼓楼区', cityCode: 'NJ' },
  { code: 'YX', name: '雨花台区', cityCode: 'NJ' },
  { code: 'PJ', name: '浦口区', cityCode: 'NJ' },
  { code: 'XQ', name: '栖霞区', cityCode: 'NJ' },
  { code: 'JN', name: '江宁区', cityCode: 'NJ' },
  // 四川 - 成都
  { code: 'JQ', name: '锦江区', cityCode: 'CD' },
  { code: 'WH', name: '武侯区', cityCode: 'CD' },
  { code: 'QN', name: '青羊区', cityCode: 'CD' },
  { code: 'CH', name: '成华区', cityCode: 'CD' },
  { code: 'JS', name: '金堂县', cityCode: 'CD' },
  { code: 'SL', name: '双流区', cityCode: 'CD' },
  { code: 'DW', name: '大邑县', cityCode: 'CD' },
  { code: 'PS', name: '蒲江县', cityCode: 'CD' },
  { code: 'XJ', name: '新津区', cityCode: 'CD' },
  // 湖北 - 武汉
  { code: 'JY_WH', name: '江岸区', cityCode: 'WH_HB' },
  { code: 'JW_WH', name: '江汉区', cityCode: 'WH_HB' },
  { code: 'YQ', name: '硚口区', cityCode: 'WH_HB' },
  { code: 'HBY', name: '汉阳区', cityCode: 'WH_HB' },
  { code: 'WCH', name: '武昌区', cityCode: 'WH_HB' },
  { code: 'QSK', name: '青山区', cityCode: 'WH_HB' },
  { code: 'HXS', name: '洪山区', cityCode: 'WH_HB' },
  { code: 'DNH', name: '东湖高新区', cityCode: 'WH_HB' },
  // 陕西 - 西安
  { code: 'BC', name: '碑林区', cityCode: 'XA' },
  { code: 'YT', name: '雁塔区', cityCode: 'XA' },
  { code: 'YL', name: '阎良区', cityCode: 'XA' },
  { code: 'LN', name: '临潼区', cityCode: 'XA' },
  { code: 'WE', name: '未央区', cityCode: 'XA' },
  { code: 'HT', name: '灞桥区', cityCode: 'XA' },
  // 湖南 - 长沙
  { code: 'TY', name: '天心区', cityCode: 'CS' },
  { code: 'YH_CS', name: '岳麓区', cityCode: 'CS' },
  { code: 'FY', name: '芙蓉区', cityCode: 'CS' },
  { code: 'YX', name: '雨花区', cityCode: 'CS' },
  { code: 'KS', name: '开福区', cityCode: 'CS' },
  { code: 'XN', name: '长沙县', cityCode: 'CS' },
];

// 根据星球获取国家列表
export function getCountriesByPlanet(planetCode: string): Country[] {
  return countries.filter(c => c.planetCode === planetCode);
}

// 根据国家获取省份列表
export function getProvincesByCountry(countryCode: string): Province[] {
  return provinces.filter(p => p.countryCode === countryCode);
}

// 根据省份获取城市列表
export function getCitiesByProvince(provinceCode: string): City[] {
  return cities.filter(c => c.provinceCode === provinceCode);
}

// 根据城市获取区县列表
export function getDistrictsByCity(cityCode: string): District[] {
  return districts.filter(d => d.cityCode === cityCode);
}

// 根据code获取星球名称
export function getPlanetName(code: string): string {
  return planets.find(p => p.code === code)?.name || '';
}

// 根据code获取国家名称
export function getCountryName(code: string): string {
  return countries.find(c => c.code === code)?.name || '';
}

// 根据code获取省份名称
export function getProvinceName(code: string): string {
  return provinces.find(p => p.code === code)?.name || '';
}

// 根据code获取城市名称
export function getCityName(code: string): string {
  return cities.find(c => c.code === code)?.name || '';
}

// 根据code获取区县名称
export function getDistrictName(code: string): string {
  return districts.find(d => d.code === code)?.name || '';
}

// 格式化完整地址
export function formatFullAddress(
  planetCode: string,
  countryCode: string,
  provinceCode: string,
  cityCode: string,
  districtCode: string
): string {
  const parts = [
    getPlanetName(planetCode),
    getCountryName(countryCode),
    getProvinceName(provinceCode),
    getCityName(cityCode),
    getDistrictName(districtCode),
  ].filter(Boolean);
  return parts.join('');
}
