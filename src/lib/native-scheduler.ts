import type { ScheduledTask } from './task-types';
import { getNextExecuteDate } from './task-types';
import { getAllTasks } from './task-store';
import { resolveTracksJsonFromTaskAudios } from './audio/resolve';

/**
 * Capacitor 原生调度器
 * 仅在 Android 原生壳内可用
 * 所有播放通过 AudioPlaybackService.java 处理
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
    tracksJson: string;
    loopSingle: boolean;
    endTime: number;
  }): Promise<void>;
  cancelTask(options: { taskId: string }): Promise<void>;
  cancelAllTasks(): Promise<void>;
  stopPlayback(): Promise<void>;
  isPlaying(): Promise<{ playing: boolean; trackIndex: number }>;
  playNow(options: {
    taskId: string;
    taskName: string;
    playDurationMinutes: number;
    volume: number;
    enableFade: boolean;
    fadeInDuration: number;
    fadeOutDuration: number;
    audioUrl: string;
    audioName: string;
    tracksJson: string;
    loopSingle: boolean;
    endTime: number;
  }): Promise<void>;
}

export function isNativeEnvironment(): boolean {
  return typeof window !== 'undefined'
    && typeof (window as any).Capacitor !== 'undefined'
    && (window as any).Capacitor.isNativePlatform?.() === true;
}

function getAlarmScheduler(): AlarmSchedulerPlugin | null {
  if (!isNativeEnvironment()) return null;
  const Capacitor = (window as any).Capacitor;
  return Capacitor.Plugins?.AlarmScheduler ?? null;
}

/**
 * 将所有未来任务同步到原生 AlarmScheduler
 */
export async function syncTasksToNative(): Promise<void> {
  const plugin = getAlarmScheduler();
  if (!plugin) return;

  try {
    await plugin.cancelAllTasks();

    const tasks = getAllTasks();
    const now = Date.now();

    for (const task of tasks) {
      if (task.status === 'cancelled' || task.status === 'completed') continue;
      if (task.skipUntil && now < task.skipUntil) continue;

      const nextExec = getNextExecuteDate(task);
      if (!nextExec) continue;

      const triggerAt = nextExec.getTime();
      if (triggerAt < now) continue;

      const { tracksJson, firstUrl, firstName } = await resolveTracksJsonFromTaskAudios(task.audios);
      if (!tracksJson && !firstUrl) continue;

      const endTime = triggerAt + task.playDurationMinutes * 60000;

      await plugin.scheduleTask({
        taskId: task.id,
        taskName: task.name,
        triggerAt,
        playDurationMinutes: task.playDurationMinutes,
        volume: task.volume ?? 70,
        enableFade: task.enableFade ?? false,
        fadeInDuration: task.fadeInDuration ?? 0,
        fadeOutDuration: task.fadeOutDuration ?? 0,
        audioUrl: firstUrl,
        audioName: firstName,
        tracksJson,
        loopSingle: true,
        endTime,
      });
    }
    console.log('[NativeScheduler] 任务同步完成');
  } catch (e) {
    console.error('[NativeScheduler] sync failed:', e);
  }
}

/**
 * 立即触发原生播放
 */
export async function triggerNativePlayback(taskId: string): Promise<void> {
  const plugin = getAlarmScheduler();
  if (!plugin) return;

  const task = getAllTasks().find(t => t.id === taskId);
  if (!task) return;

  const { tracksJson, firstUrl, firstName } = await resolveTracksJsonFromTaskAudios(task.audios);
  if (!tracksJson && !firstUrl) {
    console.error('[NativeScheduler] 无法解析音频 URL');
    return;
  }

  const endTime = Date.now() + task.playDurationMinutes * 60000;

  try {
    await plugin.playNow({
      taskId: task.id,
      taskName: task.name,
      playDurationMinutes: task.playDurationMinutes,
      volume: task.volume ?? 70,
      enableFade: task.enableFade ?? false,
      fadeInDuration: task.fadeInDuration ?? 0,
      fadeOutDuration: task.fadeOutDuration ?? 0,
      audioUrl: firstUrl,
      audioName: firstName,
      tracksJson,
      loopSingle: true,
      endTime,
    });
    console.log('[NativeScheduler] 原生播放已启动:', taskId);
  } catch (e) {
    console.error('[NativeScheduler] triggerPlayback failed:', e);
  }
}

export async function removeNativeTask(taskId: string): Promise<void> {
  const plugin = getAlarmScheduler();
  if (!plugin) return;
  try {
    await plugin.cancelTask({ taskId });
  } catch (e) {
    console.error('[NativeScheduler] remove failed:', e);
  }
}

export async function stopNativePlayback(): Promise<void> {
  const plugin = getAlarmScheduler();
  if (!plugin) return;
  try {
    await plugin.stopPlayback();
  } catch (e) {
    console.error('[NativeScheduler] stop failed:', e);
  }
}

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
