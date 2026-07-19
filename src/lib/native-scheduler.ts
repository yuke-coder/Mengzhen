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
  }): Promise<void>;
}

/**
 * 检测是否在 Capacitor 原生环境中运行
 */
export function isNativeEnvironment(): boolean {
  return typeof window !== 'undefined'
    && typeof (window as any).Capacitor !== 'undefined'
    && (window as any).Capacitor.isNativePlatform?.() === true;
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
 * Supabase 项目 URL（和 .env.local 一致）
 */
const SUPABASE_URL = 'https://br-epic-clam-5a2fd709.supabase2.aidap-global.cn-beijing.volces.com';
const AUDIO_BUCKET = 'audios';

/**
 * 获取音频的直链 URL
 * 优先 serverUrl，否则用 fileKey 构造 Supabase 公开 URL
 */
async function getAudioUrl(task: ScheduledTask): Promise<string> {
  for (const audio of task.audios) {
    console.log('[NativeScheduler] audio:', JSON.stringify({ name: audio.name, fileKey: audio.fileKey, serverUrl: audio.serverUrl, dbKey: audio.dbKey, pendingUploadKey: audio.pendingUploadKey }));
    if (audio.serverUrl && audio.serverUrl.trim() !== '' && audio.serverUrl.startsWith('http')) {
      console.log('[NativeScheduler] Using serverUrl:', audio.serverUrl);
      return audio.serverUrl;
    }
    if (audio.fileKey && audio.fileKey.trim() !== '') {
      // 直接构造 Supabase Storage 公开 URL
      const publicUrl = `${SUPABASE_URL}/storage/v1/object/public/${AUDIO_BUCKET}/${audio.fileKey}`;
      console.log('[NativeScheduler] Using public URL:', publicUrl);
      return publicUrl;
    }
  }
  console.warn('[NativeScheduler] No audio URL found for task', task.id, 'audios:', task.audios.length);
  return '';
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

      const audioUrl = await getAudioUrl(task);
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
 * 立即触发原生播放（用于"立即执行"按钮）
 */
export async function triggerNativePlayback(taskId: string): Promise<void> {
  const plugin = getAlarmScheduler();
  if (!plugin) return;
  const task = getAllTasks().find(t => t.id === taskId);
  if (!task) return;
  const audioUrl = await getAudioUrl(task);
  if (!audioUrl) {
    console.error('[NativeScheduler] No audio URL for task', taskId);
    return;
  }
  try {
    await plugin.playNow({
      taskId: task.id,
      taskName: task.name,
      playDurationMinutes: task.playDurationMinutes,
      volume: task.volume ?? 70,
      enableFade: task.enableFade ?? false,
      fadeInDuration: task.fadeInDuration ?? 0,
      fadeOutDuration: task.fadeOutDuration ?? 0,
      audioUrl,
      audioName: task.audios[0]?.name ?? '',
    });
  } catch (e) {
    console.error('[NativeScheduler] triggerPlayback failed:', e);
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
