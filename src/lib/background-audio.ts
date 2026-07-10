"use client";

// 精简版安卓后台音频播放服务
export class BackgroundAudioService {
  private wakeLock: WakeLockSentinel | null = null;
  private keepAliveTimer: number | null = null;
  private audioContext: AudioContext | null = null;
  private isInitialized = false;

  // 手动初始化AudioContext（必须在用户交互后调用）
  async initializeAudioContext(): Promise<boolean> {
    if (this.audioContext && this.audioContext.state !== 'suspended') {
      return true;
    }

    try {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      await this.audioContext.resume();
      this.isInitialized = true;
      this.setupVisibilityHandler();
      this.startKeepAlive();
      return true;
    } catch (error) {
      console.error('[BG Audio] 初始化失败:', error);
      return false;
    }
  }

  // 自动初始化（如果已经有用户交互）
  async init() {
    if (this.isInitialized) return;

    // 只有在document.hasFocus()时才自动初始化
    if (!document.hasFocus()) {
      console.log('[BG Audio] 页面未获得焦点，延迟初始化');
      return;
    }

    await this.initializeAudioContext();
  }

  // 销毁服务
  destroy() {
    if (this.keepAliveTimer) clearInterval(this.keepAliveTimer);
    this.releaseWakeLock();
    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
    this.isInitialized = false;
  }

  // 请求Wake Lock
  async requestWakeLock(): Promise<boolean> {
    if (!('wakeLock' in navigator)) return false;

    // 确保AudioContext已经初始化
    if (!this.audioContext) {
      await this.initializeAudioContext();
    }

    try {
      this.wakeLock = await navigator.wakeLock.request('screen');
      this.wakeLock.addEventListener('release', () => (this.wakeLock = null));
      return true;
    } catch (error) {
      console.warn('[BG Audio] Wake Lock失败:', error);
      return false;
    }
  }

  // 释放Wake Lock
  private releaseWakeLock() {
    if (this.wakeLock) {
      this.wakeLock.release().catch(() => {});
      this.wakeLock = null;
    }
  }

  // 页面可见性处理
  private setupVisibilityHandler() {
    document.addEventListener('visibilitychange', async () => {
      if (document.visibilityState === 'visible' && this.audioContext?.state === 'suspended') {
        await this.audioContext.resume();
      }
    });
  }

  // 启动保活机制
  private startKeepAlive() {
    if (this.keepAliveTimer) clearInterval(this.keepAliveTimer);

    this.keepAliveTimer = window.setInterval(() => {
      if (this.audioContext?.state === 'suspended') {
        this.audioContext.resume().catch(() => {});
      }
      this.playSilentBeep();
    }, 15000);
  }

  // 播放极短静音片段
  private playSilentBeep() {
    try {
      if (!this.audioContext || this.audioContext.state !== 'running') return;

      const osc = this.audioContext.createOscillator();
      const gain = this.audioContext.createGain();
      gain.gain.value = 0;
      osc.connect(gain);
      gain.connect(this.audioContext.destination);
      osc.start(this.audioContext.currentTime);
      osc.stop(this.audioContext.currentTime + 0.05);
    } catch {}
  }

  // 获取AudioContext状态
  getAudioState(): string | undefined {
    return this.audioContext?.state;
  }

  // 工具方法
  static isAndroidDevice(): boolean {
    return /android/i.test(navigator.userAgent.toLowerCase());
  }

  static isUserInteraction(): boolean {
    return document.hasFocus() && (navigator as any).userActivation?.hasBeenActive;
  }
}

export default BackgroundAudioService;