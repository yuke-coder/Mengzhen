/**
 * 核心类型定义 - 精简版
 */

export type PlayPhase = 'waiting' | 'fading-in' | 'playing' | 'fading-out' | 'idle';

export interface SchedulerEvent {
  type: 'task-started' | 'task-completed' | 'task-cancelled' | 'task-resumed' | 'phase-change' | 'tick';
  taskId: string;
  phase: PlayPhase;
  remainingMs: number;
  taskName?: string;
}

export interface PlaybackState {
  taskId: string;
  phase: PlayPhase;
  audio: HTMLAudioElement;
  targetVolume: number;
  retryCount: number;
  currentBlobUrl: string | null;
  startedAt: number;
  gainNode: GainNode | null;
  sourceNode: MediaElementAudioSourceNode | null;
  // 任务计划开始时间（不是音频开始播放的时间）
  scheduledStartAt: number;
  // 已播放时长（毫秒）
  playedDuration: number;
}

export interface FadeOptions {
  duration: number;
  targetVolume: number;
  onComplete?: () => void;
}
