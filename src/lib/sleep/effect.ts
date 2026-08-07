/**
 * 助眠音效配置模块
 * 整迁自 com.tencent.qqmusicplayerprocess.audio.supersound.SuperSoundSleepEffectSetting
 *       及 com.tencent.qqmusic.activity.soundfx.supersound.SleepEffectBackSoundItem
 *
 * 原始设计：
 * - 助眠效果 = 背景白噪音(backSoundName) + 音量增益(gain)
 * - 序列化格式: backSoundName + "\u0003" + gain (用 SongTable.MULTI_SINGERS_SPLIT_CHAR 分隔)
 * - 互斥类型: sleepEffectParam = 8 (与 EQ/3D/全景等音效互斥)
 * - 可编辑效果ID: ss_editable_effect_sleep_id = 808
 * - 白噪音资源从服务端下发，本地下载缓存播放
 */

/** 白噪音资源列表项（对齐 SleepEffectBackSoundItem） */
export interface BackSoundItem {
  /** 资源 ID */
  id: string;
  /** 中文名 */
  cnName: string;
  /** 英文名 */
  enName: string;
  /** 主题色 */
  color: string;
  /** 封面图 URL */
  imgUrl: string;
  /** 选中封面图 URL */
  imgUrlSelected: string;
  /** 音频下载地址 */
  url: string;
  /** MD5 校验值 */
  md5: string;
  /** 文件大小（字节） */
  size: number;
}

/** 助眠音效设置（对齐 SuperSoundSleepEffectSetting） */
export interface SleepEffectSetting {
  /** 背景白噪音名称 */
  backSoundName: string;
  /** 音量增益（0-1，float） */
  gain: number;
}

/** 互斥类型（对齐 SSMutexTypes，sleepEffectParam=8） */
export const SLEEP_EFFECT_MUTEX_TYPE = 8;

/** 助眠可编辑效果 ID（对齐 SSEditableEffectIdDefine，ss_editable_effect_sleep_id=808） */
export const SLEEP_EFFECT_EDITABLE_ID = 808;

/** 序列化分隔符（对齐 SongTable.MULTI_SINGERS_SPLIT_CHAR = \u0003） */
const SERIALIZATION_SPLIT_CHAR = '\u0003';

/**
 * 序列化助眠音效设置
 * 对齐 SuperSoundSleepEffectSetting 的序列化逻辑
 */
export function serializeSleepEffect(setting: SleepEffectSetting): string {
  return `${setting.backSoundName}${SERIALIZATION_SPLIT_CHAR}${setting.gain}`;
}

/**
 * 反序列化助眠音效设置
 * 对齐 SuperSoundSleepEffectSetting.from(String) 方法
 */
export function deserializeSleepEffect(data: string): SleepEffectSetting | null {
  if (!data) return null;
  const parts = data.split(SERIALIZATION_SPLIT_CHAR);
  if (parts.length < 2) return null;
  const backSoundName = parts[0];
  const gain = parseFloat(parts[1]);
  if (!backSoundName || isNaN(gain)) return null;
  return { backSoundName, gain };
}

/**
 * 默认助眠音效设置
 */
export const DEFAULT_SLEEP_EFFECT: SleepEffectSetting = {
  backSoundName: '',
  gain: 0.5,
};
