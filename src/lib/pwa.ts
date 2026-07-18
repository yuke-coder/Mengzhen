'use client';

/**
 * PWA 安装状态由根级管理器统一维护。
 * 浏览器原生安装事件只能使用一次，因此必须尽早捕获并在页面切换间保留。
 */
interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

declare global {
  interface Window {
    __dreamPwaCaptureReady?: boolean;
    __dreamPwaInstallPrompt?: BeforeInstallPromptEvent | null;
    __dreamPwaAppInstalled?: boolean;
  }
}

export type PwaInstallResult = 'accepted' | 'dismissed' | 'unavailable';
export type PwaInstallMode = 'checking' | 'native' | 'manual' | 'waiting' | 'installed';

export interface PwaInstallStatus {
  mode: PwaInstallMode;
  isInstalled: boolean;
  canPromptNatively: boolean;
}

const INITIAL_STATUS: PwaInstallStatus = Object.freeze({
  mode: 'checking',
  isInstalled: false,
  canPromptNatively: false,
});

const INSTALL_ACCEPTED_SESSION_KEY = 'pwa_install_accepted_session';
const LEGACY_INSTALL_ACCEPTED_SESSION_KEY = 'pwa_prompted_session';
const PROMPT_AVAILABLE_EVENT = 'dream:pwa-install-prompt-available';
const APP_INSTALLED_EVENT = 'dream:pwa-app-installed';
const MANUAL_FALLBACK_DELAY_MS = 1800;

let deferredPrompt: BeforeInstallPromptEvent | null = null;
let installStatus: PwaInstallStatus = INITIAL_STATUS;
let managerInitialized = false;
let manualFallbackTimer: number | null = null;
const subscribers = new Set<() => void>();

function setInstallStatus(nextStatus: PwaInstallStatus): void {
  if (
    installStatus.mode === nextStatus.mode
    && installStatus.isInstalled === nextStatus.isInstalled
    && installStatus.canPromptNatively === nextStatus.canPromptNatively
  ) {
    return;
  }

  installStatus = nextStatus;
  subscribers.forEach((subscriber) => subscriber());
}

function clearManualFallbackTimer(): void {
  if (manualFallbackTimer === null || typeof window === 'undefined') return;
  window.clearTimeout(manualFallbackTimer);
  manualFallbackTimer = null;
}

function wasInstallAcceptedThisSession(): boolean {
  if (typeof window === 'undefined') return false;
  try {
    return sessionStorage.getItem(INSTALL_ACCEPTED_SESSION_KEY) === 'true'
      || sessionStorage.getItem(LEGACY_INSTALL_ACCEPTED_SESSION_KEY) === 'true';
  } catch {
    return false;
  }
}

function markInstallAccepted(): void {
  if (typeof window === 'undefined') return;
  try {
    sessionStorage.setItem(INSTALL_ACCEPTED_SESSION_KEY, 'true');
  } catch {
    // 部分隐私模式会禁用 sessionStorage，不影响当前页面的安装状态。
  }
}

function getCapturedPrompt(): BeforeInstallPromptEvent | null {
  if (typeof window === 'undefined') return null;
  if (!deferredPrompt && window.__dreamPwaInstallPrompt) {
    deferredPrompt = window.__dreamPwaInstallPrompt;
  }
  return deferredPrompt;
}

function setInstalledStatus(): void {
  clearManualFallbackTimer();
  deferredPrompt = null;

  if (typeof window !== 'undefined') {
    window.__dreamPwaInstallPrompt = null;
    window.__dreamPwaAppInstalled = true;
  }

  setInstallStatus({
    mode: 'installed',
    isInstalled: true,
    canPromptNatively: false,
  });
}

function setNativePromptStatus(prompt: BeforeInstallPromptEvent): void {
  clearManualFallbackTimer();
  deferredPrompt = prompt;
  window.__dreamPwaInstallPrompt = prompt;

  setInstallStatus({
    mode: 'native',
    isInstalled: false,
    canPromptNatively: true,
  });
}

function scheduleManualFallback(): void {
  clearManualFallbackTimer();
  manualFallbackTimer = window.setTimeout(() => {
    manualFallbackTimer = null;

    if (isPwaInstalled() || window.__dreamPwaAppInstalled || wasInstallAcceptedThisSession()) {
      setInstalledStatus();
      return;
    }

    const prompt = getCapturedPrompt();
    if (prompt) {
      setNativePromptStatus(prompt);
      return;
    }

    if (installStatus.mode !== 'waiting') {
      setInstallStatus({
        mode: 'manual',
        isInstalled: false,
        canPromptNatively: false,
      });
    }
  }, MANUAL_FALLBACK_DELAY_MS);
}

