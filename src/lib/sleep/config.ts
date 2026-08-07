/**
 * QQ音乐脑波疗愈助眠配置（整迁自 jzt_router.apk + brain_wave_healing_main_player.json）
 *
 * 原始来源：
 * - brain_wave_healing_main_player.json (page_name: bw_main_player)
 * - brain_wave_healing_submode.json (mode: sleep/meditation/focus, scene: ocean/forest/sky)
 * - assets/bw_common/ (3 种模式音频 + 引导视频 + PAG 动画)
 * - assets/bw_main_player/ (播放控制按钮图标)
 */

/** 助眠模式（对齐 QQ音乐 brain_wave_healing_sub_mode 的 mode 字段） */
export type SleepMode = 'sleep' | 'meditation' | 'focus';

/** 场景背景（对齐 QQ音乐 brain_wave_healing_sub_mode 的 scene_name 字段） */
export type SleepScene = 'ocean' | 'forest' | 'sky' | 'soundspace' | 'spatial_audio' | 'personalization';

/** 助眠模式元数据 */
export interface SleepModeConfig {
  /** 模式标识 */
  mode: SleepMode;
  /** 中文名称 */
  title: string;
  /** 英文名称 */
  enName: string;
  /** 主题色（用于背景渐变） */
  color: string;
  /** 本地引导音频文件名（assets/bw_common/ 下） */
  audioFile: string;
  /** 模式描述 */
  description: string;
}

/** 助眠场景背景元数据 */
export interface SleepSceneConfig {
  /** 场景标识 */
  scene: SleepScene;
  /** 中文名称 */
  title: string;
  /** 背景图 URL（QQ音乐 CDN） */
  imageBg: string;
  /** 背景 PAG 动画 URL（QQ音乐 CDN，动态星空等） */
  videoBg: string;
}

/**
 * 三种助眠模式配置
 * 整迁自 assets/bw_common/ 的 qmkuikly_bw_audio_{sleep,meditation,focus}.mp3
 */
export const SLEEP_MODES: SleepModeConfig[] = [
  {
    mode: 'sleep',
    title: '助眠',
    enName: 'Sleep',
    color: '#1a237e',
    audioFile: 'qmkuikly_bw_audio_sleep.mp3',
    description: '深度睡眠引导，帮助快速入睡',
  },
  {
    mode: 'meditation',
    title: '冥想',
    enName: 'Meditation',
    color: '#00695c',
    audioFile: 'qmkuikly_bw_audio_meditation.mp3',
    description: '静心冥想，放松身心',
  },
  {
    mode: 'focus',
    title: '专注',
    enName: 'Focus',
    color: '#e65100',
    audioFile: 'qmkuikly_bw_audio_focus.mp3',
    description: '提升专注力，高效工作学习',
  },
];

/**
 * 场景背景配置
 * 整迁自 brain_wave_healing_main_player.json 的 image_bg / video_bg 字段
 * 及 R.java 中的 qmkuikly_bw_bg_* 资源
 */
export const SLEEP_SCENES: SleepSceneConfig[] = [
  {
    scene: 'ocean',
    title: '海洋',
    imageBg: 'https://dlied5sdk.myapp.com/music/release/upload/ocs/ffadaec023d1b5db583b47eb055cfd9f.png',
    videoBg: 'https://music-conf-cdn.y.qq.com/ocs/1c63337_lgx9044p/f8678c1e16f17a8fecc9f3768593f1e1.pag',
  },
  {
    scene: 'forest',
    title: '森林',
    imageBg: '',
    videoBg: '',
  },
  {
    scene: 'sky',
    title: '星空',
    imageBg: '',
    videoBg: '',
  },
  {
    scene: 'soundspace',
    title: '声空间',
    imageBg: '',
    videoBg: '',
  },
];

/**
 * 疗愈模式播放列表（20 首）
 * 整迁自 brain_wave_healing_main_player.json 的 play_info.song 数组
 * 歌曲通过 QQ音乐 API 播放
 */
export const HEALING_SONG_IDS: string[] = [
  '378337006', '378337007', '378337008', '378315024', '378314987',
  '378315051', '378315032', '378315052', '378315055', '378314969',
  '378314946', '378315045', '378315008', '378314975', '378315048',
  '378311479', '378311471', '378311477', '378311478', '378311472',
];

/**
 * 疗愈模式播放来源
 * 整迁自 brain_wave_healing_main_player.json 的 play_info 字段
 */
export const HEALING_PLAY_INFO = {
  /** 播放动作：后台播放 */
  action: 'backplay' as const,
  /** 是否隐藏加载动画 */
  hideLoading: 1,
  /** 长音频播放列表块类型 */
  longAudioPlaySongListBlockType: 'album' as const,
  /** 播放来源 ID */
  from: '42800849',
  /** 来源信息 */
  source: {
    type: 111117,
    listTag: 146,
    title: '疗愈模式',
  },
} as const;

/** 定时关闭模式（对齐 QQ音乐 qmkuikly_bw_countdown_mode / qmkuikly_bw_infinit_mode） */
export type TimerMode = 'countdown' | 'infinite';

/** 定时关闭预设时长（分钟） */
export const TIMER_PRESETS: number[] = [15, 30, 60, 90];

/**
 * 获取助眠模式配置
 */
export function getSleepMode(mode: SleepMode): SleepModeConfig {
  return SLEEP_MODES.find((m) => m.mode === mode) ?? SLEEP_MODES[0];
}

/**
 * 获取场景背景配置
 */
export function getSleepScene(scene: SleepScene): SleepSceneConfig {
  return SLEEP_SCENES.find((s) => s.scene === scene) ?? SLEEP_SCENES[0];
}
