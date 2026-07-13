'use client';

/**
 * PWA 安装管理器
 */

// 保存 deferredPrompt 的类型
interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

let deferredPrompt: BeforeInstallPromptEvent | null = null;
let installPromptListener: ((e: Event) => void) | null = null;
let appInstalledListener: (() => void) | null = null;
let hasPromptedThisSession = false;

/**
 * 检查是否是 PWA 模式（已经安装到桌面）
 */
export function isPwaInstalled(): boolean {
  if (typeof window !== 'undefined') {
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches;
    const isIosStandalone = (navigator as Navigator & { standalone?: boolean }).standalone === true;
    return isStandalone || isIosStandalone;
  }
  return false;
}

/**
 * 检查是否已经提示过安装（同一会话只提示一次）
 */
export function hasPromptedInstall(): boolean {
  if (typeof window === 'undefined') return false;
  return sessionStorage.getItem('pwa_prompted_session') === 'true';
}

/**
 * 标记已提示过安装
 */
export function markPromptedInstall(): void {
  if (typeof window === 'undefined') return;
  sessionStorage.setItem('pwa_prompted_session', 'true');
}

/**
 * 检查是否可以安装（浏览器支持 PWA）
 */
export function canInstallPwa(): boolean {
  if (typeof window === 'undefined') return false;
  if (isPwaInstalled()) return false;
  const isHttps = window.location.protocol === 'https:';
  const isLocalhost = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  const hasServiceWorker = 'serviceWorker' in navigator;
  return (isHttps || isLocalhost) && hasServiceWorker;
}

/**
 * 初始化 PWA 安装监听器
 */
export function initPwaInstallListener(onPromptAvailable?: () => void): () => void {
  if (typeof window === 'undefined') return () => {};
  if (isPwaInstalled()) return () => {};
  if (hasPromptedThisSession || hasPromptedInstall()) return () => {};

  // 监听 beforeinstallprompt 事件
  installPromptListener = (e: Event) => {
    console.log('[PWA] beforeinstallprompt event received');
    // 阻止默认提示，由我们主动调用prompt()来确保每次都能触发
    e.preventDefault();
    deferredPrompt = e as BeforeInstallPromptEvent;
    if (!hasPromptedThisSession && !hasPromptedInstall()) {
      hasPromptedThisSession = true;
      onPromptAvailable?.();
    }
  };

  window.addEventListener('beforeinstallprompt', installPromptListener);

  appInstalledListener = () => {
    console.log('[PWA] App installed to home screen!');
    deferredPrompt = null;
    markPromptedInstall();
  };

  window.addEventListener('appinstalled', appInstalledListener);

  return () => {
    if (installPromptListener) {
      window.removeEventListener('beforeinstallprompt', installPromptListener);
      installPromptListener = null;
    }
    if (appInstalledListener) {
      window.removeEventListener('appinstalled', appInstalledListener);
      appInstalledListener = null;
    }
  };
}

/**
 * 触发 PWA 安装提示
 */
export async function promptInstall(): Promise<boolean> {
  if (!deferredPrompt) {
    console.log('[PWA] No deferred prompt available');
    return false;
  }

  try {
    console.log('[PWA] Showing install prompt');
    await deferredPrompt.prompt();

    const choiceResult = await deferredPrompt.userChoice;
    console.log('[PWA] User choice:', choiceResult.outcome);

    if (choiceResult.outcome === 'accepted') {
      console.log('[PWA] User accepted the install prompt');
      markPromptedInstall();
      deferredPrompt = null;
      return true;
    } else {
      console.log('[PWA] User dismissed the install prompt');
      deferredPrompt = null;
      return false;
    }
  } catch (error) {
    console.error('[PWA] Error prompting install:', error);
    return false;
  }
}

/**
 * 检查 PWA 相关状态
 */
export async function getPwaStatus(): Promise<{
  isInstalled: boolean;
  canInstall: boolean;
  hasPrompted: boolean;
  hasDeferredPrompt: boolean;
}> {
  return {
    isInstalled: isPwaInstalled(),
    canInstall: canInstallPwa(),
    hasPrompted: hasPromptedInstall(),
    hasDeferredPrompt: deferredPrompt !== null
  };
}
