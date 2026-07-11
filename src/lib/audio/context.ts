"use client";

/**
 * 统一 AudioContext 管理器
 * 处理音频上下文的初始化、恢复和销毁
 */

let audioContext: AudioContext | null = null;
let initializationPromise: Promise<boolean> | null = null;

/**
 * 获取或创建 AudioContext 实例
 */
export function getAudioContext(): AudioContext | null {
  if (!audioContext) {
    try {
      const Context = window.AudioContext || (window as any).webkitAudioContext;
      if (Context) {
        audioContext = new Context();
        console.log('[AudioContext] 创建成功');
      }
    } catch (error) {
      console.error('[AudioContext] 创建失败:', error);
    }
  }
  return audioContext;
}

/**
 * 初始化 AudioContext（需要用户交互后调用）
 */
export async function initializeAudioContext(): Promise<boolean> {
  if (initializationPromise) {
    return initializationPromise;
  }

  initializationPromise = (async () => {
    const ctx = getAudioContext();
    if (!ctx) return false;

    if (ctx.state === 'running') {
      console.log('[AudioContext] 已运行');
      return true;
    }

    try {
      await ctx.resume();
      console.log('[AudioContext] 初始化成功，状态:', ctx.state);
      return true;
    } catch (error) {
      console.error('[AudioContext] 初始化失败:', error);
      return false;
    }
  })();

  return initializationPromise;
}

/**
 * 恢复 AudioContext（当被暂停时）
 */
export async function resumeAudioContext(): Promise<boolean> {
  const ctx = audioContext;
  if (!ctx) return false;

  if (ctx.state === 'suspended') {
    try {
      await ctx.resume();
      console.log('[AudioContext] 已恢复');
      return true;
    } catch (error) {
      console.error('[AudioContext] 恢复失败:', error);
      return false;
    }
  }

  return ctx.state === 'running';
}

/**
 * 获取 AudioContext 状态
 */
export function getAudioState(): string | undefined {
  return audioContext?.state;
}

/**
 * 销毁 AudioContext
 */
export function destroyAudioContext(): void {
  if (audioContext) {
    try {
      audioContext.close();
      console.log('[AudioContext] 已销毁');
    } catch (error) {
      console.error('[AudioContext] 销毁失败:', error);
    }
    audioContext = null;
    initializationPromise = null;
  }
}

/**
 * 尝试解锁音频播放（处理浏览器自动播放策略）
 */
export function tryUnlockAudio(): void {
  const ctx = getAudioContext();
  if (!ctx) return;

  // 尝试恢复上下文
  resumeAudioContext().catch(() => {});

  // 创建一个临时的静音音频元素尝试播放
  try {
    const unlockAudio = new Audio();
    unlockAudio.volume = 0;
    unlockAudio.muted = true;
    unlockAudio.play().catch(() => {}).finally(() => {
      setTimeout(() => {
        unlockAudio.pause();
        unlockAudio.src = '';
      }, 100);
    });
  } catch {}
}

