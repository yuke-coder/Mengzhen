"use client";

/**
 * Service Worker 调度器桥接模块
 *
 * 作为原生 AlarmScheduler 的 fallback：
 * 1. 将任务数据同步到 Service Worker
 * 2. SW 在后台设置定时器 + 弹通知兜底
 */

import { ScheduledTask, getNextExecuteDate } from './task-types';
import { getAllTasks } from './task-store';

interface SwTaskPayload {
  id: string;
  name: string;
  nextExecAt: number;
  playDurationMinutes: number;
}

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
 * 监听来自 SW 的消息（通知点击、SW 定时触发等）
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
