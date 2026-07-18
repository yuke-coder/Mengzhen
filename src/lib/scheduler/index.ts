"use client";

/**
 * 梦枕 - 高效精炼调度器（使用统一音频系统）
 */

import { ScheduledTask, getNextExecuteDate, AudioPlayConfig } from "@/lib/task-types";
import { getAllTasks, updateTask } from "@/lib/task-store";
import { PriorityQueue } from "./queue";
import { EventEmitter } from "./events";
import { PlayPhase, SchedulerEvent, PlaybackState, FadeOptions } from "./core";
import UnifiedAudioManager, {
  getAudioBlob,
  initializeAudioContext,
  resumeAudioContext,
  tryUnlockAudio,
  getAudioState,
  setVolume,
  fadeIn,
  fadeOut,
  stopFade,
  setupMediaSession,
  updateMediaSessionMetadata,
  updateMediaSessionPlaybackState,
  releaseMediaSession
} from "@/lib/audio";

type ErrorCode = 'UNAUTHORIZED' | 'FORBIDDEN' | 'TOO_MANY_REQUESTS' | 'SERVER_ERROR' | 'TIMEOUT' | 'UNKNOWN';

interface AudioPlaybackError {
  code: ErrorCode;
  message: string;
  originalError?: Error;
}

interface PlaybackLog {
  taskId: string;
  event: string;
  timestamp: number;
  provider?: string;
  duration?: number;
  statusCode?: number;
  errorSummary?: string;
  metadata?: Record<string, unknown>;
}

const logs: PlaybackLog[] = [];
const MAX_LOGS = 100;

function addLog(log: PlaybackLog): void {
  logs.push(log);
  if (logs.length > MAX_LOGS) logs.shift();
}

function classifyError(error: Error | DOMException): AudioPlaybackError {
  const message = error.message || String(error);

  if ('name' in error && error.name === 'TimeoutError' || message.includes('timeout')) {
    return { code: 'TIMEOUT', message: '播放超时', originalError: error };
  }

  if (message.includes('401') || message.includes('Unauthorized') || message.includes('认证')) {
    return { code: 'UNAUTHORIZED', message: '未授权访问', originalError: error };
  }

  if (message.includes('403') || message.includes('Forbidden') || message.includes('禁止')) {
    return { code: 'FORBIDDEN', message: '无访问权限', originalError: error };
  }

  if (message.includes('429') || message.includes('Too Many')) {
    return { code: 'TOO_MANY_REQUESTS', message: '请求过于频繁', originalError: error };
  }

  if (message.includes('5') || message.includes('Server')) {
    return { code: 'SERVER_ERROR', message: '服务端错误', originalError: error };
  }

  return { code: 'UNKNOWN', message: message || '播放异常', originalError: error };
}

function log(taskId: string, level: 'info' | 'warn' | 'error', msg: string, data?: unknown) {
  const prefix = `[Scheduler:${taskId}]`;
  if (level === 'error') {
    console.error(prefix, msg, data || '');
  } else if (level === 'warn') {
    console.warn(prefix, msg, data || '');
  } else {
    console.log(prefix, msg, data || '');
  }
}

interface AudioPlayerAdapter {
  play(audioUrl: string, config: AudioPlayConfig): Promise<void>;
  stop(): void;
}

interface LocalPlaybackState {
  taskId: string;
  audio: HTMLAudioElement;
  targetVolume: number;
  phase: 'fading-in' | 'playing' | 'fading-out';
  startedAt: number;
  scheduledStartAt: number;
  playedDuration: number;
  retryCount: number;
  currentBlobUrl: string | null;
}

const activePlaybacks = new Map<string, LocalPlaybackState>();
const audioManager = UnifiedAudioManager.getInstance();

