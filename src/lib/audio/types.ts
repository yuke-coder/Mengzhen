"use client";

/**
 * 统一播放配置类型
 * Templates 页面和 Scheduler 共用此类型，确保参数一致性
 */

/** 单个音频轨道信息（已解析 URL） */
export interface AudioTrack {
  id: string;
  name: string;
  /** 已解析的可播放 URL（web 侧或 native 侧） */
  url: string;
  /** 原始来源标识 */
  source: 'server' | 'proxy' | 'indexeddb' | 'local-file';
  /** 音频时长（秒），可选 */
  duration?: number;
  /** 文件大小（字节），可选 */
  size?: number;
}

/** 播放阶段 */
export type PlaybackPhase = 'idle' | 'waiting' | 'fading-in' | 'playing' | 'fading-out' | 'completed';

/** 统一播放配置 */
export interface UnifiedPlaybackConfig {
  /** 唯一标识（任务 ID 或模板 ID） */
  id: string;
  /** 显示名称 */
  name: string;
  /** 播放列表（有序） */
  tracks: AudioTrack[];
  /** 音量 0-100 */
  volume: number;
  /** 是否启用渐入渐出 */
  enableFade: boolean;
  /** 渐入时长（秒） */
  fadeInDuration: number;
  /** 渐出时长（秒） */
  fadeOutDuration: number;
  /** 播放时长（分钟），与 endTime 二选一 */
  playDurationMinutes?: number;
  /** 开始时间戳（ms），用于调度 */
  startTime?: number;
  /** 结束时间戳（ms），与 playDurationMinutes 二选一，优先级更高 */
  endTime?: number;
  /** 是否单曲循环（false=播完列表后从头开始，true=当前单曲循环直到停止） */
  loopSingle?: boolean;
}

/** 播放状态快照（供 UI 订阅） */
export interface PlaybackStateSnapshot {
  /** 当前配置 ID */
  configId: string;
  /** 当前播放阶段 */
  phase: PlaybackPhase;
  /** 当前轨道索引 */
  currentTrackIndex: number;
  /** 当前轨道名称 */
  currentTrackName: string;
  /** 当前轨道已播放时长（秒） */
  currentTrackElapsed: number;
  /** 当前轨道总时长（秒） */
  currentTrackDuration: number;
  /** 目标音量 0-1 */
  targetVolume: number;
  /** 渐入剩余秒数 */
  fadeInRemaining: number;
  /** 渐出剩余秒数 */
  fadeOutRemaining: number;
  /** 播放总剩余秒数 */
  totalRemaining: number;
  /** 下一首轨道名称 */
  nextTrackName: string;
  /** 是否正在播放 */
  isPlaying: boolean;
  /** 配置快照 */
  config: UnifiedPlaybackConfig | null;
}

/** 播放事件回调 */
export interface PlaybackCallbacks {
  /** 阶段变化 */
  onPhaseChange?: (configId: string, phase: PlaybackPhase) => void;
  /** 轨道切换 */
  onTrackChange?: (configId: string, trackIndex: number, track: AudioTrack) => void;
  /** 状态更新（每秒触发） */
  onStateUpdate?: (snapshot: PlaybackStateSnapshot) => void;
  /** 播放结束 */
  onComplete?: (configId: string) => void;
  /** 错误 */
  onError?: (configId: string, error: Error, trackIndex: number) => void;
}

/** 从 ScheduledTask 转换为 UnifiedPlaybackConfig 的工厂参数 */
export interface TaskToConfigOptions {
  /** 是否已经解析了音频 URL */
  resolvedTracks?: AudioTrack[];
}
