"use client";

// 音频调试工具
export class AudioDebug {
  static log(...args: any[]) {
    if (process.env.NODE_ENV === 'development') {
      console.log('[🎵 Audio Debug]', ...args);
    }
  }

  static error(...args: any[]) {
    console.error('[🎵 Audio Error]', ...args);
  }

  // 检查浏览器音频权限
  static checkPermissions(): boolean {
    const hasAudio = 'AudioContext' in window || 'webkitAudioContext' in window;
    this.log('浏览器支持音频:', hasAudio);
    return hasAudio;
  }

  // 检查AudioContext状态
  static async checkAudioContextState(): Promise<string | undefined> {
    try {
      const AudioContext = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioContext) return undefined;

      const context = new AudioContext();
      const state = context.state;
      await context.close();
      this.log('AudioContext状态:', state);
      return state;
    } catch (error) {
      this.error('检查AudioContext失败:', error);
      return undefined;
    }
  }

  // 强制解锁音频
  static async forceUnlock(): Promise<boolean> {
    this.log('开始强制解锁音频...');

    try {
      // 方法1: AudioContext
      const AudioContext = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioContext) {
        const context = new AudioContext();
        if (context.state === 'suspended') {
          await context.resume();
          this.log('AudioContext恢复成功');
          return true;
        }
      }

      // 方法2: HTML5 Audio
      const audio = new Audio();
      audio.volume = 0;
      audio.muted = true;
      audio.src = 'data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIG2m98OScTgwOUarm7blmGg15k9n1unEiBC13yO/eizEIHWq+8+OWQ';

      await audio.play();
      audio.pause();
      audio.src = '';
      this.log('HTML5 Audio解锁成功');
      return true;
    } catch (error) {
      this.error('强制解锁失败:', error);
      return false;
    }
  }

  // 测试音频播放
  static async testAudioPlayback(audioUrl: string): Promise<boolean> {
    this.log('测试音频播放:', audioUrl);

    try {
      const audio = new Audio(audioUrl);
      audio.volume = 0.1;

      await audio.play();
      audio.pause();
      this.log('音频播放测试成功');
      return true;
    } catch (error) {
      this.error('音频播放测试失败:', error);
      return false;
    }
  }

  // 监听用户交互事件
  static setupUserInteractionListener(callback: () => void) {
    const events = ['click', 'touchstart', 'keydown', 'scroll'];

    const handler = () => {
      callback();
      events.forEach(event => document.removeEventListener(event, handler));
    };

    events.forEach(event => {
      document.addEventListener(event, handler, { once: true, passive: true });
    });

    return () => {
      events.forEach(event => document.removeEventListener(event, handler));
    };
  }
}

// 全局调试函数
if (typeof window !== 'undefined') {
  (window as any).audioDebug = AudioDebug;
}

export default AudioDebug;