async function playAudio(task: ScheduledTask, scheduledStartAt: number): Promise<LocalPlaybackState | null> {
  const startTime = Date.now();
  addLog({ taskId: task.id, event: 'playback_start', timestamp: startTime, metadata: { audioCount: task.audios.length } });

  if (task.audios.length === 0) {
    log(task.id, 'warn', '没有音频可播放');
    addLog({ taskId: task.id, event: 'error', timestamp: Date.now(), errorSummary: 'No audio' });
    return null;
  }

  let audioUrl: string | null = null;
  let isBlobUrl = false;

  for (const audio of task.audios) {
    if (audio.serverUrl && audio.serverUrl.trim() !== '') {
      audioUrl = audio.serverUrl;
      log(task.id, 'info', `使用服务器URL: ${audio.name}`);
      break;
    }
    if (audio.fileKey && audio.fileKey.trim() !== '') {
      audioUrl = `/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}`;
      log(task.id, 'info', `使用文件密钥: ${audio.name}`);
      break;
    }
    if (audio.dbKey && audio.dbKey.trim() !== '') {
      try {
        const blob = await getAudioBlob(audio.dbKey);
        if (blob) {
          audioUrl = URL.createObjectURL(blob);
          isBlobUrl = true;
          log(task.id, 'info', `使用IndexedDB: ${audio.name}`);
          break;
        }
      } catch (error) {
        log(task.id, 'warn', `IndexedDB读取失败: ${error}`);
      }
    }
  }

  if (!audioUrl || audioUrl.trim() === '') {
    log(task.id, 'error', '无法找到有效的音频源');
    addLog({ taskId: task.id, event: 'error', timestamp: Date.now(), errorSummary: 'No audio source' });
    return null;
  }

  const audioElement = new Audio();
  audioElement.src = audioUrl;
  audioElement.loop = true;
  audioElement.volume = 0;
  audioElement.preload = 'auto';

  await new Promise<void>((resolve) => {
    const onCanPlay = () => {
      log(task.id, 'info', '音频可以播放了');
      addLog({ taskId: task.id, event: 'audio_ready', timestamp: Date.now(), duration: Date.now() - startTime });
      audioElement.removeEventListener('canplay', onCanPlay);
      audioElement.removeEventListener('error', onError);
      resolve();
    };
    const onError = () => {
      log(task.id, 'warn', '音频加载错误');
      addLog({ taskId: task.id, event: 'audio_error', timestamp: Date.now(), errorSummary: 'Audio load error' });
      audioElement.removeEventListener('canplay', onCanPlay);
      audioElement.removeEventListener('error', onError);
      resolve();
    };
    audioElement.addEventListener('canplay', onCanPlay);
    audioElement.addEventListener('error', onError);
    setTimeout(resolve, 2000);
    audioElement.load();
  });

  audioElement.play().catch((error) => {
    log(task.id, 'warn', `初始播放失败: ${error}`);
    addLog({ taskId: task.id, event: 'play_error', timestamp: Date.now(), errorSummary: String(error) });
  });

  log(task.id, 'info', '播放已启动');
  addLog({ taskId: task.id, event: 'player_ready', timestamp: Date.now(), duration: Date.now() - startTime });

  const playback: LocalPlaybackState = {
    taskId: task.id,
    audio: audioElement,
    targetVolume: (task.volume ?? 70) / 100,
    phase: 'fading-in',
    startedAt: Date.now(),
    scheduledStartAt,
    playedDuration: 0,
    retryCount: 0,
    currentBlobUrl: isBlobUrl ? audioUrl : null
  };

  activePlaybacks.set(task.id, playback);
  return playback;
}

function stopAudio(taskId: string): void {
  const playback = activePlaybacks.get(taskId);
  if (!playback) return;

  stopFade();

  try {
    playback.audio.volume = 0;
    playback.audio.pause();
    playback.audio.removeAttribute('src');
    playback.audio.src = '';
    try { playback.audio.load(); } catch {}
    if (playback.currentBlobUrl) {
      try { URL.revokeObjectURL(playback.currentBlobUrl); } catch {}
    }
  } catch (error) {
    console.error('[Player] 停止音频失败:', error);
  }

  activePlaybacks.delete(taskId);
}

interface QueuedTask { taskId: string; nextExecAt: number; task: ScheduledTask; }

class TaskCache {
  private cache = new Map<string, { task: ScheduledTask; nextExecAt: number | null; cachedAt: number; lastModifiedAt: number }>();
  private lastCount = 0;
  private readonly CACHE_TTL = 3600000;

