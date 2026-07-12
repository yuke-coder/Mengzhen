'use client';

/**
 * 浏览器原生权限管理器
 */

export type PermissionType = 'notifications' | 'persistent-storage';

export interface PermissionStatus {
  granted: boolean;
  needsAction: boolean;
}

const checkSupport = (api: string): boolean => typeof window !== 'undefined' && api in navigator;

/**
 * 检测单个权限状态
 */
export async function checkPermission(name: PermissionType): Promise<PermissionStatus | null> {
  try {
    if (!checkSupport('permissions')) return null;
    const result = await navigator.permissions.query({ name: name as PermissionName });
    return { granted: result.state === 'granted', needsAction: result.state === 'prompt' };
  } catch {
    console.log(`[Permission] ${name} not supported`);
    return null;
  }
}

/**
 * 请求通知权限
 */
export async function requestNotificationPermission(): Promise<boolean> {
  if (!('Notification' in window)) {
    console.warn('[Permission] Notification API not supported');
    return false;
  }
  try {
    console.log('[Permission] Requesting notification permission...');
    const result = await Notification.requestPermission();
    console.log('[Permission] Notification permission result:', result);
    return result === 'granted';
  } catch (error) {
    console.error('[Permission] Notification permission error:', error);
    return false;
  }
}

/**
 * 请求持久化存储权限
 */
export async function requestPersistentStorage(): Promise<boolean> {
  try {
    if (checkSupport('storage') && 'persist' in navigator.storage) {
      console.log('[Permission] Requesting persistent storage...');
      const result = await navigator.storage.persist();
      console.log('[Permission] Persistent storage result:', result);
      return result;
    }
    console.warn('[Permission] Storage API not supported');
    return false;
  } catch (error) {
    console.error('[Permission] Persistent storage error:', error);
    return false;
  }
}

/**
 * 检查所有核心权限状态
 */
export async function checkAllPermissions(): Promise<{
  notifications: PermissionStatus | null;
  persistentStorage: PermissionStatus | null;
  needsAnyPermission: boolean;
}> {
  const notifications = await checkPermission('notifications');
  const persistentStorage = await checkPermission('persistent-storage');
  return {
    notifications,
    persistentStorage,
    needsAnyPermission: notifications?.needsAction || persistentStorage?.needsAction || false
  };
}
