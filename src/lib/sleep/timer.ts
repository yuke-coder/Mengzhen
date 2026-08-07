/**
 * 定时关闭模块（ExitAppTimer 迁移）
 *
 * 整迁自 QQ音乐 FromInfo.FROM_AUTO_CLOSE(114, "定时关闭")
 *       及 IPlayProcessInterface.setIsExitAppTimerRunningFalse()
 *
 * 原始设计：
 * - 内部命名 ExitAppTimer（退出 App 定时器）
 * - 到期后调用 stopMusic(FROM_AUTO_CLOSE=114) 停止播放
 * - 通过 setIsExitAppTimerRunningFalse() 重置定时器运行状态
 * - 定时器 UI 在 Kuikly JS 侧（countdown_mode / hour / minute）
 *
 * 梦枕适配：
 * - 复用现有 timer-worker.ts（Web Worker 后台计时，避免 PWA 节流）
 * - 暴露 React Hook 供 UI 使用
 */

'use client';

import { useState, useRef, useCallback, useEffect } from 'react';
import { SleepStorageKeys, getNumber, setNumber, getString, setString, getBool, setBool, getJSON, setJSON, removeKey } from './store';
import type { TimerMode } from './config';
import { TIMER_PRESETS } from './config';

/** 定时关闭来源标识（对齐 QQ音乐 FromInfo.FROM_AUTO_CLOSE=114） */
export const FROM_AUTO_CLOSE = 114;

/** 上次定时配置（记忆用户选择） */
interface LastTimerConfig {
  mode: TimerMode;
  minutes: number;
}

const DEFAULT_LAST_CONFIG: LastTimerConfig = {
  mode: 'countdown',
  minutes: 30,
};

/**
 * 定时关闭 Hook
 *
 * 用法：
 * ```tsx
 * const timer = useSleepTimer();
 * timer.start(30);  // 30分钟后关闭
 * timer.stop();     // 取消定时
 * ```
 */
export function useSleepTimer(onExpire?: () => void) {
  const [timerMode, setTimerMode] = useState<TimerMode>(
    () => (getString(SleepStorageKeys.TIMER_MODE, 'countdown') as TimerMode)
  );
  const [timerMinutes, setTimerMinutes] = useState<number>(
    () => getNumber(SleepStorageKeys.TIMER_MINUTES, 30)
  );
  const [isRunning, setIsRunning] = useState(false);
  const [remainingMs, setRemainingMs] = useState(0);

  const workerRef = useRef<Worker | null>(null);
  const expireCallbackRef = useRef<typeof onExpire>(undefined);
  const endTimeRef = useRef<number>(0);

  // 保存回调
  useEffect(() => {
    expireCallbackRef.current = onExpire;
  }, [onExpire]);

  // 初始化 Worker
  useEffect(() => {
    return () => {
      if (workerRef.current) {
        workerRef.current.terminate();
        workerRef.current = null;
      }
    };
  }, []);

  /**
   * 启动定时关闭
   * @param minutes 分钟数（0 表示无限模式）
   */
  const start = useCallback((minutes: number) => {
    // 保存配置
    const mode: TimerMode = minutes === 0 ? 'infinite' : 'countdown';
    setString(SleepStorageKeys.TIMER_MODE, mode);
    setNumber(SleepStorageKeys.TIMER_MINUTES, minutes);
    setJSON(SleepStorageKeys.LAST_TIMER_CONFIG, { mode, minutes });

    setTimerMode(mode);
    setTimerMinutes(minutes);

    // 无限模式：不启动定时器
    if (mode === 'infinite' || minutes === 0) {
      stopInternal();
      setIsRunning(false);
      setRemainingMs(0);
      return;
    }

    // 倒计时模式：启动 Worker
    stopInternal();

    try {
      workerRef.current = new Worker(new URL('@/lib/timer-worker.ts', import.meta.url));
    } catch {
      // Worker 创建失败，降级到 setInterval
      const endTime = Date.now() + minutes * 60 * 1000;
      endTimeRef.current = endTime;
      setIsRunning(true);
      const fallbackTimer = setInterval(() => {
        const remaining = endTime - Date.now();
        if (remaining <= 0) {
          clearInterval(fallbackTimer);
          setIsRunning(false);
          setRemainingMs(0);
          setBool(SleepStorageKeys.EFFECT_ENABLED, false);
          expireCallbackRef.current?.();
        } else {
          setRemainingMs(remaining);
        }
      }, 1000);
      return;
    }

    const endTime = Date.now() + minutes * 60 * 1000;
    endTimeRef.current = endTime;
    setIsRunning(true);

    workerRef.current.onmessage = (e: MessageEvent) => {
      const { type, remainingMs: ms } = e.data;
      if (type === 'tick') {
        setRemainingMs(ms);
      } else if (type === 'ended') {
        setIsRunning(false);
        setRemainingMs(0);
        // 对齐 QQ音乐：定时器到期后重置运行状态
        // setIsExitAppTimerRunningFalse() 的等效实现
        expireCallbackRef.current?.();
      }
    };

    workerRef.current.postMessage({ type: 'start', endTime });
  }, []);

  /** 停止定时关闭 */
  const stop = useCallback(() => {
    stopInternal();
    setIsRunning(false);
    setRemainingMs(0);
  }, []);

  /** 内部停止 */
  const stopInternal = useCallback(() => {
    if (workerRef.current) {
      workerRef.current.postMessage({ type: 'stop' });
      workerRef.current.terminate();
      workerRef.current = null;
    }
  }, []);

  /** 获取上次配置 */
  const getLastConfig = useCallback((): LastTimerConfig => {
    return getJSON(SleepStorageKeys.LAST_TIMER_CONFIG, DEFAULT_LAST_CONFIG);
  }, []);

  /** 格式化剩余时间 */
  const formatRemaining = useCallback((): string => {
    if (remainingMs <= 0) return '00:00';
    const totalSec = Math.ceil(remainingMs / 1000);
    const h = Math.floor(totalSec / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const s = totalSec % 60;
    if (h > 0) {
      return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }, [remainingMs]);

  return {
    /** 定时模式 */
    timerMode,
    /** 定时分钟数 */
    timerMinutes,
    /** 是否运行中 */
    isRunning,
    /** 剩余毫秒 */
    remainingMs,
    /** 格式化的剩余时间 */
    formatRemaining,
    /** 启动定时（0=无限模式） */
    start,
    /** 停止定时 */
    stop,
    /** 获取上次配置 */
    getLastConfig,
    /** 预设时长列表 */
    presets: TIMER_PRESETS,
  };
}
