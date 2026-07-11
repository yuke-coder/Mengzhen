"use client";

/**
 * 统一音频管理器
 * 将所有音频相关功能整合在一起
 */

import {
  getAudioContext,
  initializeAudioContext,
  resumeAudioContext,
  getAudioState,
  destroyAudioContext,
  tryUnlockAudio
} from './context';

import {
  requestWakeLock,
  releaseWakeLock,
  isWakeLockActive,
  isAndroidDevice
} from './wakelock';

import {
  stopFade,
  setVolume,
  fadeIn,
  fadeOut
} from './fader';

import {
  savePlaybackState,
  getPlaybackState,
  restoreTaskPlayback,
  cleanupExpiredStates,
  clearAllStates,
  clearTaskState
} from './state';

import {
  setupMediaSession,
  updateMediaSessionMetadata,
  updateMediaSessionPlaybackState,
  releaseMediaSession,
  getCurrentTask
} from './mediasession';

import { unifiedAudioPlayer } from './player';

import type { ScheduledTask, TaskAudio } from "@/lib/task-types";

interface AudioTaskConfig {
  id: string;
  url: string;
  volume: number;
  fadeInDuration: number;
  fadeOutDuration: number;
  playDurationMinutes: number;
}

interface PlayState {
  onEnded?: (taskId: string) => void;
  onError?: (taskId: string, error: Error) => void;
  onPhaseChange?: (taskId: string, phase: 'fading-in' | 'playing' | 'fading-out') => void;
}

class UnifiedAudioManager {
  private static instance: UnifiedAudioManager;
  private initialized = false;
  private callbacks = new Map<string, PlayState>();

  private constructor() {}

  static getInstance(): UnifiedAudioManager {
    if (!UnifiedAudioManager.instance) {
      UnifiedAudioManager.instance = new UnifiedAudioManager();
    }
    return UnifiedAudioManager.instance;
  }

  // ========== 初始化 ==========

  async initialize(): Promise<boolean> {
    if (this.initialized) return true;

    try {
      setupMediaSession();
      this.initialized = true;
      console.log('[AudioManager] 初始化成功');
      return true;
    } catch (error) {
      console.error('[AudioManager] 初始化失败:', error);
      return false;
    }
  }

  async initializeAudioContext(): Promise<boolean> {
    return initializeAudioContext();
  }

  tryUnlockAudio(): void {
    tryUnlockAudio();
  }

  resumeAudioContext(): Promise<boolean> {
    return resumeAudioContext();
  }

  isInitialized(): boolean {
    return this.initialized;
  }

  getAudioState(): string | undefined {
    return getAudioState();
  }

  // ========== WakeLock ==========

  async requestWakeLock(): Promise<boolean> {
    return requestWakeLock();
  }

  releaseWakeLock(): void {
    releaseWakeLock();
  }

  isWakeLockActive(): boolean {
    return isWakeLockActive();
  }

  static isAndroidDevice(): boolean {
    return isAndroidDevice();
  }

  // ========== 播放控制 ==========

  async playAudio(task: ScheduledTask, scheduledStartAt: number): Promise<void> {
    const playback = await unifiedAudioPlayer.playAudio(task, scheduledStartAt);
    if (!playback) return;

    updateMediaSessionMetadata(task);
    updateMediaSessionPlaybackState(true);

    const fadeInMs = task.enableFade ? (task.fadeInDuration || 0) * 1000 : 0;
    const audioStartAt = scheduledStartAt - fadeInMs;
    const alreadyElapsed = Math.max(0, Date.now() - audioStartAt);

    if (task.enableFade && task.fadeInDuration && task.fadeInDuration > 0 && alreadyElapsed < fadeInMs) {
      this.emitPhaseChange(task.id, 'fading-in');
      fadeIn(playback.audio, {
        duration: task.fadeInDuration,
        targetVolume: playback.targetVolume,
        onComplete: () => {
          this.emitPhaseChange(task.id, 'playing');
        }
      }, alreadyElapsed);
    } else {
      playback.audio.play().catch(() => {});
      setVolume(playback.audio, playback.targetVolume);
      this.emitPhaseChange(task.id, 'playing');
    }
  }

  stopAudio(taskId: string): void {
    unifiedAudioPlayer.stopAudio(taskId);
    clearTaskState(taskId);
    const playbacks = unifiedAudioPlayer.getActiveTaskIds();
    if (playbacks.length === 0) {
      updateMediaSessionPlaybackState(false);
    }
  }

  stopAll(): void {
    unifiedAudioPlayer.stopAll();
    clearAllStates();
    updateMediaSessionPlaybackState(false);
  }

  hasActivePlayback(taskId: string): boolean {
    return unifiedAudioPlayer.hasActivePlayback(taskId);
  }

  getActiveTaskIds(): string[] {
    return unifiedAudioPlayer.getActiveTaskIds();
  }

  // ========== 淡入淡出 ==========

  fadeInAudio(
    taskId: string,
    duration: number,
    targetVolume: number,
    onComplete?: () => void
  ): void {
    const playback = unifiedAudioPlayer.getPlayback(taskId);
    if (!playback) return;
    fadeIn(playback.audio, { duration, targetVolume, onComplete });
  }

  fadeOutAudio(
    taskId: string,
    duration: number,
    targetVolume: number = 0,
    onComplete?: () => void
  ): void {
    const playback = unifiedAudioPlayer.getPlayback(taskId);
    if (!playback) return;
    fadeOut(playback.audio, { duration, targetVolume, onComplete });
  }

  stopFade(): void {
    stopFade();
  }

  setVolume(taskId: string, volume: number): void {
    const playback = unifiedAudioPlayer.getPlayback(taskId);
    if (!playback) return;
    setVolume(playback.audio, volume);
  }

  // ========== 状态管理 ==========

  savePlaybackState(taskId: string, currentTime: number, volume: number, isPlaying: boolean): void {
    savePlaybackState(taskId, currentTime, volume, isPlaying);
  }

  getPlaybackState(taskId: string) {
    return getPlaybackState(taskId);
  }

  restoreTaskPlayback(taskId: string, audioElement: HTMLAudioElement): boolean {
    return restoreTaskPlayback(taskId, audioElement);
  }

  // ========== 媒体会话 ==========

  updateMediaSessionMetadata(task: ScheduledTask, audio?: TaskAudio): void {
    updateMediaSessionMetadata(task, audio);
  }

  updateMediaSessionPlaybackState(playing: boolean): void {
    updateMediaSessionPlaybackState(playing);
  }

  getCurrentTask(): ScheduledTask | null {
    return getCurrentTask();
  }

  // ========== 回调 ==========

  setCallbacks(taskId: string, callbacks: PlayState): void {
    this.callbacks.set(taskId, callbacks);
  }

  clearCallbacks(taskId: string): void {
    this.callbacks.delete(taskId);
  }

  private emitPhaseChange(taskId: string, phase: 'fading-in' | 'playing' | 'fading-out'): void {
    const callbacks = this.callbacks.get(taskId);
    callbacks?.onPhaseChange?.(taskId, phase);
  }

  // ========== 清理 ==========

  destroy(): void {
    console.log('[AudioManager] 销毁');
    unifiedAudioPlayer.stopAll();
    releaseWakeLock();
    releaseMediaSession();
    destroyAudioContext();
    this.initialized = false;
    this.callbacks.clear();
  }
}

export default UnifiedAudioManager;

// 导出所有模块
export * from './context';
export * from './wakelock';
export * from './fader';
export * from './state';
export * from './mediasession';
export * from './player';
export * from './utils';
export * from './formats';
export * from './db';
export * from './unlock';
export * from './debug';

