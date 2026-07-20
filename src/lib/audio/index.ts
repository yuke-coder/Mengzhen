"use client";

/**
 * 统一音频管理入口
 *
 * Web 端保留工具函数和类型导出。
 * 实际播放由纯原生 App 的 AudioPlaybackService 处理。
 * Web 端不再有播放能力，只保留音频文件管理（上传、格式、时长计算等）。
 */

import type { ScheduledTask, TaskAudio } from '@/lib/task-types';
import type { UnifiedPlaybackConfig } from './types';

// ============ 工具函数和常量(前端在用) ============

export { formatFileSize, formatDuration } from './utils';
export type { AudioItemBase } from './utils';
export { AUDIO_EXTENSIONS, AUDIO_ACCEPT } from './formats';
export { getAudioBlob, saveAudioBlob, deleteAudioBlob } from './db';

// ============ Stub 函数(前端导入但实际由原生处理) ============

/** AudioContext 初始化 - 原生环境不需要,空实现 */
export function initializeAudioContext(): void {}

/** AudioContext 解锁 - 原生环境不需要 */
export function tryUnlockAudio(): void {}

/** AudioContext 恢复 - 原生环境不需要 */
export function resumeAudioContext(): Promise<boolean> {
  return Promise.resolve(true);
}

/** 获取 AudioContext 状态 - 原生环境不需要 */
export function getAudioState(): string | undefined {
  return undefined;
}

/** 设置音量 - 由原生 AudioPlaybackService 处理,web 侧空实现 */
export function setVolume(_audio: HTMLAudioElement, _volume: number): void {}

/** 渐入 - 由原生 Handler 处理,web 侧空实现 */
export function fadeIn(
  _audio: HTMLAudioElement,
  _options: { duration: number; targetVolume: number; onComplete?: () => void },
  _alreadyElapsed?: number
): void {}

/** 渐出 - 由原生 Handler 处理,web 侧空实现 */
export function fadeOut(
  _audio: HTMLAudioElement,
  _options: { duration: number; targetVolume: number; onComplete?: () => void }
): void {}

/** 停止渐入渐出 - 由原生处理 */
export function stopFade(): void {}

/** MediaSession 设置 - 由原生 MediaSession 处理 */
export function setupMediaSession(): void {}

/** Audio 自动解锁设置 - 由原生处理 */
export function setupAutoUnlock(): void {}

/** MediaSession 元数据更新 - 由原生处理 */
export function updateMediaSessionMetadata(_task: ScheduledTask, _audio?: TaskAudio): void {}

/** MediaSession 播放状态更新 - 由原生处理 */
export function updateMediaSessionPlaybackState(_playing: boolean): void {}

/** MediaSession 释放 - 由原生处理 */
export function releaseMediaSession(): void {}

// ============ UnifiedAudioManager 单例 ============

class UnifiedAudioManager {
  private static instance: UnifiedAudioManager;
  private initialized = false;

  private constructor() {}

  static getInstance(): UnifiedAudioManager {
    if (!UnifiedAudioManager.instance) {
      UnifiedAudioManager.instance = new UnifiedAudioManager();
    }
    return UnifiedAudioManager.instance;
  }

  async initialize(): Promise<boolean> {
    if (this.initialized) return true;
    this.initialized = true;
    return true;
  }

  isNative(): boolean {
    return false;
  }

  /** Web 端不支持播放 */
  async play(_config: UnifiedPlaybackConfig): Promise<boolean> {
    console.warn('[UnifiedAudioManager] Web 端不支持播放，请下载原生 App');
    return false;
  }

  stop(_configId?: string): void {}
  stopAll(): void {}

  async playAudio(_task: ScheduledTask, _scheduledStartAt: number): Promise<void> {}
  stopAudio(_taskId: string): void {}
  getActiveTaskIds(): string[] { return []; }
  async syncTasks(): Promise<void> {}

  // ========== 兼容旧 API ==========
  async requestWakeLock(): Promise<boolean> { return false; }
  releaseWakeLock(): void {}
  setKeepScreenOn(_value: boolean): void {}
  shouldKeepScreenOn(): boolean { return false; }
  tryUnlockAudio(): void {}
  async resumeAudioContext(): Promise<boolean> { return true; }
  getAudioState(): string | undefined { return undefined; }
  updateMediaSessionMetadata(_task: ScheduledTask, _audio?: TaskAudio): void {}
  updateMediaSessionPlaybackState(_playing: boolean): void {}
}

export default UnifiedAudioManager;

// Re-exports
export * from './types';
