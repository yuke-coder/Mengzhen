"use client";

import type { ScheduledTask, TaskAudio } from "@/lib/task-types";

/**
 * MediaSession Manager - 优化版
 * 处理锁屏和通知中心显示，支持后台播放
 */

let currentTask: ScheduledTask | null = null;
let currentAudio: TaskAudio | null = null;
let isPlaying = false;
let setup = false;

/**
 * 设置 MediaSession
 */
export function setupMediaSession(): void {
  if (setup) return;
  if (!('mediaSession' in navigator)) {
    console.log('[MediaSession] 不支持 MediaSession API');
    return;
  }

  // 设置播放状态处理器
  navigator.mediaSession.setActionHandler('play', () => {
    console.log('[MediaSession] 播放');
    window.dispatchEvent(new CustomEvent('mediasession-play'));
  });

  navigator.mediaSession.setActionHandler('pause', () => {
    console.log('[MediaSession] 暂停');
    window.dispatchEvent(new CustomEvent('mediasession-pause'));
  });

  navigator.mediaSession.setActionHandler('previoustrack', () => {
    console.log('[MediaSession] 上一曲');
  });

  navigator.mediaSession.setActionHandler('nexttrack', () => {
    console.log('[MediaSession] 下一曲');
  });

  navigator.mediaSession.setActionHandler('seekforward', (details) => {
    console.log('[MediaSession] 快进', details.seekTime);
  });

  navigator.mediaSession.setActionHandler('seekbackward', (details) => {
    console.log('[MediaSession] 快退', details.seekTime);
  });

  // 停止/关闭处理
  navigator.mediaSession.setActionHandler('stop', () => {
    console.log('[MediaSession] 停止');
    window.dispatchEvent(new CustomEvent('mediasession-stop'));
  });

  setup = true;
  console.log('[MediaSession] 设置完成');
}

/**
 * 更新 MediaSession 元数据
 */
export function updateMediaSessionMetadata(
  task: ScheduledTask,
  audio?: TaskAudio
): void {
  if (!('mediaSession' in navigator)) return;

  currentTask = task;
  currentAudio = audio || task.audios[0];
  const audioInfo = currentAudio;

  if (!audioInfo) return;

  try {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: audioInfo.name || task.name || '梦枕助眠',
      artist: '梦枕',
      album: '助眠音乐',
      artwork: [
        { src: '/logo-96.png', sizes: '96x96', type: 'image/png' },
        { src: '/logo-128.png', sizes: '128x128', type: 'image/png' },
        { src: '/logo-192.png', sizes: '192x192', type: 'image/png' },
        { src: '/logo-256.png', sizes: '256x256', type: 'image/png' },
        { src: '/logo-384.png', sizes: '384x384', type: 'image/png' },
        { src: '/logo-512.png', sizes: '512x512', type: 'image/png' },
      ]
    });
    console.log('[MediaSession] 元数据已更新');
  } catch (error) {
    console.error('[MediaSession] 更新元数据失败:', error);
  }
}

/**
 * 更新播放状态
 */
export function updateMediaSessionPlaybackState(playing: boolean): void {
  if (!('mediaSession' in navigator)) return;

  isPlaying = playing;
  try {
    navigator.mediaSession.playbackState = playing ? 'playing' : 'paused';
    console.log('[MediaSession] 播放状态已更新:', playing ? 'playing' : 'paused');
  } catch (error) {
    console.error('[MediaSession] 更新状态失败:', error);
  }
}

/**
 * 释放 MediaSession
 */
export function releaseMediaSession(): void {
  if (!('mediaSession' in navigator)) return;

  try {
    navigator.mediaSession.metadata = null;
    navigator.mediaSession.playbackState = 'none';

    const handlers = ['play', 'pause', 'previoustrack', 'nexttrack', 'seekforward', 'seekbackward', 'stop'] as const;
    handlers.forEach(handler => {
      try {
        navigator.mediaSession.setActionHandler(handler as MediaSessionAction, null);
      } catch {}
    });

    currentTask = null;
    currentAudio = null;
    isPlaying = false;
    setup = false;
    console.log('[MediaSession] 已释放');
  } catch (error) {
    console.error('[MediaSession] 释放失败:', error);
  }
}

/**
 * 获取当前任务
 */
export function getCurrentTask(): ScheduledTask | null {
  return currentTask;
}
