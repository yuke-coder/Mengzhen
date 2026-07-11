"use client";

/**
 * 播放状态持久化管理器
 * 保存和恢复音频播放状态
 */

interface AudioPlaybackState {
  currentTime: number;
  volume: number;
  isPlaying: boolean;
  taskId?: string;
  timestamp: number;
}

const STORAGE_KEY = 'audio-playback-states';
const states = new Map<string, AudioPlaybackState>();
let initialized = false;

/**
 * 加载保存的状态
 */
function loadSavedStates(): void {
  if (initialized) return;
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      const data = JSON.parse(saved) as [string, AudioPlaybackState][];
      data.forEach(([taskId, state]) => {
        states.set(taskId, state);
      });
    }
    initialized = true;
  } catch (error) {
    console.error('[AudioState] 加载状态失败:', error);
  }
}

/**
 * 保存状态到 localStorage
 */
function saveToStorage(): void {
  try {
    const data = Array.from(states.entries());
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch (error) {
    console.error('[AudioState] 保存状态失败:', error);
  }
}

/**
 * 保存播放状态
 */
export function savePlaybackState(
  taskId: string,
  currentTime: number,
  volume: number,
  isPlaying: boolean
): void {
  loadSavedStates();
  const state: AudioPlaybackState = {
    currentTime,
    volume,
    isPlaying,
    taskId,
    timestamp: Date.now()
  };
  states.set(taskId, state);
  saveToStorage();
}

/**
 * 获取播放状态
 */
export function getPlaybackState(taskId: string): AudioPlaybackState | undefined {
  loadSavedStates();
  return states.get(taskId);
}

/**
 * 恢复任务播放状态
 */
export function restoreTaskPlayback(
  taskId: string,
  audioElement: HTMLAudioElement
): boolean {
  loadSavedStates();
  const state = states.get(taskId);
  if (!state) return false;

  try {
    audioElement.currentTime = state.currentTime;
    audioElement.volume = state.volume;
    if (state.isPlaying) {
      audioElement.play().catch(() => {});
    }
    return true;
  } catch (error) {
    console.warn('[AudioState] 恢复播放状态失败:', error);
    return false;
  }
}

/**
 * 清理过期状态
 */
export function cleanupExpiredStates(maxAgeMs: number = 3600000): void {
  loadSavedStates();
  const now = Date.now();
  for (const [taskId, state] of states.entries()) {
    if (now - state.timestamp > maxAgeMs) {
      states.delete(taskId);
    }
  }
  saveToStorage();
}

/**
 * 清除所有状态
 */
export function clearAllStates(): void {
  loadSavedStates();
  states.clear();
  saveToStorage();
}

/**
 * 清除指定任务状态
 */
export function clearTaskState(taskId: string): void {
  loadSavedStates();
  states.delete(taskId);
  saveToStorage();
}

