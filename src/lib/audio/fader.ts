"use client";

/**
 * 音量淡入淡出管理器
 */

interface FadeOptions {
  duration: number;
  targetVolume: number;
  onComplete?: () => void;
}

interface FadeState {
  timeout: ReturnType<typeof setTimeout> | null;
}

const fadeState: FadeState = {
  timeout: null
};

/**
 * 停止当前的淡入淡出
 */
export function stopFade(): void {
  if (fadeState.timeout) {
    clearTimeout(fadeState.timeout);
    fadeState.timeout = null;
  }
}

/**
 * 设置音频音量
 */
export function setVolume(audio: HTMLAudioElement, volume: number): void {
  audio.volume = Math.max(0, Math.min(1, volume));
}

/**
 * 淡入效果
 */
export function fadeIn(
  audio: HTMLAudioElement,
  { duration, targetVolume, onComplete }: FadeOptions,
  alreadyElapsed: number = 0
): void {
  stopFade();
  const start = Date.now();
  const totalDuration = duration * 1000;

  setVolume(audio, 0);
  audio.play().catch(() => {});

  const tick = () => {
    const elapsed = Date.now() - start + alreadyElapsed;
    const progress = Math.min(1, elapsed / totalDuration);
    const currentVol = targetVolume * progress;
    setVolume(audio, currentVol);

    if (progress >= 1) {
      console.log('[Fader] 淡入完成，音量:', currentVol);
      onComplete?.();
    } else {
      fadeState.timeout = setTimeout(tick, 50);
    }
  };

  tick();
}

/**
 * 淡出效果
 */
export function fadeOut(
  audio: HTMLAudioElement,
  { duration, targetVolume = 0, onComplete }: FadeOptions,
  alreadyElapsed: number = 0
): void {
  stopFade();
  const start = Date.now();
  const totalDuration = duration * 1000;
  const startVol = audio.volume;

  console.log('[Fader] 开始淡出，初始音量:', startVol, '目标音量:', targetVolume, '时长:', duration);

  const tick = () => {
    const elapsed = Date.now() - start + alreadyElapsed;
    const progress = Math.min(1, elapsed / totalDuration);
    const currentVol = startVol + (targetVolume - startVol) * progress;
    setVolume(audio, currentVol);

    if (progress >= 1) {
      console.log('[Fader] 淡出完成，最终音量:', currentVol);
      onComplete?.();
    } else {
      fadeState.timeout = setTimeout(tick, 50);
    }
  };

  tick();
}