/**
 * 检查是否是 PWA 模式（已经安装并从桌面启动）。
 */
export function isPwaInstalled(): boolean {
  if (typeof window === 'undefined') return false;
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches;
  const isIosStandalone = (navigator as Navigator & { standalone?: boolean }).standalone === true;
  return isStandalone || isIosStandalone;
}

/**
 * 在根级客户端组件中调用一次。该管理器不会随页面切换卸载。
 */
export function initializePwaInstallManager(): void {
  if (typeof window === 'undefined') return;

  if (managerInitialized) {
    const capturedPrompt = getCapturedPrompt();
    if (capturedPrompt) setNativePromptStatus(capturedPrompt);
    return;
  }

  managerInitialized = true;

  const handlePromptAvailable = (event?: Event) => {
    const prompt = event?.type === 'beforeinstallprompt'
      ? event as BeforeInstallPromptEvent
      : window.__dreamPwaInstallPrompt ?? null;

    if (!prompt) return;
    prompt.preventDefault();
    if (prompt === deferredPrompt && installStatus.mode === 'native') return;
    console.log('[PWA] beforeinstallprompt event captured');
    setNativePromptStatus(prompt);
  };

  const handleAppInstalled = () => {
    if (installStatus.mode === 'installed') return;
    console.log('[PWA] App installed');
    markInstallAccepted();
    setInstalledStatus();
  };

  window.addEventListener('beforeinstallprompt', handlePromptAvailable);
  window.addEventListener(PROMPT_AVAILABLE_EVENT, handlePromptAvailable);
  window.addEventListener('appinstalled', handleAppInstalled);
  window.addEventListener(APP_INSTALLED_EVENT, handleAppInstalled);

  const displayMode = window.matchMedia('(display-mode: standalone)');
  const handleDisplayModeChange = () => {
    if (isPwaInstalled()) setInstalledStatus();
  };
  displayMode.addEventListener('change', handleDisplayModeChange);

  if (isPwaInstalled() || window.__dreamPwaAppInstalled || wasInstallAcceptedThisSession()) {
    setInstalledStatus();
    return;
  }

  const capturedPrompt = getCapturedPrompt();
  if (capturedPrompt) {
    setNativePromptStatus(capturedPrompt);
    return;
  }

  setInstallStatus(INITIAL_STATUS);
  scheduleManualFallback();
}

export function subscribePwaInstallStatus(subscriber: () => void): () => void {
  subscribers.add(subscriber);
  return () => subscribers.delete(subscriber);
}

export function getPwaInstallStatus(): PwaInstallStatus {
  return installStatus;
}

export function getPwaInstallServerStatus(): PwaInstallStatus {
  return INITIAL_STATUS;
}

/**
 * 调用已捕获的浏览器原生安装弹窗。
 */
export async function promptInstall(): Promise<PwaInstallResult> {
  initializePwaInstallManager();
  const prompt = getCapturedPrompt();

  if (!prompt) {
    console.log('[PWA] No native install prompt available');
    setInstallStatus({
      mode: 'manual',
      isInstalled: false,
      canPromptNatively: false,
    });
    return 'unavailable';
  }

  try {
    console.log('[PWA] Showing native install prompt');
    await prompt.prompt();
    const choiceResult = await prompt.userChoice;

    deferredPrompt = null;
    window.__dreamPwaInstallPrompt = null;

    if (choiceResult.outcome === 'accepted') {
      console.log('[PWA] User accepted the install prompt');
      markInstallAccepted();
      setInstalledStatus();
      return 'accepted';
    }

    console.log('[PWA] User dismissed the install prompt');
    setInstallStatus({
      mode: 'waiting',
      isInstalled: false,
      canPromptNatively: false,
    });
    return 'dismissed';
  } catch (error) {
    console.error('[PWA] Error showing native install prompt:', error);
    deferredPrompt = null;
    window.__dreamPwaInstallPrompt = null;
    setInstallStatus({
      mode: 'manual',
      isInstalled: false,
      canPromptNatively: false,
    });
    return 'unavailable';
  }
}