  sync(): boolean {
    const tasks = getAllTasks();
    if (tasks.length === this.lastCount) {
      let changed = false;
      for (const task of tasks) {
        const cached = this.cache.get(task.id);
        if (!cached || cached.lastModifiedAt < task.updatedAt) { changed = true; break; }
      }
      if (!changed) return false;
    }
    this.cache.clear();
    for (const task of tasks) {
      this.cache.set(task.id, { task, nextExecAt: null, cachedAt: 0, lastModifiedAt: task.updatedAt });
    }
    this.lastCount = tasks.length;
    return true;
  }

  getNextExecAt(taskId: string): number | null {
    const cached = this.cache.get(taskId);
    if (!cached) return null;
    const now = Date.now();
    if (cached.nextExecAt !== null && now - cached.cachedAt < this.CACHE_TTL) {
      if (cached.task.repeatType !== 'once' || cached.nextExecAt > now) return cached.nextExecAt;
    }
    const nextDate = getNextExecuteDate(cached.task);
    cached.nextExecAt = nextDate?.getTime() || null;
    cached.cachedAt = now;
    return cached.nextExecAt;
  }

  invalidate(taskId: string): void {
    const cached = this.cache.get(taskId);
    if (cached) { cached.nextExecAt = null; cached.cachedAt = 0; }
  }

  getPendingTasks(): Array<{ task: ScheduledTask; nextExecAt: number }> {
    this.sync();
    const now = Date.now();
    const result: Array<{ task: ScheduledTask; nextExecAt: number }> = [];
    for (const [, cached] of this.cache) {
      if (cached.task.status === 'cancelled') continue;
      if (cached.task.skipUntil && now < cached.task.skipUntil) continue;
      const nextExec = this.getNextExecAt(cached.task.id);
      if (nextExec !== null) result.push({ task: cached.task, nextExecAt: nextExec });
    }
    return result;
  }

  getTask(taskId: string): ScheduledTask | undefined {
    return this.cache.get(taskId)?.task;
  }
}

class HighPerformanceScheduler {
  private eventEmitter = new EventEmitter();
  private taskCache = new TaskCache();
  private taskQueue = new PriorityQueue<QueuedTask>((a, b) => a.nextExecAt - b.nextExecAt);
  private nextTimer: ReturnType<typeof setTimeout> | null = null;
  private tickTimer: ReturnType<typeof setTimeout> | null = null;
  private isBackground = false;
  private isRunning = false;
  private readonly CHECK_WINDOW = 1500;
  private readonly MIN_DELAY = 100;

  async start(): Promise<void> {
    if (this.isRunning) return;
    this.isRunning = true;
    addLog({ taskId: 'scheduler', event: 'start', timestamp: Date.now() });
    this.setupVisibilityHandler();
    setupMediaSession();
    tryUnlockAudio();
    await this.resumeActiveTasks();
    this.rebuildQueue();
    this.scheduleNextCheck();
    this.startTickLoop();
  }

  tryUnlockAudio(): void {
    tryUnlockAudio();
  }

  stop(): void {
    addLog({ taskId: 'scheduler', event: 'stop', timestamp: Date.now() });
    this.isRunning = false;
    if (this.nextTimer) clearTimeout(this.nextTimer);
    if (this.tickTimer) clearTimeout(this.tickTimer);
    activePlaybacks.forEach((_, id) => this.stopPlayback(id));
    releaseMediaSession();
  }

  on(eventTypeOrCallback: string | ((event: SchedulerEvent) => void), callback?: (event: SchedulerEvent) => void): () => void {
    if (typeof eventTypeOrCallback === 'function') {
      const unsubscribes: Array<() => void> = [];
      const allTypes = ['task-started', 'task-completed', 'task-cancelled', 'task-resumed', 'phase-change', 'tick'];
      for (const type of allTypes) unsubscribes.push(this.eventEmitter.on(type, eventTypeOrCallback));
      return () => unsubscribes.forEach(unsub => unsub());
    } else if (callback) {
      return this.eventEmitter.on(eventTypeOrCallback, callback);
    }
    return () => {};
  }

