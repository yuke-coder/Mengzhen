"use client";

import { getAudioBlob } from "./db";
import { stopFade } from "./fader";
import { savePlaybackState, getPlaybackState } from "./state";
import type { ScheduledTask } from "@/lib/task-types";

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
  saveProgress?: () => void;
}

const activePlaybacks = new Map<string, PlaybackState>();

/**
 * 获取有效的音频URL
 */
async function getAudioUrl(task: ScheduledTask): Promise<{ url: string; isBlobUrl: boolean } | null> {
  for (const audio of task.audios) {
    if (audio.serverUrl && audio.serverUrl.trim() !== '') {
      return { url: audio.serverUrl, isBlobUrl: false };
    }
    if (audio.fileKey && audio.fileKey.trim() !== '') {
      return { url: `/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}`, isBlobUrl: false };
    }
    if (audio.dbKey && audio.dbKey.trim() !== '') {
      try {
        const blob = await getAudioBlob(audio.dbKey);
        if (blob) {
          return { url: URL.createObjectURL(blob), isBlobUrl: true };
        }
      } catch (error) {
        console.warn('[Player] IndexedDB读取失败:', error);
      }
    }
  }
  return null;
}

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
    console.log('[Player] 开始播放...');

    if (task.audios.length === 0) {
      console.warn('[Player] 没有音频可播放');
      return null;
    }

    const audioResult = await getAudioUrl(task);
    if (!audioResult) {
      console.error('[Player] 无法找到有效的音频源');
      return null;
    }
    const { url: audioUrl, isBlobUrl } = audioResult;

    const audioElement = new Audio();
    audioElement.src = audioUrl;
    audioElement.loop = true;
    audioElement.volume = 0;
    audioElement.preload = 'auto';

    // 尝试恢复播放进度
    const savedState = getPlaybackState(task.id);
    if (savedState) {
      audioElement.currentTime = savedState.currentTime;
      audioElement.volume = savedState.volume;
    }

    // 实时保存播放进度
    const saveProgress = () => {
      savePlaybackState(task.id, audioElement.currentTime, audioElement.volume, !audioElement.paused);
    };
    audioElement.addEventListener('timeupdate', saveProgress);
    audioElement.addEventListener('play', saveProgress);
    audioElement.addEventListener('pause', saveProgress);

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
      targetVolume: (task.volume ?? 70) / 100,
      phase: 'fading-in',
      startedAt: Date.now(),
      scheduledStartAt,
      playedDuration: 0,
      retryCount: 0,
      currentBlobUrl: isBlobUrl ? audioUrl : null,
      saveProgress
    };

    activePlaybacks.set(task.id, playback);
    console.log('[Player] 播放已启动，目标音量:', playback.targetVolume);
    return playback;
  }

  /**
   * 停止音频
   */
  stopAudio(taskId: string): void {
    const playback = activePlaybacks.get(taskId);
    if (!playback) return;

    stopFade();

    try {
      // 清理事件监听器
      if (playback.saveProgress) {
        playback.audio.removeEventListener('timeupdate', playback.saveProgress);
        playback.audio.removeEventListener('play', playback.saveProgress);
        playback.audio.removeEventListener('pause', playback.saveProgress);
      }

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
