/**
 * 助眠功能 localStorage 存储模块
 *
 * 命名约定：沿用梦枕 dream_ 前缀 + sleep_ 子前缀
 * 对齐 QQ音乐的 MMKV 存储模式（key-value 持久化）
 */

const STORAGE_PREFIX = 'dream_sleep_';

/** 存储键名枚举 */
export const SleepStorageKeys = {
  /** 当前助眠模式 */
  MODE: `${STORAGE_PREFIX}mode`,
  /** 当前场景背景 */
  SCENE: `${STORAGE_PREFIX}scene`,
  /** 定时关闭模式（countdown/infinite） */
  TIMER_MODE: `${STORAGE_PREFIX}timer_mode`,
  /** 定时关闭剩余分钟 */
  TIMER_MINUTES: `${STORAGE_PREFIX}timer_minutes`,
  /** 助眠音效设置（序列化字符串） */
  EFFECT_SETTING: `${STORAGE_PREFIX}effect_setting`,
  /** 助眠音效开关 */
  EFFECT_ENABLED: `${STORAGE_PREFIX}effect_enabled`,
  /** 上次定时关闭配置（用于记忆用户选择） */
  LAST_TIMER_CONFIG: `${STORAGE_PREFIX}last_timer_config`,
} as const;

/** 通用读取 */
function get(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

/** 通用写入 */
function set(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // 存储满或隐私模式忽略
  }
}

/** 通用删除 */
function remove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // 忽略
  }
}

/** 字符串读取 */
export function getString(key: string, defaultValue = ''): string {
  return get(key) ?? defaultValue;
}

/** 字符串写入 */
export function setString(key: string, value: string): void {
  set(key, value);
}

/** 布尔读取 */
export function getBool(key: string, defaultValue = false): boolean {
  const v = get(key);
  if (v === null) return defaultValue;
  return v === 'true';
}

/** 布尔写入 */
export function setBool(key: string, value: boolean): void {
  set(key, value ? 'true' : 'false');
}

/** 数字读取 */
export function getNumber(key: string, defaultValue = 0): number {
  const v = get(key);
  if (v === null) return defaultValue;
  const n = parseFloat(v);
  return isNaN(n) ? defaultValue : n;
}

/** 数字写入 */
export function setNumber(key: string, value: number): void {
  set(key, String(value));
}

/** JSON 对象读取 */
export function getJSON<T>(key: string, defaultValue: T): T {
  const v = get(key);
  if (!v) return defaultValue;
  try {
    return JSON.parse(v) as T;
  } catch {
    return defaultValue;
  }
}

/** JSON 对象写入 */
export function setJSON(key: string, value: unknown): void {
  try {
    set(key, JSON.stringify(value));
  } catch {
    // 忽略循环引用等错误
  }
}

/** 删除 */
export function removeKey(key: string): void {
  remove(key);
}