  cancelTask(taskId: string): void {
    const task = getAllTasks().find(t => t.id === taskId);
    if (!task) return;
    this.stopPlayback(taskId);
    // 同时停止 UnifiedAudioManager 中可能存在的播放（双调度器防漏）
    UnifiedAudioManager.getInstance().stopAudio(taskId);
    updateTask(taskId, task.repeatType === 'once' ? { status: 'cancelled' } : { skipUntil: Date.now() + task.playDurationMinutes * 60000 });
    this.taskCache.invalidate(taskId);
    this.rebuildQueue();
    this.scheduleNextCheck();
    this.emit('task-cancelled', taskId, 'idle', 0);
  }

  resumeTask(taskId: string): void {
    const task = getAllTasks().find(t => t.id === taskId);
    if (!task) return;
    updateTask(taskId, { skipUntil: undefined });
    this.taskCache.invalidate(taskId);
    this.rebuildQueue();
    this.scheduleNextCheck();
    this.emit('task-resumed', taskId, 'waiting', 0);
  }

  async executeNow(taskId: string): Promise<void> {
    const task = getAllTasks().find(t => t.id === taskId);
    if (!task || activePlaybacks.has(taskId)) return;
    log(taskId, 'info', `立即执行任务，音频数: ${task.audios.length}`);
    for (const audio of task.audios) {
      log(taskId, 'info', `  - ${audio.name}: serverUrl=${!!audio.serverUrl}, fileKey=${!!audio.fileKey}, dbKey=${!!audio.dbKey}`);
    }
    updateTask(taskId, { status: 'executing', lastExecutedAt: Date.now() });
    await this.startPlayback(task, Date.now());
  }

  forceStopPlayback(taskId: string): void {
    this.cancelTask(taskId);
  }

  getActiveTaskIds(): string[] { return Array.from(activePlaybacks.keys()); }

  getTaskPhase(taskId: string): PlayPhase {
    const playback = activePlaybacks.get(taskId);
    if (playback) return playback.phase;
    const task = getAllTasks().find(t => t.id === taskId);
    if (!task || task.status === 'cancelled') return 'idle';
    if (task.skipUntil && Date.now() < task.skipUntil) return 'idle';
    if (task.status === 'completed') return 'idle';
    return 'waiting';
  }

