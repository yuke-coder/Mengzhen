// 音频解锁工具函数

/**
 * 尝试解锁音频上下文
 * 兼容所有浏览器的自动播放限制
 */
export async function unlockAudio(): Promise<boolean> {
  try {
    // 方法1: 使用AudioContext
    const AudioContext = window.AudioContext || (window as any).webkitAudioContext;
    if (AudioContext) {
      const context = new AudioContext();
      if (context.state === 'suspended') {
        await context.resume();
      }
      context.close();
      return true;
    }

    // 方法2: 使用HTML5 Audio元素
    const audio = new Audio();
    audio.volume = 0;
    audio.muted = true;
    audio.src = 'data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIG2m98OScTgwOUarm7blmGg15k9n1unEiBC13yO/eizEIHWq+8+OWQ';

    const result = await audio.play();
    audio.pause();
    audio.src = '';
    return !!result;
  } catch (error) {
    console.warn('音频解锁失败:', error);
    return false;
  }
}

/**
 * 监听用户交互自动解锁音频
 */
export function setupAutoUnlock() {
  const unlockEvents = ['click', 'touchstart', 'keydown', 'scroll'];
  let unlocked = false;

  const tryUnlock = async () => {
    if (unlocked) return true;
    unlocked = await unlockAudio();
    if (unlocked) {
      // 移除所有事件监听
      unlockEvents.forEach(event => {
        document.removeEventListener(event, tryUnlock);
      });
      window.dispatchEvent(new CustomEvent('audio-unlocked'));
      console.log('[AudioUnlock] 音频已通过用户交互解锁');
    }
    return unlocked;
  };

  // 添加一次性事件监听
  unlockEvents.forEach(event => {
    document.addEventListener(event, tryUnlock, { once: true, passive: true });
  });

  return () => {
    unlockEvents.forEach(event => {
      document.removeEventListener(event, tryUnlock);
    });
  };
}

/**
 * 检查音频是否已解锁
 */
export function isAudioUnlocked(): boolean {
  try {
    const AudioContext = window.AudioContext || (window as any).webkitAudioContext;
    if (AudioContext) {
      const context = new AudioContext();
      const state = context.state;
      context.close();
      return state === 'running';
    }
    return 'AudioContext' in window;
  } catch {
    return false;
  }
}

/**
 * 等待音频解锁
 */
export function waitForAudioUnlock(timeout = 10000): Promise<boolean> {
  return new Promise((resolve) => {
    if (isAudioUnlocked()) {
      return resolve(true);
    }

    const handler = () => {
      resolve(isAudioUnlocked());
      window.removeEventListener('audio-unlocked', handler);
    };

    window.addEventListener('audio-unlocked', handler);

    // 超时处理
    setTimeout(() => {
      window.removeEventListener('audio-unlocked', handler);
      resolve(false);
    }, timeout);
  });
}