"use client";

import { getAudioBlob } from "./db";
import { getAudioContext } from "./context";
import { setVolume, fadeIn, fadeOut, stopFade } from "./fader";
import type { ScheduledTask } from "@/lib/task-types";

interface AudioTaskConfig {
  id: string;
  url: string;
  volume: number;
  fadeInDuration: number;
  fadeOutDuration: number;
  playDurationMinutes: number;
}

interface PlaybackState {
  taskId: string;
  audio: HTMLAudioElement;
  targetVolume: number;
  phase: 'fading-in' | 'playing' | 'fading-out';
  startedAt: number;
  scheduledStartAt: number;
  playedDuration: number;
  retryCount: number;
  currentBlobUrl: string | null;
  gainNode: any;
  sourceNode: any;
}

const activePlaybacks = new Map<string, PlaybackState>();

/**
 * 音频播放器
 */
export class UnifiedAudioPlayer {
  /**
   * 播放单个音频任务
   */
  async playAudio(
    task: ScheduledTask,
    scheduledStartAt: number
  ): Promise<PlaybackState | null> {
    const startTime = Date.now();
    console.log('[Player] 开始播放...');

    if (task.audios.length === 0) {
      console.warn('[Player] 没有音频可播放');
      return null;
    }

    let audioUrl: string | null = null;
    let isBlobUrl = false;

    for (const audio of task.audios) {
      if (audio.serverUrl && audio.serverUrl.trim() !== '') {
        audioUrl = audio.serverUrl;
        console.log('[Player] 使用服务器URL:', audio.name);
        break;
      }
      if (audio.fileKey && audio.fileKey.trim() !== '') {
        audioUrl = `/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}`;
        console.log('[Player] 使用文件密钥:', audio.name);
        break;
      }
      if (audio.dbKey && audio.dbKey.trim() !== '') {
        try {
          const blob = await getAudioBlob(audio.dbKey);
          if (blob) {
            audioUrl = URL.createObjectURL(blob);
            isBlobUrl = true;
            console.log('[Player] 使用IndexedDB:', audio.name);
            break;
          }
        } catch (error) {
          console.warn('[Player] IndexedDB读取失败:', error);
        }
      }
    }

    if (!audioUrl || audioUrl.trim() === '') {
      console.error('[Player] 无法找到有效的音频源');
      return null;
    }

    const audioElement = new Audio();
    audioElement.src = audioUrl;
    audioElement.loop = true;
    audioElement.volume = 0;
    audioElement.preload = 'auto';

    await new Promise<void>((resolve) => {
      const onCanPlay = () => {
        console.log('[Player] 音频可以播放了');
        audioElement.removeEventListener('canplay', onCanPlay);
        audioElement.removeEventListener('error', onError);
        resolve();
      };
      const onError = () => {
        console.warn('[Player] 音频加载错误');
        audioElement.removeEventListener('canplay', onCanPlay);
        audioElement.removeEventListener('error', onError);
        resolve();
      };
      audioElement.addEventListener('canplay', onCanPlay);
      audioElement.addEventListener('error', onError);
      setTimeout(resolve, 2000);
      audioElement.load();
    });

    audioElement.play().catch((error) => {
      console.warn('[Player] 初始播放失败:', error);
    });

    const playback: PlaybackState = {
      taskId: task.id,
      audio: audioElement,
      targetVolume: (task.volume || 70) / 100,
      phase: 'fading-in',
      startedAt: Date.now(),
      scheduledStartAt,
      playedDuration: 0,
      retryCount: 0,
      currentBlobUrl: isBlobUrl ? audioUrl : null,
      gainNode: null,
      sourceNode: null
    };

    activePlaybacks.set(task.id, playback);
    console.log('[Player] 播放已启动，目标音量:', playback.targetVolume);
    return playback;
  }

  /**
   * 获取有效的音频URL
   */
  async getAudioUrl(task: ScheduledTask): Promise<string | null> {
    for (const audio of task.audios) {
      if (audio.serverUrl) return audio.serverUrl;
      if (audio.fileKey) {
        return `/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}`;
      }
      if (audio.dbKey) {
        const blob = await getAudioBlob(audio.dbKey);
        if (blob) return URL.createObjectURL(blob);
      }
    }
    return null;
  }

  /**
   * 停止音频
   */
  stopAudio(taskId: string): void {
    const playback = activePlaybacks.get(taskId);
    if (!playback) return;

    stopFade();

    try {
      playback.audio.volume = 0;
      playback.audio.pause();
      playback.audio.removeAttribute('src');
      playback.audio.src = '';
      try { playback.audio.load(); } catch {}
      if (playback.currentBlobUrl) {
        try { URL.revokeObjectURL(playback.currentBlobUrl); } catch {}
      }
    } catch (error) {
      console.error('[Player] 停止音频失败:', error);
    }

    activePlaybacks.delete(taskId);
  }

  /**
   * 获取活跃播放状态
   */
  getPlayback(taskId: string): PlaybackState | undefined {
    return activePlaybacks.get(taskId);
  }

  /**
   * 检查是否正在播放
   */
  hasActivePlayback(taskId: string): boolean {
    return activePlaybacks.has(taskId);
  }

  /**
   * 获取所有活跃播放任务
   */
  getActiveTaskIds(): string[] {
    return Array.from(activePlaybacks.keys());
  }

  /**
   * 清除所有播放
   */
  stopAll(): void {
    stopFade();
    activePlaybacks.forEach((_, id) => this.stopAudio(id));
  }
}

export const unifiedAudioPlayer = new UnifiedAudioPlayer();

