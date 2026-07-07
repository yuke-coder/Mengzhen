'use client';

import { useState, useEffect } from 'react';
import { ChevronDown, MapPin } from 'lucide-react';
import {
  planets,
  countries,
  provinces,
  cities,
  districts,
  getCountriesByPlanet,
  getProvincesByCountry,
  getCitiesByProvince,
  getDistrictsByCity,
  Country,
  Province,
  City,
  District,
} from '@/lib/location-data';

interface LocationSelectorProps {
  value: {
    planet?: string;
    country?: string;
    province?: string;
    city?: string;
    district?: string;
  };
  onChange: (value: {
    planet?: string;
    country?: string;
    province?: string;
    city?: string;
    district?: string;
  }) => void;
  disabled?: boolean;
}

export function LocationSelector({ value, onChange, disabled }: LocationSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  
  // 筛选后的选项
  const [countryOptions, setCountryOptions] = useState<Country[]>([]);
  const [provinceOptions, setProvinceOptions] = useState<Province[]>([]);
  const [cityOptions, setCityOptions] = useState<City[]>([]);
  const [districtOptions, setDistrictOptions] = useState<District[]>([]);

  // 初始化：当星球改变时，重置并更新国家选项
  useEffect(() => {
    if (value.planet) {
      // 如果是地球，加载国家选项；否则清空后续选项
      if (value.planet === 'earth') {
        const newCountries = getCountriesByPlanet(value.planet);
        setCountryOptions(newCountries);
        
        // 如果当前国家不在新列表中，重置
        if (value.country && !newCountries.find(c => c.code === value.country)) {
          onChange({ ...value, country: '', province: '', city: '', district: '' });
        }
      } else {
        // 非地球星球：清空所有后续选项
        setCountryOptions([]);
        onChange({ planet: value.planet, country: '', province: '', city: '', district: '' });
      }
    }
  }, [value.planet]);

  // 当国家改变时，更新省份选项
  useEffect(() => {
    if (value.country) {
      const newProvinces = getProvincesByCountry(value.country);
      setProvinceOptions(newProvinces);
      
      if (value.province && !newProvinces.find(p => p.code === value.province)) {
        onChange({ ...value, province: '', city: '', district: '' });
      }
    } else {
      setProvinceOptions([]);
      setCityOptions([]);
      setDistrictOptions([]);
    }
  }, [value.country]);

  // 当省份改变时，更新城市选项
  useEffect(() => {
    if (value.province) {
      const newCities = getCitiesByProvince(value.province);
      setCityOptions(newCities);
      
      if (value.city && !newCities.find(c => c.code === value.city)) {
        onChange({ ...value, city: '', district: '' });
      }
    } else {
      setCityOptions([]);
      setDistrictOptions([]);
    }
  }, [value.province]);

  // 当城市改变时，更新区县选项
  useEffect(() => {
    if (value.city) {
      const newDistricts = getDistrictsByCity(value.city);
      setDistrictOptions(newDistricts);
      
      if (value.district && !newDistricts.find(d => d.code === value.district)) {
        onChange({ ...value, district: '' });
      }
    } else {
      setDistrictOptions([]);
    }
  }, [value.city]);

  const handleChange = (field: 'planet' | 'country' | 'province' | 'city' | 'district', code: string) => {
    // 根据改变的层级，重置后续层级
    switch (field) {
      case 'planet':
        onChange({ planet: code, country: '', province: '', city: '', district: '' });
        break;
      case 'country':
        onChange({ ...value, country: code, province: '', city: '', district: '' });
        break;
      case 'province':
        onChange({ ...value, province: code, city: '', district: '' });
        break;
      case 'city':
        onChange({ ...value, city: code, district: '' });
        break;
      case 'district':
        onChange({ ...value, district: code });
        break;
    }
  };

  // 获取当前选中的显示文本
  const getDisplayText = () => {
    const parts: string[] = [];
    if (value.planet) {
      const planet = planets.find(p => p.code === value.planet);
      if (planet) parts.push(planet.name);
    }
    if (value.country) {
      const country = countries.find(c => c.code === value.country);
      if (country) parts.push(country.name);
    }
    if (value.province) {
      const province = provinces.find(p => p.code === value.province);
      if (province) parts.push(province.name);
    }
    if (value.city) {
      const city = cities.find(c => c.code === value.city);
      if (city) parts.push(city.name);
    }
    if (value.district) {
      const district = districts.find(d => d.code === value.district);
      if (district) parts.push(district.name);
    }
    return parts.length > 0 ? parts.join(' / ') : '请选择所在地';
  };

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        className={`
          w-full px-4 py-3 text-left bg-muted border border-border rounded-lg
          flex items-center justify-between gap-2
          transition-all duration-200
          ${disabled ? 'opacity-50 cursor-not-allowed' : 'hover:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/30'}
        `}
      >
        <div className="flex items-center gap-2 flex-1 min-w-0">
          <MapPin className="w-4 h-4 text-muted-foreground flex-shrink-0" />
          <span className={`truncate ${value.planet ? 'text-foreground' : 'text-muted-foreground'}`}>
            {getDisplayText()}
          </span>
        </div>
        <ChevronDown className={`w-4 h-4 text-muted-foreground transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {isOpen && (
        <>
          <div 
            className="fixed inset-0 z-40" 
            onClick={() => setIsOpen(false)} 
          />
          <div className="absolute top-full left-0 right-0 mt-2 bg-card border border-border rounded-lg shadow-lg z-50 p-4 max-h-96 overflow-y-auto">
            <div className="space-y-4">
              {/* 星球选择 */}
              <div>
                <label className="block text-xs text-muted-foreground mb-2 font-medium">
                  1. 选择星球
                </label>
                <select
                  value={value.planet || ''}
                  onChange={(e) => handleChange('planet', e.target.value)}
                  className="w-full px-3 py-2 bg-background border border-border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                >
                  <option value="">请选择星球</option>
                  {planets.map((planet) => (
                    <option key={planet.code} value={planet.code}>
                      {planet.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* 国家选择 - 仅地球显示 */}
              {value.planet === 'earth' && (
                <div>
                  <label className="block text-xs text-muted-foreground mb-2 font-medium">
                    2. 选择国家/地区
                  </label>
                  <select
                    value={value.country || ''}
                    onChange={(e) => handleChange('country', e.target.value)}
                    className="w-full px-3 py-2 bg-background border border-border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                  >
                    <option value="">请选择国家/地区</option>
                    {countryOptions.map((country) => (
                      <option key={country.code} value={country.code}>
                        {country.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* 非地球星球提示 */}
              {value.planet && value.planet !== 'earth' && (
                <div className="text-xs text-muted-foreground text-center py-2">
                  其他星球暂不支持详细地址选择
                </div>
              )}

              {/* 省份选择 */}
              {value.country && provinceOptions.length > 0 && (
                <div>
                  <label className="block text-xs text-muted-foreground mb-2 font-medium">
                    3. 选择省级行政区
                  </label>
                  <select
                    value={value.province || ''}
                    onChange={(e) => handleChange('province', e.target.value)}
                    className="w-full px-3 py-2 bg-background border border-border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                  >
                    <option value="">请选择省份/州</option>
                    {provinceOptions.map((province) => (
                      <option key={province.code} value={province.code}>
                        {province.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* 城市选择 */}
              {value.province && cityOptions.length > 0 && (
                <div>
                  <label className="block text-xs text-muted-foreground mb-2 font-medium">
                    4. 选择市级行政区
                  </label>
                  <select
                    value={value.city || ''}
                    onChange={(e) => handleChange('city', e.target.value)}
                    className="w-full px-3 py-2 bg-background border border-border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                  >
                    <option value="">请选择城市</option>
                    {cityOptions.map((city) => (
                      <option key={city.code} value={city.code}>
                        {city.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* 区县选择 */}
              {value.city && districtOptions.length > 0 && (
                <div>
                  <label className="block text-xs text-muted-foreground mb-2 font-medium">
                    5. 选择区级行政区
                  </label>
                  <select
                    value={value.district || ''}
                    onChange={(e) => handleChange('district', e.target.value)}
                    className="w-full px-3 py-2 bg-background border border-border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                  >
                    <option value="">请选择区县</option>
                    {districtOptions.map((district) => (
                      <option key={district.code} value={district.code}>
                        {district.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>

            {/* 已选地址预览 */}
            {value.planet && (
              <div className="mt-4 pt-4 border-t border-border">
                <div className="text-xs text-muted-foreground mb-1">已选地址</div>
                <div className="text-sm text-foreground font-medium">
                  {getDisplayText()}
                </div>
              </div>
            )}

            {/* 确认按钮 */}
            {value.planet && (
              <div className="mt-4 pt-4 border-t border-border flex justify-end">
                <button
                  type="button"
                  onClick={() => setIsOpen(false)}
                  className="px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 transition-colors"
                >
                  确认
                </button>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
