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

/**
 * 检查是否是 PWA 模式（已经安装到桌面）
 */
export function isPwaInstalled(): boolean {
  // 检查 display-mode 是否为 standalone
  if (typeof window !== 'undefined') {
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches;
    // 或者检查 iOS 的 navigator.standalone
    const isIosStandalone = (navigator as any).standalone === true;
    return isStandalone || isIosStandalone;
  }
  return false;
}

/**
 * 检查是否已经提示过安装
 */
export function hasPromptedInstall(): boolean {
  if (typeof window === 'undefined') return false;
  return localStorage.getItem('pwa_prompted') === 'true';
}

/**
 * 标记已提示过安装
 */
export function markPromptedInstall(): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem('pwa_prompted', 'true');
  }
}

/**
 * 检查是否可以安装（浏览器支持 PWA）
 */
export function canInstallPwa(): boolean {
  if (typeof window === 'undefined') return false;

  // 检查是否已经是 PWA
  if (isPwaInstalled()) return false;

  // 检查是否在 HTTPS 或 localhost（PWA 要求）
  const isHttps = window.location.protocol === 'https:';
  const isLocalhost = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';

  // 检查浏览器是否支持 service worker
  const hasServiceWorker = 'serviceWorker' in navigator;

  return (isHttps || isLocalhost) && hasServiceWorker;
}

/**
 * 初始化 PWA 安装监听器
 */
export function initPwaInstallListener(onPromptAvailable?: () => void): () => void {
  if (typeof window === 'undefined') return () => {};

  // 如果已经是 PWA，不需要监听
  if (isPwaInstalled()) return () => {};

  // 监听 beforeinstallprompt 事件
  installPromptListener = (e: Event) => {
    console.log('[PWA] beforeinstallprompt event received');
    // 阻止默认的提示
    e.preventDefault();
    // 保存事件以便稍后触发
    deferredPrompt = e as BeforeInstallPromptEvent;
    // 通知调用者可以显示提示了
    onPromptAvailable?.();
  };

  window.addEventListener('beforeinstallprompt', installPromptListener);

  // 监听 appinstalled 事件
  appInstalledListener = () => {
    console.log('[PWA] App installed to home screen!');
    deferredPrompt = null;
    markPromptedInstall();
    // 可以在这里触发庆祝动画等
  };

  window.addEventListener('appinstalled', appInstalledListener);

  // 返回清理函数
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
