"use client";

/**
 * Service Worker 调度器桥接模块
 *
 * 解决息屏/后台时浏览器节流 setTimeout 的问题：
 * 1. 将任务数据同步到 Service Worker
 * 2. SW 在后台设置定时器 + 弹通知兜底
 * 3. 主线程用 WakeLock 临近保活策略确保精确触发
 */

import { ScheduledTask, getNextExecuteDate } from './task-types';
import { getAllTasks } from './task-store';

interface SwTaskPayload {
  id: string;
  name: string;
  nextExecAt: number;
  playDurationMinutes: number;
}

// 临近保活窗口（任务到期前多少毫秒开始保活）
const PRE_WAKE_WINDOW_MS = 2 * 60 * 1000; // 2 分钟
// WakeLock 实例
let wakeLock: WakeLockSentinel | null = null;
let wakeLockTimer: ReturnType<typeof setTimeout> | null = null;
// 保活状态
let keepAliveActive = false;
// 唤醒回调
let onWakeCallback: (() => void) | null = null;
// SW 是否已就绪
let swReady = false;

function getSWRegistration(): Promise<ServiceWorkerRegistration | null> {
  if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) {
    return Promise.resolve(null);
  }
  return navigator.serviceWorker.ready;
}

/**
 * 将当前所有任务同步到 Service Worker
 */
export async function syncTasksToSW(): Promise<void> {
  const reg = await getSWRegistration();
  if (!reg || !reg.active) return;

  const tasks = getAllTasks();
  const now = Date.now();
  const swTasks: SwTaskPayload[] = [];

  for (const task of tasks) {
    if (task.status === 'cancelled' || task.status === 'completed') continue;
    if (task.skipUntil && now < task.skipUntil) continue;

    const nextExec = getNextExecuteDate(task);
    if (!nextExec) continue;

    const nextExecAt = nextExec.getTime();
    // 只同步未来 24 小时内的任务
    if (nextExecAt < now) continue;
    if (nextExecAt > now + 24 * 60 * 60 * 1000) continue;

    swTasks.push({
      id: task.id,
      name: task.name,
      nextExecAt,
      playDurationMinutes: task.playDurationMinutes,
    });
  }

  reg.active.postMessage({
    type: 'SYNC_TASKS',
    payload: { tasks: swTasks },
  });

  // 尝试注册 Periodic Background Sync
  try {
    if ('periodicSync' in reg) {
      reg.active.postMessage({ type: 'REGISTER_SYNC' });
    }
  } catch {
    // 不支持，忽略
  }

  swReady = true;
}

/**
 * 从 SW 移除单个任务
 */
export async function removeTaskFromSW(taskId: string): Promise<void> {
  const reg = await getSWRegistration();
  if (!reg?.active) return;
  reg.active.postMessage({
    type: 'REMOVE_TASK',
    payload: { taskId },
  });
}

/**
 * 清除 SW 中所有任务
 */
export async function clearSWTasks(): Promise<void> {
  const reg = await getSWRegistration();
  if (!reg?.active) return;
  reg.active.postMessage({ type: 'CLEAR_ALL' });
}

/**
 * 启动临近保活策略
 * 当最近的任务即将在 PRE_WAKE_WINDOW_MS 内执行时，请求 WakeLock 防止设备休眠
 *
 * @param onWake 任务到期时的回调
 * @returns 清理函数
 */
export function startProximityKeepAlive(onWake: () => void): () => void {
  onWakeCallback = onWake;
  let checkTimer: ReturnType<typeof setInterval> | null = null;

  const checkAndWake = async () => {
    if (keepAliveActive) return; // 已经在保活中

    const tasks = getAllTasks();
    const now = Date.now();
    let nearestExecAt = Infinity;

    for (const task of tasks) {
      if (task.status === 'cancelled' || task.status === 'completed') continue;
      if (task.skipUntil && now < task.skipUntil) continue;

      const nextExec = getNextExecuteDate(task);
      if (!nextExec) continue;

      const execAt = nextExec.getTime();
      // 考虑渐入时间
      const fadeInMs = task.enableFade ? (task.fadeInDuration || 0) * 1000 : 0;
      const audioStartAt = execAt - fadeInMs;

      if (audioStartAt < nearestExecAt) {
        nearestExecAt = audioStartAt;
      }
    }

    if (nearestExecAt === Infinity) return;

    const delay = nearestExecAt - now;

    if (delay <= PRE_WAKE_WINDOW_MS && delay > 0) {
      // 临近！开始保活
      await engageKeepAlive(nearestExecAt);
    }
  };

  // 每 30 秒检查一次
  checkTimer = setInterval(checkAndWake, 30000);
  // 立即检查一次
  checkAndWake();

  return () => {
    if (checkTimer) clearInterval(checkTimer);
    disengageKeepAlive();
    onWakeCallback = null;
  };
}

async function engageKeepAlive(targetTime: number): Promise<void> {
  if (keepAliveActive) return;

  try {
    if ('wakeLock' in navigator) {
      wakeLock = await navigator.wakeLock.request('screen');
      keepAliveActive = true;
      console.log('[KeepAlive] WakeLock 已获取，保活至', new Date(targetTime).toLocaleTimeString());

      wakeLock.addEventListener('release', () => {
        keepAliveActive = false;
        wakeLock = null;
        console.log('[KeepAlive] WakeLock 已释放');
      });
    }
  } catch (e) {
    console.warn('[KeepAlive] WakeLock 获取失败:', e);
    // 降级：不获取 WakeLock，但仍尝试用其他策略
  }

  // 设置精确的唤醒定时器
  const delay = Math.max(100, targetTime - Date.now());
  if (wakeLockTimer) clearTimeout(wakeLockTimer);

  wakeLockTimer = setTimeout(() => {
    console.log('[KeepAlive] 保活定时器触发，执行回调');
    onWakeCallback?.();
    // 任务开始后 2 秒释放 WakeLock（音频已开始播放，MediaSession 会保持）
    setTimeout(() => {
      disengageKeepAlive();
    }, 2000);
  }, delay);
}

function disengageKeepAlive(): void {
  if (wakeLockTimer) {
    clearTimeout(wakeLockTimer);
    wakeLockTimer = null;
  }
  if (wakeLock) {
    wakeLock.release().catch(() => {});
    wakeLock = null;
  }
  keepAliveActive = false;
}

/**
 * 监听来自 SW 的消息（通知点击、SW 定时触发等）
 *
 * @param onTrigger 收到 SW 触发信号时的回调
 * @returns 清理函数
 */
export function listenSWMessages(
  onTrigger: (taskId: string) => void
): () => void {
  if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) {
    return () => {};
  }

  const handler = (event: MessageEvent) => {
    const { type, taskId } = event.data || {};
    if (type === 'SW_TASK_TRIGGER' || type === 'SW_NOTIFICATION_CLICK') {
      if (taskId) {
        onTrigger(taskId);
      }
    }
  };

  navigator.serviceWorker.addEventListener('message', handler);

  return () => {
    navigator.serviceWorker.removeEventListener('message', handler);
  };
}

/**
 * 检查 SW 调度器是否可用
 */
export function isSWSchedulerAvailable(): boolean {
  return typeof navigator !== 'undefined'
    && 'serviceWorker' in navigator
    && swReady;
}

/**
 * 获取保活状态
 */
export function getKeepAliveStatus(): {
  active: boolean;
  hasWakeLock: boolean;
} {
  return {
    active: keepAliveActive,
    hasWakeLock: wakeLock !== null,
  };
}
