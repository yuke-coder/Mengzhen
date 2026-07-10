"use client";

interface AudioPlaybackState {
  currentTime: number;
  volume: number;
  isPlaying: boolean;
  taskId?: string;
  timestamp: number;
}

// 全局音频播放管理器
class AudioPlaybackManager {
  private static instance: AudioPlaybackManager;
  private audioElements: Map<string, HTMLAudioElement> = new Map();
  private playbackStates: Map<string, AudioPlaybackState> = new Map();
  private persistKey = 'audio-playback-states';

  private constructor() {
    this.loadSavedStates();
    this.setupAutoSave();
  }

  public static getInstance(): AudioPlaybackManager {
    if (!AudioPlaybackManager.instance) {
      AudioPlaybackManager.instance = new AudioPlaybackManager();
    }
    return AudioPlaybackManager.instance;
  }

  // 获取或创建音频元素
  getAudioElement(audioUrl: string, taskId?: string): HTMLAudioElement {
    if (this.audioElements.has(audioUrl)) {
      return this.audioElements.get(audioUrl)!;
    }

    const audio = new Audio(audioUrl);
    audio.preload = 'metadata';
    this.audioElements.set(audioUrl, audio);

    // 监听播放状态变化
    audio.addEventListener('timeupdate', () => {
      if (taskId) {
        this.savePlaybackState(taskId, audio.currentTime, audio.volume, !audio.paused);
      }
    });

    return audio;
  }

  // 保存播放状态
  savePlaybackState(taskId: string, currentTime: number, volume: number, isPlaying: boolean) {
    const state: AudioPlaybackState = {
      currentTime,
      volume,
      isPlaying,
      taskId,
      timestamp: Date.now()
    };

    this.playbackStates.set(taskId, state);
  }

  // 获取播放状态
  getPlaybackState(taskId: string): AudioPlaybackState | undefined {
    return this.playbackStates.get(taskId);
  }

  // 恢复任务播放状态
  restoreTaskPlayback(taskId: string, audioElement: HTMLAudioElement): boolean {
    const state = this.playbackStates.get(taskId);
    if (!state) return false;

    try {
      audioElement.currentTime = state.currentTime;
      audioElement.volume = state.volume;

      if (state.isPlaying) {
        audioElement.play().catch(() => {});
      }

      return true;
    } catch (error) {
      console.warn('[AudioPlayback] 恢复播放状态失败:', error);
      return false;
    }
  }

  // 保存到localStorage
  private setupAutoSave() {
    // 定期保存状态
    setInterval(() => {
      this.saveToStorage();
    }, 10000);

    // 页面卸载前保存
    window.addEventListener('beforeunload', () => {
      this.saveToStorage();
    });

    // 页面可见性变化时保存
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        this.saveToStorage();
      }
    });
  }

  // 保存状态到存储
  private saveToStorage() {
    try {
      const states = Array.from(this.playbackStates.entries());
      localStorage.setItem(this.persistKey, JSON.stringify(states));
    } catch (error) {
      console.warn('[AudioPlayback] 保存状态失败:', error);
    }
  }

  // 从存储加载状态
  private loadSavedStates() {
    try {
      const saved = localStorage.getItem(this.persistKey);
      if (saved) {
        const states = JSON.parse(saved);
        states.forEach(([taskId, state]: [string, AudioPlaybackState]) => {
          this.playbackStates.set(taskId, state);
        });
      }
    } catch (error) {
      console.warn('[AudioPlayback] 加载保存状态失败:', error);
    }
  }

  // 清理过期状态
  cleanupExpiredStates(maxAgeMs: number = 3600000) {
    const now = Date.now();
    this.playbackStates.forEach((state, taskId) => {
      if (now - state.timestamp > maxAgeMs) {
        this.playbackStates.delete(taskId);
      }
    });
    this.saveToStorage();
  }

  // 清理所有状态
  clearAllStates() {
    this.playbackStates.clear();
    this.saveToStorage();
  }

  // 销毁所有音频元素
  destroyAllAudio() {
    this.audioElements.forEach(audio => {
      audio.pause();
      audio.src = '';
      audio.load();
    });
    this.audioElements.clear();
  }
}

export { AudioPlaybackManager, AudioPlaybackState };
export default AudioPlaybackManager;