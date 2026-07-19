"use client";

/**
 * 统一音频管理入口(仅原生 Android 播放服务)
 *
 * 架构:
 *   前端 (Templates/Scheduler/Hooks)
 *     -> @/lib/audio (本文件,提供所有导出名称)
 *     -> native-scheduler.ts (Capacitor 桥接)
 *     -> AudioPlaybackService.java (唯一播放引擎)
 *
 * 删除的 web 引擎文件:
 *   player.ts, fader.ts, wakelock.ts, mediasession.ts,
 *   context.ts, state.ts, unlock.ts, debug.ts
 *
 * 所有播放、渐入渐出、WakeLock、MediaSession、定时停止
 * 均由 Android AudioPlaybackService.java 处理,
 * 不再依赖 HTML5 Audio / JS 定时器。
 */

import type { ScheduledTask, TaskAudio } from '@/lib/task-types';
import type { UnifiedPlaybackConfig } from './types';

// ============ 工具函数和常量(保留,前端在用) ============

export { formatFileSize, formatDuration } from './utils';
export type { AudioItemBase } from './utils';
export { AUDIO_EXTENSIONS, AUDIO_ACCEPT } from './formats';
export { getAudioBlob, saveAudioBlob, deleteAudioBlob } from './db';

// ============ 原生环境检测和桥接 ============

export { isNativeEnvironment } from '@/lib/native-scheduler';
import { isNativeEnvironment } from '@/lib/native-scheduler';
import { triggerNativePlayback, stopNativePlayback, syncTasksToNative } from '@/lib/native-scheduler';

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
    return isNativeEnvironment();
  }

  /**
   * 开始播放 - 唯一播放入口
   * 委托给 Android AudioPlaybackService
   */
  async play(config: UnifiedPlaybackConfig): Promise<boolean> {
    if (!isNativeEnvironment()) {
      console.warn('[UnifiedAudioManager] 非原生环境,播放不可用');
      return false;
    }

    // 解析播放列表 URL 并调用原生播放
    const { resolveTracksJson } = await import('./resolve');
    const { tracksJson, firstUrl, firstName } = await resolveTracksJson(config.tracks);

    const Capacitor = (window as any).Capacitor;
    const plugin = Capacitor?.Plugins?.AlarmScheduler;
    if (!plugin) {
      console.error('[UnifiedAudioManager] AlarmScheduler 插件不可用');
      return false;
    }

    try {
      await plugin.playNow({
        taskId: config.id,
        taskName: config.name,
        playDurationMinutes: config.playDurationMinutes ?? 30,
        volume: config.volume,
        enableFade: config.enableFade,
        fadeInDuration: config.fadeInDuration,
        fadeOutDuration: config.fadeOutDuration,
        audioUrl: firstUrl,
        audioName: firstName,
        tracksJson,
        loopSingle: config.loopSingle ?? true,
        endTime: config.endTime ?? 0,
      });
      return true;
    } catch (e) {
      console.error('[UnifiedAudioManager] 播放失败:', e);
      return false;
    }
  }

  /** 停止播放 */
  stop(_configId?: string): void {
    stopNativePlayback();
  }

  stopAll(): void {
    stopNativePlayback();
  }

  /** Scheduler 旧接口 */
  async playAudio(task: ScheduledTask, scheduledStartAt: number): Promise<void> {
    if (!isNativeEnvironment()) return;
    await triggerNativePlayback(task.id);
  }

  stopAudio(_taskId: string): void {
    stopNativePlayback();
  }

  getActiveTaskIds(): string[] {
    return [];
  }

  async syncTasksToNative(): Promise<void> {
    await syncTasksToNative();
  }

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
