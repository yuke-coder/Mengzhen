import type { ScheduledTask } from './task-types';
import { getNextExecuteDate } from './task-types';
import { getAllTasks } from './task-store';

/**
 * Capacitor 原生调度器接口
 * 仅在 Android 原生壳内可用，提供息屏定时启播能力
 */

interface AlarmSchedulerPlugin {
  scheduleTask(options: {
    taskId: string;
    taskName: string;
    triggerAt: number;
    playDurationMinutes: number;
    volume: number;
    enableFade: boolean;
    fadeInDuration: number;
    fadeOutDuration: number;
    audioUrl: string;
    audioName: string;
  }): Promise<void>;

  cancelTask(options: { taskId: string }): Promise<void>;

  cancelAllTasks(): Promise<void>;

  stopPlayback(): Promise<void>;

  isPlaying(): Promise<{ playing: boolean }>;
}

/**
 * 检测是否在 Capacitor 原生环境中运行
 */
export function isNativeEnvironment(): boolean {
  return typeof window !== 'undefined'
    && typeof (window as any).Capacitor !== 'undefined'
    && (window as any).Capacitor.isNative === true;
}

/**
 * 获取 AlarmScheduler 插件实例
 */
function getAlarmScheduler(): AlarmSchedulerPlugin | null {
  if (!isNativeEnvironment()) return null;
  const Capacitor = (window as any).Capacitor;
  return Capacitor.Plugins?.AlarmScheduler ?? null;
}

/**
 * 获取任务的实际音频 URL（转为绝对路径，供原生 MediaPlayer 使用）
 */
function getAudioUrl(task: ScheduledTask): string {
  const baseUrl = isNativeEnvironment()
    ? (window as any).Capacitor?.getServerUrl?.() ?? 'https://mengzhen-chi.vercel.app'
    : '';
  for (const audio of task.audios) {
    if (audio.serverUrl && audio.serverUrl.trim() !== '') {
      return audio.serverUrl.startsWith('http')
        ? audio.serverUrl
        : `${baseUrl}${audio.serverUrl}`;
    }
    if (audio.fileKey && audio.fileKey.trim() !== '') {
      return `${baseUrl}/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}`;
    }
  }
  return '';
}

/**
 * 将所有未来任务同步到原生 AlarmScheduler
 */
export async function syncTasksToNative(): Promise<void> {
  const plugin = getAlarmScheduler();
  if (!plugin) return;

  try {
    // 先清除所有旧的闹钟
    await plugin.cancelAllTasks();

    const tasks = getAllTasks();
    const now = Date.now();

    for (const task of tasks) {
      if (task.status === 'cancelled' || task.status === 'completed') continue;
      if (task.skipUntil && now < task.skipUntil) continue;

      const nextExec = getNextExecuteDate(task);
      if (!nextExec) continue;

      const triggerAt = nextExec.getTime();
      if (triggerAt < now) continue; // 只同步未来任务

      const audioUrl = getAudioUrl(task);
      if (!audioUrl) continue;

      await plugin.scheduleTask({
        taskId: task.id,
        taskName: task.name,
        triggerAt,
        playDurationMinutes: task.playDurationMinutes,
        volume: task.volume ?? 70,
        enableFade: task.enableFade ?? false,
        fadeInDuration: task.fadeInDuration ?? 0,
        fadeOutDuration: task.fadeOutDuration ?? 0,
        audioUrl,
        audioName: task.audios[0]?.name ?? '',
      });
    }
  } catch (e) {
    console.error('[NativeScheduler] sync failed:', e);
  }
}

/**
 * 从原生 AlarmScheduler 中移除单个任务
 */
export async function removeNativeTask(taskId: string): Promise<void> {
  const plugin = getAlarmScheduler();
  if (!plugin) return;
  try {
    await plugin.cancelTask({ taskId });
  } catch (e) {
    console.error('[NativeScheduler] remove failed:', e);
  }
}

/**
 * 停止原生播放
 */
export async function stopNativePlayback(): Promise<void> {
  const plugin = getAlarmScheduler();
  if (!plugin) return;
  try {
    await plugin.stopPlayback();
  } catch (e) {
    console.error('[NativeScheduler] stop failed:', e);
  }
}

/**
 * 检查原生是否正在播放
 */
export async function isNativePlaying(): Promise<boolean> {
  const plugin = getAlarmScheduler();
  if (!plugin) return false;
  try {
    const result = await plugin.isPlaying();
    return result.playing;
  } catch {
    return false;
  }
}
