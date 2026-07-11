"use client";

/**
 * 统一 WakeLock 管理器
 * 处理设备唤醒锁，防止屏幕熄灭
 */

let wakeLock: WakeLockSentinel | null = null;

/**
 * 请求 WakeLock
 */
export async function requestWakeLock(): Promise<boolean> {
  if (!('wakeLock' in navigator)) {
    console.log('[WakeLock] 浏览器不支持');
    return false;
  }

  if (wakeLock) {
    return true;
  }

  try {
    wakeLock = await navigator.wakeLock.request('screen');
    wakeLock.addEventListener('release', () => {
      console.log('[WakeLock] 已释放');
      wakeLock = null;
    });
    console.log('[WakeLock] 获取成功');
    return true;
  } catch (error) {
    console.warn('[WakeLock] 获取失败:', error);
    return false;
  }
}

/**
 * 释放 WakeLock
 */
export function releaseWakeLock(): void {
  if (wakeLock) {
    try {
      wakeLock.release();
    } catch (error) {
      console.error('[WakeLock] 释放失败:', error);
    }
    wakeLock = null;
  }
}

/**
 * 检查 WakeLock 是否激活
 */
export function isWakeLockActive(): boolean {
  return wakeLock !== null;
}

/**
 * 检测是否是 Android 设备
 */
export function isAndroidDevice(): boolean {
  return /android/i.test(navigator.userAgent.toLowerCase());
}