  getTaskRemainingMs(taskId: string): number {
    const playback = activePlaybacks.get(taskId);
    if (playback) return this.computeRemaining(playback);
    const nextExec = this.taskCache.getNextExecAt(taskId);
    if (!nextExec) return 0;
    const task = getAllTasks().find(t => t.id === taskId);
    if (!task) return 0;
    const now = Date.now();
    const fadeIn = (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;
    const audioStartAt = nextExec - fadeIn;
    return Math.max(0, audioStartAt - now);
  }

  resumeAudioContext(): Promise<boolean> {
    return resumeAudioContext();
  }

  getAudioState(): string | undefined {
    return getAudioState();
  }

  private rebuildQueue(): void {
    this.taskQueue.clear();
    const now = Date.now();
    const maxLookAhead = 24 * 60 * 60 * 1000;
    for (const { task, nextExecAt } of this.taskCache.getPendingTasks()) {
      const fadeIn = (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;
      const audioStartAt = nextExecAt - fadeIn;
      if (audioStartAt < now + maxLookAhead) this.taskQueue.push({ taskId: task.id, nextExecAt: audioStartAt, task });
    }
  }

  private scheduleNextCheck(): void {
    if (this.nextTimer) clearTimeout(this.nextTimer);
    if (!this.isRunning) return;
    if (this.taskCache.sync()) this.rebuildQueue();
    const next = this.taskQueue.peek();
    if (!next) return;
    const delay = Math.max(this.MIN_DELAY, next.nextExecAt - Date.now());
    this.nextTimer = setTimeout(() => this.processBatch(), delay);
  }

  private processBatch(): void {
    if (!this.isRunning) return;
    const now = Date.now();
    const windowEnd = now + this.CHECK_WINDOW;
    const batch: ScheduledTask[] = [];

    while (true) {
      const nextTask = this.taskQueue.peek();
      if (!nextTask || nextTask.nextExecAt > windowEnd) break;
      const queued = this.taskQueue.pop()!;
      const task = getAllTasks().find(t => t.id === queued.taskId);
      if (task && this.shouldExecute(task, now)) batch.push(task);
    }

    for (const task of batch) this.processTask(task);
    this.scheduleNextCheck();
  }

  private shouldExecute(task: ScheduledTask, now: number): boolean {
    if (task.status === 'cancelled') return false;
    if (task.skipUntil && now < task.skipUntil) return false;
    if (activePlaybacks.has(task.id)) return false;
    const nextExec = this.taskCache.getNextExecAt(task.id);
    if (!nextExec) return false;
    const fadeIn = (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;
    const audioStart = nextExec - fadeIn;
    const end = nextExec + task.playDurationMinutes * 60000;

    if (task.repeatType === 'once' && !task.lastExecutedAt) {
      return now >= audioStart;
    }

    return now >= audioStart && now < end;
  }

  private async processTask(task: ScheduledTask): Promise<void> {
    const nextExec = this.taskCache.getNextExecAt(task.id);
    if (!nextExec) return;

    if (task.repeatType === 'once') {
      const end = nextExec + task.playDurationMinutes * 60000;
      if (Date.now() >= end && task.lastExecutedAt) {
        updateTask(task.id, { status: 'completed', completedAt: Date.now() });
        return;
      }
    }

    updateTask(task.id, { status: 'executing', lastExecutedAt: Date.now() });
    await this.startPlayback(task, nextExec);
  }

  private async startPlayback(task: ScheduledTask, scheduledStartAt: number): Promise<void> {
    if (activePlaybacks.has(task.id)) return;
    log(task.id, 'info', '开始播放...');

    const now = Date.now();
    const actualStartAt = (task.repeatType === 'once' && !task.lastExecutedAt && now > scheduledStartAt) ? now : scheduledStartAt;

    const playback = await playAudio(task, actualStartAt);
    if (!playback) {
      log(task.id, 'error', '无法创建播放器');
      return;
    }

    log(task.id, 'info', `播放已启动，目标音量: ${playback.targetVolume}, 启用渐入渐出: ${task.enableFade}`);
    updateMediaSessionMetadata(task);
    updateMediaSessionPlaybackState(true);

    const fadeInMs = task.enableFade ? (task.fadeInDuration || 0) * 1000 : 0;
    // 音频是刚创建的，渐入应从头开始
    // 之前用 audioStartAt = actualStartAt - fadeInMs 计算已过时间，
    // 对过期任务会得到 alreadyElapsed = fadeInMs，导致渐入被跳过
    const alreadyElapsed = 0;

    addLog({ taskId: task.id, event: 'config_applied', timestamp: Date.now(), provider: task.enableFade ? 'fade-enabled' : 'fade-disabled', metadata: { volume: task.volume, targetVolume: playback.targetVolume, enableFade: task.enableFade } });

    if (task.enableFade && task.fadeInDuration && task.fadeInDuration > 0 && alreadyElapsed < fadeInMs) {
      log(task.id, 'info', `开始渐入，时长: ${task.fadeInDuration}秒，已过: ${alreadyElapsed}ms`);
      this.emit('task-started', task.id, 'fading-in', this.computeRemaining(playback), task.name);
      fadeIn(playback.audio, {
        duration: task.fadeInDuration,
        targetVolume: playback.targetVolume,
        onComplete: () => {
          log(task.id, 'info', '渐入完成');
          if (activePlaybacks.has(task.id)) this.emitPhaseChange(task.id, 'playing');
        }
      }, alreadyElapsed);
    } else {
      log(task.id, 'info', `直接设置音量: ${playback.targetVolume}`);
      playback.audio.play().catch(() => {});
      setVolume(playback.audio, playback.targetVolume);
      this.emit('task-started', task.id, 'playing', this.computeRemaining(playback), task.name);
      this.emitPhaseChange(task.id, 'playing');
    }

    this.schedulePlaybackEnd(task, actualStartAt);
  }

  private schedulePlaybackEnd(task: ScheduledTask, scheduledStartAt: number): void {
    const playback = activePlaybacks.get(task.id);
    if (!playback) return;
    const now = Date.now();
    const endTime = scheduledStartAt + task.playDurationMinutes * 60000;
    const remaining = Math.max(0, endTime - now);
    this.taskCache.invalidate(task.id);

    setTimeout(() => {
      const p = activePlaybacks.get(task.id);
      if (p) this.startFadeOut(task, p, scheduledStartAt);
    }, remaining);
  }

  private startFadeOut(task: ScheduledTask, playback: LocalPlaybackState, scheduledStartAt: number): void {
    if (task.enableFade && task.fadeOutDuration > 0) {
      fadeOut(playback.audio, {
        duration: task.fadeOutDuration,
        targetVolume: 0,
        onComplete: () => this.completeTask(task, playback)
      });
      this.emitPhaseChange(task.id, 'fading-out');
    } else {
      this.completeTask(task, playback);
    }
  }

  private completeTask(task: ScheduledTask, playback: LocalPlaybackState): void {
    addLog({ taskId: task.id, event: 'complete', timestamp: Date.now() });
    this.stopPlayback(task.id);
    updateTask(task.id, task.repeatType === 'once' ? { status: 'completed', completedAt: Date.now() } : { status: 'pending', lastExecutedAt: Date.now() });
    this.emit('task-completed', task.id, 'idle', 0, task.name);
    if (activePlaybacks.size === 0) {
      updateMediaSessionPlaybackState(false);
    }
    this.taskCache.invalidate(task.id);
    this.rebuildQueue();
    this.scheduleNextCheck();
  }

  private stopPlayback(taskId: string): void {
    const playback = activePlaybacks.get(taskId);
    if (!playback) return;
    stopFade();
    stopAudio(taskId);
    // 同时清理 UnifiedAudioPlayer 中的残留播放
    UnifiedAudioManager.getInstance().stopAudio(taskId);
    if (activePlaybacks.size === 0) {
      updateMediaSessionPlaybackState(false);
    }
  }

  private async resumeActiveTasks(): Promise<void> {
    const tasks = getAllTasks();
    const now = Date.now();
    for (const task of tasks) {
      if (task.status !== 'executing') continue;
      if (activePlaybacks.has(task.id)) continue;
      const nextExec = this.taskCache.getNextExecAt(task.id);
      const baseStart = nextExec || getTimestamp(task);
      const end = baseStart + task.playDurationMinutes * 60000;
      const fadeIn = (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;
      const fadeOut = (task.enableFade ? (task.fadeOutDuration || 0) : 0) * 1000;
      const audioStart = baseStart - fadeIn;
      if (now < audioStart || (now >= end + fadeOut && task.lastExecutedAt)) {
        updateTask(task.id, task.repeatType === 'once' ? { status: 'completed', completedAt: now } : { status: 'pending' });
        continue;
      }
      await this.startPlayback(task, baseStart);
    }
  }

  /** 强制刷新调度队列，用于新任务创建/修改后 */
  refreshSchedule(): void {
    if (!this.isRunning) return;
    this.taskCache.sync();
    this.rebuildQueue();
    this.scheduleNextCheck();
  }

  private startTickLoop(): void {
    let tickCount = 0;
    const tick = () => {
      if (!this.isRunning) return;
      const now = Date.now();
      const interval = this.isBackground ? 3000 : 500;
      tickCount++;

      // 每 10 个 tick（约 5 秒）检查一次是否有新任务
      if (tickCount % 10 === 0) {
        this.taskCache.sync();
        this.rebuildQueue();
        this.scheduleNextCheck();
      }

      activePlaybacks.forEach((playback, taskId) => {
        const task = getAllTasks().find(t => t.id === taskId);
        if (!task) { this.stopPlayback(taskId); return; }

        const end = playback.scheduledStartAt + task.playDurationMinutes * 60000;
        const fadeInEnd = playback.scheduledStartAt + (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;

        if (playback.phase === 'fading-in' && now >= fadeInEnd) {
          setVolume(playback.audio, playback.targetVolume);
          this.emitPhaseChange(taskId, 'playing');
        }
        if (playback.phase === 'playing' && now >= end) {
          this.startFadeOut(task, playback, playback.scheduledStartAt);
        }

        if (playback.audio.paused && (playback.phase === 'playing' || playback.phase === 'fading-in')) {
          if (playback.audio.readyState >= 2) {
            if (playback.phase === 'playing') {
              setVolume(playback.audio, playback.targetVolume);
            }
            playback.audio.play().catch((error) => this.handleError(taskId, error));
          }
        }

        this.emit('tick', taskId, playback.phase, this.computeRemaining(playback));
      });

      this.tickTimer = setTimeout(tick, interval);
    };
    tick();
  }

  private async handleError(taskId: string, error: Error): Promise<void> {
    const playback = activePlaybacks.get(taskId);
    if (!playback) return;
    const MAX_RETRY = 5;
    const RETRY_DELAY = 1500;
    const task = getAllTasks().find(t => t.id === taskId);
    if (!task) { this.stopPlayback(taskId); return; }

    const classifiedError = classifyError(error);
    log(taskId, 'warn', `处理错误: ${classifiedError.message}, 重试次数: ${playback.retryCount}`);
    addLog({ taskId, event: 'error', timestamp: Date.now(), errorSummary: classifiedError.message });

    const isUserInteractionError = error.message.includes('user didn\'t interact') || error.message.includes('user gesture') || error.name === 'NotAllowedError';

    if (isUserInteractionError && playback.retryCount === 0) {
      log(taskId, 'info', '检测到浏览器自动播放限制，尝试恢复 AudioContext');
      await resumeAudioContext();
      playback.retryCount++;
      await new Promise(r => setTimeout(r, 500));
      try {
        if (task.enableFade && task.fadeInDuration && task.fadeInDuration > 0) {
          fadeIn(playback.audio, {
            duration: task.fadeInDuration,
            targetVolume: playback.targetVolume,
            onComplete: () => {
              if (activePlaybacks.has(taskId)) this.emitPhaseChange(taskId, 'playing');
            }
          });
        } else {
          setVolume(playback.audio, playback.targetVolume);
        }
        await playback.audio.play();
        updateMediaSessionMetadata(task);
        updateMediaSessionPlaybackState(true);
        log(taskId, 'info', `恢复后播放成功，音量: ${playback.audio.volume}`);
        return;
      } catch (error) {
        log(taskId, 'warn', '恢复后仍然无法播放，继续重试流程');
      }
    }

    const now = Date.now();
    const end = playback.scheduledStartAt + task.playDurationMinutes * 60000;
    const fadeOut = (task.enableFade ? (task.fadeOutDuration || 0) : 0) * 1000;
    if (now >= end + fadeOut && task.lastExecutedAt) {
      log(taskId, 'info', '已到结束时间，停止播放');
      this.stopPlayback(taskId);
      if (task.repeatType === 'once') updateTask(task.id, { status: 'completed', completedAt: now });
      return;
    }

    if (playback.retryCount < MAX_RETRY) {
      playback.retryCount++;
      log(taskId, 'info', `开始重试 ${playback.retryCount}/${MAX_RETRY}`);
      await new Promise(r => setTimeout(r, RETRY_DELAY));
      const current = getAllTasks().find(t => t.id === taskId);
      if (!current || current.status === 'cancelled') { this.stopPlayback(taskId); return; }
      try {
        let newAudioUrl: string | null = null;
        for (const audio of task.audios) {
          if (audio.serverUrl) { newAudioUrl = audio.serverUrl; break; }
          if (audio.fileKey) { newAudioUrl = `/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}`; break; }
          if (audio.dbKey) {
            const blob = await getAudioBlob(audio.dbKey);
            if (blob) {
              if (playback.currentBlobUrl) { try { URL.revokeObjectURL(playback.currentBlobUrl); } catch {} }
              newAudioUrl = URL.createObjectURL(blob);
              playback.currentBlobUrl = newAudioUrl;
              break;
            }
          }
        }
        if (newAudioUrl) {
          log(taskId, 'info', `重新设置音频源: ${newAudioUrl.substring(0, 50)}...`);
          playback.audio.src = newAudioUrl;
          playback.audio.load();
          setVolume(playback.audio, playback.targetVolume);
          await playback.audio.play();
          updateMediaSessionMetadata(task);
          updateMediaSessionPlaybackState(true);
          log(taskId, 'info', `重试播放成功，音量: ${playback.audio.volume}`);
        }
      } catch (error) {
        log(taskId, 'warn', `重试失败: ${error}`);
        setTimeout(() => this.handleError(taskId, error instanceof Error ? error : new Error(String(error))), 0);
      }
      return;
    }

    log(taskId, 'error', '已达最大重试次数，停止播放');
    addLog({ taskId, event: 'max_retry', timestamp: Date.now() });
    this.stopPlayback(taskId);
    updateTask(task.id, task.repeatType === 'once' ? { status: 'completed', completedAt: Date.now() } : { status: 'pending' });
    this.emit('task-completed', task.id, 'idle', 0, task.name);
  }

  private setupVisibilityHandler(): void {
    const handle = () => {
      this.isBackground = document.visibilityState !== 'visible';
      if (!this.isBackground) {
        this.taskCache.sync();
        this.rebuildQueue();
        this.scheduleNextCheck();
        resumeAudioContext().catch(() => {});
        updateMediaSessionPlaybackState(activePlaybacks.size > 0);

        activePlaybacks.forEach((playback, taskId) => {
          const task = getAllTasks().find(t => t.id === taskId);
          if (!task) return;

          if (playback.audio.paused && playback.audio.readyState >= 2) {
            if (playback.phase === 'playing') {
              setVolume(playback.audio, playback.targetVolume);
            } else if (playback.phase === 'fading-in') {
              const now = Date.now();
              const fadeInMs = task.enableFade ? (task.fadeInDuration || 0) : 0 * 1000;
              const audioStartAt = playback.scheduledStartAt - fadeInMs;
              const alreadyElapsed = Math.max(0, now - audioStartAt);
              fadeIn(playback.audio, {
                duration: task.fadeInDuration || 0,
                targetVolume: playback.targetVolume,
                onComplete: () => {
                  if (activePlaybacks.has(taskId)) this.emitPhaseChange(taskId, 'playing');
                }
              }, alreadyElapsed);
            }
            playback.audio.play().catch((error) => {
              log(taskId, 'warn', `可见性变化后播放失败: ${error}`);
            });
          }
        });
      } else {
        updateMediaSessionPlaybackState(activePlaybacks.size > 0);
      }
    };
    document.addEventListener('visibilitychange', handle);
  }

  private computeRemaining(playback: LocalPlaybackState): number {
    const task = getAllTasks().find(t => t.id === playback.taskId);
    if (!task) return 0;
    const now = Date.now();
    const fadeInEnd = playback.scheduledStartAt + (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;
    const end = playback.scheduledStartAt + task.playDurationMinutes * 60000;
    const fadeOutEnd = end + (task.enableFade ? (task.fadeOutDuration || 0) : 0) * 1000;
    if (playback.phase === 'fading-in') return Math.max(0, fadeInEnd - now);
    if (playback.phase === 'playing') return Math.max(0, end - now);
    if (playback.phase === 'fading-out') return Math.max(0, fadeOutEnd - now);
    return 0;
  }

  private emitPhaseChange(taskId: string, phase: LocalPlaybackState['phase']): void {
    const playback = activePlaybacks.get(taskId);
    if (playback) {
      playback.phase = phase;
      this.emit('phase-change', taskId, phase, this.computeRemaining(playback));
    }
  }

  private emit(type: SchedulerEvent['type'], taskId: string, phase: PlayPhase, remainingMs: number, taskName?: string): void {
    this.eventEmitter.emit({ type, taskId, phase, remainingMs, taskName });
  }
}

function getTimestamp(task: ScheduledTask): number {
  return new Date(task.startTime.year, task.startTime.month - 1, task.startTime.day, task.startTime.hour, task.startTime.minute, task.startTime.second).getTime();
}

let schedulerInstance: HighPerformanceScheduler | null = null;

export function getTaskScheduler(): HighPerformanceScheduler {
  if (!schedulerInstance) schedulerInstance = new HighPerformanceScheduler();
  return schedulerInstance;
}

export async function startTaskScheduler(): Promise<HighPerformanceScheduler> {
  const scheduler = getTaskScheduler();
  await scheduler.start();
  return scheduler;
}

export function stopTaskScheduler(): void {
  if (schedulerInstance) {
    schedulerInstance.stop();
    schedulerInstance = null;
  }
}

export function getPlaybackLogs(): PlaybackLog[] {
  return [...logs];
}

export type { SchedulerEvent, PlayPhase, HighPerformanceScheduler };
