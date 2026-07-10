"use client";

// 健壮的音频管理系统
interface AudioTaskConfig {
  id: string;
  url: string;
  volume: number;
  fadeInDuration: number;
  fadeOutDuration: number;
  playDurationMinutes: number;
}

interface PlaybackState {
  currentTime: number;
  volume: number;
  isPlaying: boolean;
  startTime: number;
}

export class AudioManager {
  private static instance: AudioManager;
  private audioContext: AudioContext | null = null;
  private wakeLock: WakeLockSentinel | null = null;
  private activePlayers: Map<string, HTMLAudioElement | GainNode> = new Map();
  private playbackStates: Map<string, PlaybackState> = new Map();
  private initialized = false;

  private constructor() {}

  public static getInstance(): AudioManager {
    if (!AudioManager.instance) {
      AudioManager.instance = new AudioManager();
    }
    return AudioManager.instance;
  }

  // 初始化音频上下文（必须在用户交互后调用）
  async initialize(): Promise<boolean> {
    if (this.initialized) return true;

    try {
      const Context = window.AudioContext || (window as any).webkitAudioContext;
      if (!Context) {
        console.error('浏览器不支持AudioContext');
        return false;
      }

      this.audioContext = new Context();
      await this.audioContext.resume();
      this.initialized = true;

      console.log('[AudioManager] 音频上下文初始化成功，状态:', this.audioContext.state);
      return true;
    } catch (error) {
      console.error('[AudioManager] 初始化失败:', error);
      return false;
    }
  }

  // 请求唤醒锁（安卓专用）
  async requestWakeLock(): Promise<boolean> {
    if (!('wakeLock' in navigator)) return false;
    if (!this.audioContext) return false;

    try {
      this.wakeLock = await navigator.wakeLock.request('screen');
      this.wakeLock.addEventListener('release', () => {
        console.log('[AudioManager] Wake Lock已释放');
        this.wakeLock = null;
      });
      console.log('[AudioManager] Wake Lock获取成功');
      return true;
    } catch (error) {
      console.warn('[AudioManager] Wake Lock失败:', error);
      return false;
    }
  }

  // 释放资源
  destroy() {
    this.activePlayers.forEach(player => {
      if (player instanceof HTMLAudioElement) {
        player.pause();
        player.src = '';
      } else {
        player.disconnect();
      }
    });
    this.activePlayers.clear();
    this.playbackStates.clear();

    if (this.wakeLock) {
      this.wakeLock.release().catch(() => {});
      this.wakeLock = null;
    }

    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
    this.initialized = false;
  }

  // 播放单个音频
  async playAudio(config: AudioTaskConfig): Promise<void> {
    if (!this.audioContext || !this.initialized) {
      throw new Error('音频上下文未初始化');
    }

    // 确保上下文处于活跃状态
    if (this.audioContext.state === 'suspended') {
      await this.audioContext.resume();
    }

    return new Promise((resolve, reject) => {
      try {
        const audio = new Audio(config.url);
        audio.volume = config.volume / 100;

        // 应用淡入效果
        if (config.fadeInDuration > 0) {
          this.applyFade(audio, config.fadeInDuration, true);
        } else {
          audio.volume = config.volume / 100;
        }

        // 恢复播放状态
        const savedState = this.playbackStates.get(config.id);
        if (savedState && savedState.isPlaying) {
          audio.currentTime = savedState.currentTime;
        } else {
          audio.currentTime = 0;
        }

        // 播放完成处理
        const handleEnded = () => {
          if (config.fadeOutDuration > 0) {
            this.applyFade(audio, config.fadeOutDuration, false, () => {
              cleanup();
              resolve();
            });
          } else {
            cleanup();
            resolve();
          }
        };

        audio.addEventListener('ended', handleEnded);
        audio.addEventListener('error', reject);

        // 开始播放
        audio.play().then(() => {
          this.activePlayers.set(config.id, audio);
          this.playbackStates.set(config.id, {
            currentTime: audio.currentTime,
            volume: audio.volume,
            isPlaying: true,
            startTime: Date.now()
          });

          // 设置自动停止
          const totalDuration = config.playDurationMinutes * 60 * 1000;
          setTimeout(() => {
            if (audio.ended) return;
            handleEnded();
          }, totalDuration);
        }).catch(reject);

        // 清理函数
        const cleanup = () => {
          audio.pause();
          audio.removeEventListener('ended', handleEnded);
          audio.removeEventListener('error', reject);
          this.activePlayers.delete(config.id);
        };

      } catch (error) {
        reject(error);
      }
    });
  }

  // 应用淡入淡出效果
  private applyFade(audio: HTMLAudioElement, duration: number, fadeIn: boolean, callback?: () => void) {
    if (!this.audioContext) {
      callback?.();
      return;
    }

    // 保存原来的音量
    const originalVolume = audio.volume;
    const gainNode = this.audioContext.createGain();

    // 断开原来的连接（如果有的话）
    audio.disconnect();
    audio.volume = 0;
    audio.connect(gainNode);
    gainNode.connect(this.audioContext.destination);

    const startVol = fadeIn ? 0 : 1;
    const endVol = fadeIn ? 1 : 0;

    gainNode.gain.setValueAtTime(startVol, this.audioContext.currentTime);
    gainNode.gain.linearRampToValueAtTime(endVol, this.audioContext.currentTime + duration);

    setTimeout(() => {
      if (!fadeIn) {
        audio.pause();
      }

      // 断开gainNode连接
      gainNode.disconnect();

      // 重新连接audio到destination（如果需要继续使用的话）
      if (fadeIn) {
        audio.volume = originalVolume;
        audio.disconnect();
        audio.connect(this.audioContext.destination);
      }

      callback?.();
    }, duration * 1000);
  }

  // 停止指定任务的音频
  stopAudio(taskId: string) {
    const player = this.activePlayers.get(taskId);
    if (player) {
      if (player instanceof HTMLAudioElement) {
        player.pause();
        player.src = '';
      } else {
        player.disconnect();
      }
      this.activePlayers.delete(taskId);
      this.playbackStates.delete(taskId);
    }
  }

  // 停止所有音频
  stopAllAudio() {
    this.activePlayers.forEach((player, id) => {
      if (player instanceof HTMLAudioElement) {
        player.pause();
        player.src = '';
      } else {
        player.disconnect();
      }
    });
    this.activePlayers.clear();
    this.playbackStates.clear();
  }

  // 获取音频状态
  getAudioState(): string | undefined {
    return this.audioContext?.state;
  }

  // 检查是否已初始化
  isInitialized(): boolean {
    return this.initialized;
  }
}

export default AudioManager;