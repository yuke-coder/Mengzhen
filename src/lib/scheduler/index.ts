/**
 * 梦枕 - 高效精炼调度器
 */
import { ScheduledTask, getNextExecuteDate, AudioPlayConfig } from '../task-types';
import { getAllTasks, updateTask } from '../task-store';
import { PriorityQueue } from './queue';
import { EventEmitter } from './events';
import { PlayPhase, SchedulerEvent, PlaybackState, FadeOptions } from './core';
import { getAudioBlob } from '../audio-db';

// 错误分类
type ErrorCode = 'UNAUTHORIZED' | 'FORBIDDEN' | 'TOO_MANY_REQUESTS' | 'SERVER_ERROR' | 'TIMEOUT' | 'UNKNOWN';

interface AudioPlaybackError {
  code: ErrorCode;
  message: string;
  originalError?: Error;
}

// 日志类型
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

// 日志存储
const logs: PlaybackLog[] = [];
const MAX_LOGS = 100;

function addLog(log: PlaybackLog): void {
  logs.push(log);
  if (logs.length > MAX_LOGS) logs.shift();
}

// 分类错误处理
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

// 简单的日志函数
function log(tag: string, level: 'info' | 'warn' | 'error', msg: string, data?: unknown) {
  const prefix = `[Scheduler:${tag}]`;
  if (level === 'error') {
    console.error(prefix, msg, data || '');
  } else if (level === 'warn') {
    console.warn(prefix, msg, data || '');
  } else {
    console.log(prefix, msg, data || '');
  }
}

// 播放适配器接口
interface AudioPlayerAdapter {
  play(audioUrl: string, config: AudioPlayConfig): Promise<void>;
  stop(): void;
}

// 音频播放器
class AudioPlayer {
  private audioContext: AudioContext | null = null;

  private ensureAudioContext(): void {
    if (!this.audioContext || this.audioContext.state === 'closed') {
      try {
        const AudioContextConstructor = (window as Window & { AudioContext?: typeof AudioContext; webkitAudioContext?: typeof AudioContext }).AudioContext || (window as any).webkitAudioContext;
        if (AudioContextConstructor) this.audioContext = new AudioContextConstructor();
      } catch {}
    }
    // 尝试 resume AudioContext，如果失败也没关系
    if (this.audioContext?.state === 'suspended') {
      this.audioContext.resume().catch(() => {});
    }
  }

  /** 强制尝试解锁音频播放 */
  tryUnlockAudio(): void {
    this.ensureAudioContext();
    // 创建一个临时的静音音频元素尝试播放
    try {
      const unlockAudio = new Audio();
      unlockAudio.volume = 0;
      unlockAudio.muted = true;
      unlockAudio.play().catch(() => {}).finally(() => {
        // 不要立即清理，稍等一下
        setTimeout(() => {
          unlockAudio.pause();
          unlockAudio.src = '';
        }, 100);
      });
    } catch {}
  }

  async playAudio(task: ScheduledTask, scheduledStartAt: number): Promise<PlaybackState | null> {
    const startTime = Date.now();
    addLog({ taskId: task.id, event: 'playback_start', timestamp: startTime, metadata: { audioCount: task.audios.length } });

    if (task.audios.length === 0) {
      log(task.id, 'warn', '没有音频可播放');
      addLog({ taskId: task.id, event: 'error', timestamp: Date.now(), errorSummary: 'No audio' });
      return null;
    }
    this.ensureAudioContext();

    let audioUrl: string | null = null;
    let isBlobUrl = false;

    // 优先级: serverUrl > fileKey > dbKey
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
        } catch (e) {
          log(task.id, 'warn', `IndexedDB读取失败: ${e}`);
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

    // 等待音频可以播放
    await new Promise<void>((resolve) => {
      const onCanPlay = () => {
        log(task.id, 'info', '音频可以播放了');
        addLog({ taskId: task.id, event: 'audio_ready', timestamp: Date.now(), duration: Date.now() - startTime });
        audioElement.removeEventListener('canplay', onCanPlay);
        audioElement.removeEventListener('error', onError);
        resolve();
      };
      const onError = (e: Event) => {
        log(task.id, 'warn', `音频加载错误: ${JSON.stringify(e)}`);
        addLog({ taskId: task.id, event: 'audio_error', timestamp: Date.now(), errorSummary: 'Audio load error' });
        audioElement.removeEventListener('canplay', onCanPlay);
        audioElement.removeEventListener('error', onError);
        resolve(); // 即使出错也继续，后续会重试
      };
      audioElement.addEventListener('canplay', onCanPlay);
      audioElement.addEventListener('error', onError);
      // 超时，不要无限等待
      setTimeout(resolve, 2000);
      // 开始加载
      audioElement.load();
    });

    // 尝试播放
    audioElement.play().catch((e) => {
      log(task.id, 'warn', `初始播放失败: ${e}`);
      addLog({ taskId: task.id, event: 'play_error', timestamp: Date.now(), errorSummary: String(e) });
    });

    log(task.id, 'info', '播放已启动');
    addLog({ taskId: task.id, event: 'player_ready', timestamp: Date.now(), duration: Date.now() - startTime });

    return {
      taskId: task.id,
      phase: 'fading-in',
      audio: audioElement,
      targetVolume: (task.volume || 70) / 100,
      retryCount: 0,
      currentBlobUrl: isBlobUrl ? audioUrl : null,
      startedAt: Date.now(),
      scheduledStartAt,
      playedDuration: 0,
      gainNode: null,
      sourceNode: null,
    };
  }

  resumeAudioContext(): void {
    this.ensureAudioContext();
  }

  stopAudio(playback: PlaybackState): void {
    try {
      // 先将音量设为0，立即静音
      playback.audio.volume = 0;
      // 暂停音频
      playback.audio.pause();
      // 清除src属性
      playback.audio.removeAttribute('src');
      playback.audio.src = '';
      // 尝试释放媒体资源（某些浏览器需要）
      try { playback.audio.load(); } catch {}
      // 释放blob URL
      if (playback.currentBlobUrl) URL.revokeObjectURL(playback.currentBlobUrl);
    } catch {}
  }
}

// 音量渐变器
class VolumeFader {
  private currentFade: { timeout: ReturnType<typeof setTimeout> } | null = null;

  // 根据配置播放：如果启用渐入渐出则渐变，否则直接设为目标音量
  applyPlayback(
    playback: PlaybackState,
    config: AudioPlayConfig,
    phase: 'start' | 'end',
    onComplete?: () => void
  ): void {
    if (!config.enableFade) {
      // 禁用模式：直接设置为目标音量
      this.setVolume(playback, config.volume / 100);
      onComplete?.();
      return;
    }

    // 启用模式：正常的渐入渐出逻辑
    if (phase === 'start') {
      this.fadeIn(playback, { duration: config.fadeInDuration, targetVolume: config.volume / 100, onComplete });
    } else {
      this.fadeOut(playback, { duration: config.fadeOutDuration, targetVolume: 0, onComplete });
    }
  }

  fadeIn(playback: PlaybackState, opts: FadeOptions, alreadyElapsed: number = 0): void {
    this.stopFade();
    const start = Date.now();
    const totalDuration = opts.duration * 1000;
    // 确保从0开始渐入
    this.setVolume(playback, 0);
    // 关键：立即尝试播放音频
    playback.audio.play().catch(() => {});

    const tick = () => {
      const elapsed = Date.now() - start + alreadyElapsed;
      const progress = Math.min(1, elapsed / totalDuration);
      const currentVol = opts.targetVolume * progress;
      this.setVolume(playback, currentVol);
      if (progress >= 1) {
        log(playback.taskId, 'info', `渐入完成，音量:${currentVol}`);
        opts.onComplete?.();
      } else {
        this.currentFade = { timeout: setTimeout(tick, 50) };
      }
    };
    tick();
  }

  fadeOut(playback: PlaybackState, opts: FadeOptions, alreadyElapsed: number = 0): void {
    this.stopFade();
    const start = Date.now();
    const totalDuration = opts.duration * 1000;
    const startVol = playback.audio.volume;
    const targetVol = opts.targetVolume ?? 0;

    log(playback.taskId, 'info', `开始渐出，初始音量:${startVol}, 目标音量:${targetVol}, 时长:${opts.duration}秒`);

    const tick = () => {
      const elapsed = Date.now() - start + alreadyElapsed;
      const progress = Math.min(1, elapsed / totalDuration);
      const currentVol = startVol + (targetVol - startVol) * progress;
      this.setVolume(playback, currentVol);
      if (progress >= 1) {
        log(playback.taskId, 'info', `渐出完成，最终音量:${currentVol}`);
        opts.onComplete?.();
      } else {
        this.currentFade = { timeout: setTimeout(tick, 50) };
      }
    };
    tick();
  }

  setVolume(playback: PlaybackState, volume: number): void {
    playback.audio.volume = Math.max(0, Math.min(1, volume));
    // 确保音频正在播放——这是关键！
    if (volume > 0 && playback.audio.paused) {
      playback.audio.play().catch(() => {});
    }
  }

  stopFade(): void {
    if (this.currentFade) {
      clearTimeout(this.currentFade.timeout);
      this.currentFade = null;
    }
  }
}

// 唤醒锁管理器
class WakeLockManager {
  private wakeLock: WakeLockSentinel | null = null;

  async acquire(): Promise<void> {
    try {
      if ('wakeLock' in navigator && !this.wakeLock) {
        this.wakeLock = await navigator.wakeLock.request('screen');
        this.wakeLock.addEventListener('release', () => { this.wakeLock = null; });
      }
    } catch {}
  }

  async release(): Promise<void> {
    if (this.wakeLock) {
      try { await this.wakeLock.release(); } catch {}
      this.wakeLock = null;
    }
  }
}

// 媒体会话管理器 - 用于锁屏和通知中心显示
class MediaSessionManager {
  private currentTask: ScheduledTask | null = null;
  private currentAudio: TaskAudio | null = null;
  private isPlaying: boolean = false;

  setup(): void {
    if (!('mediaSession' in navigator)) {
      console.log('[Scheduler] Media Session API not supported');
      return;
    }

    // 设置播放控制操作
    navigator.mediaSession.setActionHandler('play', () => {
      console.log('[MediaSession] Play action');
      this.resumeCurrent();
    });

    navigator.mediaSession.setActionHandler('pause', () => {
      console.log('[MediaSession] Pause action');
      this.pauseCurrent();
    });

    navigator.mediaSession.setActionHandler('previoustrack', () => {
      console.log('[MediaSession] Previous track');
      this.playPrevious();
    });

    navigator.mediaSession.setActionHandler('nexttrack', () => {
      console.log('[MediaSession] Next track');
      this.playNext();
    });

    // 可选：快进快退
    navigator.mediaSession.setActionHandler('seekforward', (details) => {
      console.log('[MediaSession] Seek forward', details.seekTime);
      this.seekBy(details.seekTime || 10);
    });

    navigator.mediaSession.setActionHandler('seekbackward', (details) => {
      console.log('[MediaSession] Seek backward', details.seekTime);
      this.seekBy(-(details.seekTime || 10));
    });

    console.log('[Scheduler] Media Session setup complete');
  }

  updateMetadata(task: ScheduledTask, currentAudio?: TaskAudio): void {
    if (!('mediaSession' in navigator)) return;

    this.currentTask = task;
    this.currentAudio = currentAudio || task.audios[0];
    const audio = this.currentAudio;

    if (!audio) return;

    try {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: audio.name || '梦枕助眠',
        artist: '梦枕',
        album: '助眠音乐',
        artwork: [
          { src: '/logo-96.png', sizes: '96x96', type: 'image/png' },
          { src: '/logo-128.png', sizes: '128x128', type: 'image/png' },
          { src: '/logo-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/logo-256.png', sizes: '256x256', type: 'image/png' },
          { src: '/logo-384.png', sizes: '384x384', type: 'image/png' },
          { src: '/logo-512.png', sizes: '512x512', type: 'image/png' },
        ]
      });

      // 更新播放状态
      navigator.mediaSession.playbackState = this.isPlaying ? 'playing' : 'paused';
    } catch (e) {
      console.log('[MediaSession] Failed to update metadata:', e);
    }
  }

  updatePlaybackState(playing: boolean): void {
    if (!('mediaSession' in navigator)) return;

    this.isPlaying = playing;
    try {
      navigator.mediaSession.playbackState = playing ? 'playing' : 'paused';
    } catch (e) {
      console.log('[MediaSession] Failed to update playback state:', e);
    }
  }

  private resumeCurrent(): void {
    if (!this.currentTask) return;
    const scheduler = getTaskScheduler();
    scheduler.resumeTask(this.currentTask.id);
  }

  private pauseCurrent(): void {
    if (!this.currentTask) return;
    const scheduler = getTaskScheduler();
    scheduler.cancelTask(this.currentTask.id);
  }

  private playPrevious(): void {
    // 可以实现上一曲逻辑
  }

  private playNext(): void {
    // 可以实现下一曲逻辑
  }

  private seekBy(seconds: number): void {
    // 可以实现快进快退逻辑
  }

  release(): void {
    if ('mediaSession' in navigator) {
      try {
        navigator.mediaSession.metadata = null;
        navigator.mediaSession.playbackState = 'none';

        // 移除所有操作处理程序
        const handlers = ['play', 'pause', 'previoustrack', 'nexttrack', 'seekforward', 'seekbackward'];
        handlers.forEach(handler => {
          navigator.mediaSession.setActionHandler(handler, null);
        });
      } catch (e) {
        console.log('[MediaSession] Failed to release:', e);
      }
    }
  }
}

// 任务缓存
class TaskCache {
  private cache = new Map<string, { task: ScheduledTask; nextExecAt: number | null; cachedAt: number; lastModifiedAt: number }>();
  private lastCount = 0;
  private readonly CACHE_TTL = 3600000;

  sync(): boolean {
    const tasks = getAllTasks();
    if (tasks.length === this.lastCount) {
      let changed = false;
      for (const t of tasks) {
        const c = this.cache.get(t.id);
        if (!c || c.lastModifiedAt < t.updatedAt) { changed = true; break; }
      }
      if (!changed) return false;
    }
    this.cache.clear();
    for (const t of tasks) {
      this.cache.set(t.id, { task: t, nextExecAt: null, cachedAt: 0, lastModifiedAt: t.updatedAt });
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

// 主调度器
interface QueuedTask { taskId: string; nextExecAt: number; task: ScheduledTask; }

class HighPerformanceScheduler {
  private eventEmitter = new EventEmitter();
  private audioPlayer = new AudioPlayer();
  private wakeLockManager = new WakeLockManager();
  private mediaSessionManager = new MediaSessionManager();
  private volumeFader = new VolumeFader();
  private taskCache = new TaskCache();
  private taskQueue = new PriorityQueue<QueuedTask>((a, b) => a.nextExecAt - b.nextExecAt);
  private activePlaybacks = new Map<string, PlaybackState>();
  private nextTimer: ReturnType<typeof setTimeout> | null = null;
  private tickTimer: ReturnType<typeof setTimeout> | null = null;
  private isBackground = false;
  private isRunning = false;
  private readonly CHECK_WINDOW = 1500;
  private readonly MIN_DELAY = 100;

  // 公共 API
  async start(): Promise<void> {
    if (this.isRunning) return;
    this.isRunning = true;
    addLog({ taskId: 'scheduler', event: 'start', timestamp: Date.now() });
    this.setupVisibilityHandler();
    // 初始化媒体会话
    this.mediaSessionManager.setup();
    // 启动时尝试解锁音频播放
    this.tryUnlockAudio();
    await this.resumeActiveTasks();
    this.rebuildQueue();
    this.scheduleNextCheck();
    this.startTickLoop();
    await this.wakeLockManager.acquire();
  }

  tryUnlockAudio(): void {
    this.audioPlayer.tryUnlockAudio();
  }

  stop(): void {
    addLog({ taskId: 'scheduler', event: 'stop', timestamp: Date.now() });
    this.isRunning = false;
    if (this.nextTimer) clearTimeout(this.nextTimer);
    if (this.tickTimer) clearTimeout(this.tickTimer);
    this.activePlaybacks.forEach((_, id) => this.stopPlayback(id));
    this.wakeLockManager.release();
    this.mediaSessionManager.release();
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
    // 立即停止音频播放
    const playback = this.activePlaybacks.get(taskId);
    if (playback) {
      this.volumeFader.stopFade();
      this.audioPlayer.stopAudio(playback);
      this.activePlaybacks.delete(taskId);
    }
    // 释放唤醒锁
    if (this.activePlaybacks.size === 0) this.wakeLockManager.release();
    // 更新任务状态
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
    if (!task || this.activePlaybacks.has(taskId)) return;
    log(taskId, 'info', `立即执行任务，音频数: ${task.audios.length}`);
    for (const audio of task.audios) {
      log(taskId, 'info', `  - ${audio.name}: serverUrl=${!!audio.serverUrl}, fileKey=${!!audio.fileKey}, dbKey=${!!audio.dbKey}`);
    }
    updateTask(taskId, { status: 'executing', lastExecutedAt: Date.now() });
    await this.startPlayback(task, Date.now());
  }

  /** @deprecated 直接使用 cancelTask 即可 */
  forceStopPlayback(taskId: string): void {
    this.cancelTask(taskId);
  }

  getActiveTaskIds(): string[] { return Array.from(this.activePlaybacks.keys()); }

  getTaskPhase(taskId: string): PlayPhase {
    const playback = this.activePlaybacks.get(taskId);
    if (playback) return playback.phase;
    const task = getAllTasks().find(t => t.id === taskId);
    if (!task || task.status === 'cancelled') return 'idle';
    if (task.skipUntil && Date.now() < task.skipUntil) return 'idle';
    if (task.status === 'completed') return 'idle';
    return 'waiting';
  }

  getTaskRemainingMs(taskId: string): number {
    const playback = this.activePlaybacks.get(taskId);
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

  resumeAudioContext(): void {
    this.audioPlayer.resumeAudioContext();
  }

  // 私有方法
  private rebuildQueue(): void {
    this.taskQueue.clear();
    const now = Date.now();
    const maxLookAhead = 24 * 60 * 60 * 1000;
    for (const { task, nextExecAt } of this.taskCache.getPendingTasks()) {
      // 只有启用渐入时才考虑前置时间
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
      const n = this.taskQueue.peek();
      if (!n || n.nextExecAt > windowEnd) break;
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
    if (this.activePlaybacks.has(task.id)) return false;
    const nextExec = this.taskCache.getNextExecAt(task.id);
    if (!nextExec) return false;
    // 只有启用渐入时才考虑前置时间
    const fadeIn = (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;
    const audioStart = nextExec - fadeIn;
    const end = nextExec + task.playDurationMinutes * 60000;

    // 对于未执行过的一次性任务，即使过了结束时间也可以执行
    if (task.repeatType === 'once' && !task.lastExecutedAt) {
      return now >= audioStart;
    }

    return now >= audioStart && now < end;
  }

  private async processTask(task: ScheduledTask): Promise<void> {
    const nextExec = this.taskCache.getNextExecAt(task.id);
    if (!nextExec) return;

    // 对于一次性任务，检查是否已经执行过
    if (task.repeatType === 'once') {
      const end = nextExec + task.playDurationMinutes * 60000;
      // 如果还没有执行过（没有lastExecutedAt），即使时间过了也仍然执行
      if (Date.now() >= end && task.lastExecutedAt) {
        // 只有真正执行过的任务才直接标记完成
        updateTask(task.id, { status: 'completed', completedAt: Date.now() });
        return;
      }
      // 如果没有执行过，继续执行，即使时间已过
    }

    updateTask(task.id, { status: 'executing', lastExecutedAt: Date.now() });
    await this.startPlayback(task, nextExec);
  }

  private async startPlayback(task: ScheduledTask, scheduledStartAt: number): Promise<void> {
    if (this.activePlaybacks.has(task.id)) return;
    log(task.id, 'info', '开始播放...');

    const now = Date.now();
    // 对于未执行过的一次性任务且已过原定时间，使用当前时间作为实际开始时间
    const actualStartAt = (task.repeatType === 'once' && !task.lastExecutedAt && now > scheduledStartAt)
      ? now
      : scheduledStartAt;

    const playback = await this.audioPlayer.playAudio(task, actualStartAt);
    if (!playback) {
      log(task.id, 'error', '无法创建播放器');
      return;
    }

    log(task.id, 'info', `播放已启动，目标音量:${playback.targetVolume}, 启用渐入渐出:${task.enableFade}`);
    this.activePlaybacks.set(task.id, playback);
    this.wakeLockManager.acquire();
    // 更新媒体会话信息
    this.mediaSessionManager.updateMetadata(task);
    this.mediaSessionManager.updatePlaybackState(true);

    // 只有启用渐入时才进行渐入逻辑
    const fadeInMs = task.enableFade ? (task.fadeInDuration || 0) * 1000 : 0;
    const audioStartAt = actualStartAt - fadeInMs;
    const alreadyElapsedFadeIn = Math.max(0, now - audioStartAt);

    addLog({ taskId: task.id, event: 'config_applied', timestamp: Date.now(), provider: task.enableFade ? 'fade-enabled' : 'fade-disabled', metadata: { volume: task.volume, targetVolume: playback.targetVolume, enableFade: task.enableFade } });

    if (task.enableFade && task.fadeInDuration && task.fadeInDuration > 0 && alreadyElapsedFadeIn < fadeInMs) {
      log(task.id, 'info', `开始渐入，时长:${task.fadeInDuration}秒，已过:${alreadyElapsedFadeIn}ms`);
      this.emit('task-started', task.id, 'fading-in', this.computeRemaining(playback), task.name);
      this.volumeFader.fadeIn(playback, {
        duration: task.fadeInDuration,
        targetVolume: playback.targetVolume,
        onComplete: () => {
          log(task.id, 'info', '渐入完成');
          if (this.activePlaybacks.has(task.id)) this.emitPhaseChange(task.id, 'playing');
        },
      }, alreadyElapsedFadeIn);
    } else {
      // 直接设置为目标音量
      log(task.id, 'info', `直接设置音量:${playback.targetVolume}`);
      // 先尝试播放，再设置音量
      playback.audio.play().catch(() => {});
      this.volumeFader.setVolume(playback, playback.targetVolume);
      this.emit('task-started', task.id, 'playing', this.computeRemaining(playback), task.name);
      this.emitPhaseChange(task.id, 'playing');
    }

    this.schedulePlaybackEnd(task, actualStartAt);
  }

  private schedulePlaybackEnd(task: ScheduledTask, scheduledStartAt: number): void {
    const playback = this.activePlaybacks.get(task.id);
    if (!playback) return;
    const now = Date.now();
    const endTime = scheduledStartAt + task.playDurationMinutes * 60000;
    const remaining = Math.max(0, endTime - now);
    this.taskCache.invalidate(task.id);

    setTimeout(() => {
      const p = this.activePlaybacks.get(task.id);
      if (p) this.startFadeOut(task, p, scheduledStartAt);
    }, remaining);
  }

  private startFadeOut(task: ScheduledTask, playback: PlaybackState, scheduledStartAt: number): void {
    // 只有启用渐入时才进行渐出逻辑
    if (task.enableFade && task.fadeOutDuration > 0) {
      this.volumeFader.fadeOut(playback, {
        duration: task.fadeOutDuration,
        targetVolume: 0,
        onComplete: () => this.completeTask(task, playback),
      });
      this.emitPhaseChange(task.id, 'fading-out');
    } else {
      this.completeTask(task, playback);
    }
  }

  private completeTask(task: ScheduledTask, playback: PlaybackState): void {
    addLog({ taskId: task.id, event: 'complete', timestamp: Date.now() });
    this.stopPlayback(task.id);
    updateTask(task.id, task.repeatType === 'once'
      ? { status: 'completed', completedAt: Date.now() }
      : { status: 'pending', lastExecutedAt: Date.now() });
    this.emit('task-completed', task.id, 'idle', 0, task.name);
    if (this.activePlaybacks.size === 0) {
      this.wakeLockManager.release();
      this.mediaSessionManager.updatePlaybackState(false);
    }
    this.taskCache.invalidate(task.id);
    this.rebuildQueue();
    this.scheduleNextCheck();
  }

  private stopPlayback(taskId: string): void {
    const playback = this.activePlaybacks.get(taskId);
    if (!playback) return;
    this.volumeFader.stopFade();
    this.audioPlayer.stopAudio(playback);
    this.activePlaybacks.delete(taskId);

    // 如果没有活跃的播放任务了，更新媒体会话状态
    if (this.activePlaybacks.size === 0) {
      this.mediaSessionManager.updatePlaybackState(false);
    }
  }

  private async resumeActiveTasks(): Promise<void> {
    const tasks = getAllTasks();
    const now = Date.now();
    for (const task of tasks) {
      if (task.status !== 'executing') continue;
      if (this.activePlaybacks.has(task.id)) continue;
      const nextExec = this.taskCache.getNextExecAt(task.id);
      const baseStart = nextExec || getTimestamp(task);
      const end = baseStart + task.playDurationMinutes * 60000;
      // 只有启用渐入时才考虑渐入渐出时间
      const fadeIn = (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;
      const fadeOut = (task.enableFade ? (task.fadeOutDuration || 0) : 0) * 1000;
      const audioStart = baseStart - fadeIn;
      // 对于未执行过的一次性任务，即使过了时间也仍然执行
      if (now < audioStart || (now >= end + fadeOut && task.lastExecutedAt)) {
        updateTask(task.id, task.repeatType === 'once' ? { status: 'completed', completedAt: now } : { status: 'pending' });
        continue;
      }
      await this.startPlayback(task, baseStart);
    }
  }

  private startTickLoop(): void {
    const tick = () => {
      if (!this.isRunning) return;
      const now = Date.now();
      const interval = this.isBackground ? 3000 : 500;

      this.activePlaybacks.forEach((playback, taskId) => {
        const task = getAllTasks().find(t => t.id === taskId);
        if (!task) { this.stopPlayback(taskId); return; }

        const end = playback.scheduledStartAt + task.playDurationMinutes * 60000;
        // 只有启用渐入时才关心渐入结束时间
        const fadeInEnd = playback.scheduledStartAt + (task.enableFade ? (task.fadeInDuration || 0) : 0) * 1000;

        if (playback.phase === 'fading-in' && now >= fadeInEnd) {
          this.volumeFader.setVolume(playback, playback.targetVolume);
          this.emitPhaseChange(taskId, 'playing');
        }
        if (playback.phase === 'playing' && now >= end) {
          this.startFadeOut(task, playback, playback.scheduledStartAt);
        }

        if (playback.audio.paused && (playback.phase === 'playing' || playback.phase === 'fading-in')) {
          if (playback.audio.readyState >= 2) {
            // 确保音量设置正确
            if (playback.phase === 'playing') {
              this.volumeFader.setVolume(playback, playback.targetVolume);
            }
            playback.audio.play().catch((e) => this.handleError(taskId, e));
          }
        }

        this.emit('tick', taskId, playback.phase, this.computeRemaining(playback));
      });

      this.tickTimer = setTimeout(tick, interval);
    };
    tick();
  }

  private async handleError(taskId: string, error: Error): Promise<void> {
    const playback = this.activePlaybacks.get(taskId);
    if (!playback) return;
    const MAX_RETRY = 5;
    const RETRY_DELAY = 1500;
    const task = getAllTasks().find(t => t.id === taskId);
    if (!task) { this.stopPlayback(taskId); return; }

    const classifiedError = classifyError(error);
    log(taskId, 'warn', `处理错误: ${classifiedError.message}, 重试次数: ${playback.retryCount}`);
    addLog({ taskId, event: 'error', timestamp: Date.now(), errorSummary: classifiedError.message });

    // 检测是否是用户交互限制错误
    const isUserInteractionError = error.message.includes('user didn\'t interact') ||
      error.message.includes('user gesture') ||
      error.name === 'NotAllowedError';

    // 如果是用户交互错误，尝试恢复 AudioContext 后继续重试
    if (isUserInteractionError && playback.retryCount === 0) {
      log(taskId, 'info', '检测到浏览器自动播放限制，尝试恢复 AudioContext');
      this.audioPlayer.resumeAudioContext();
      playback.retryCount++;
      // 延迟后重试
      await new Promise(r => setTimeout(r, 500));
      try {
        // 确保音量正确设置
        if (task.enableFade && task.fadeInDuration && task.fadeInDuration > 0) {
          // 如果启用了渐入，重新开始渐入
          this.volumeFader.fadeIn(playback, {
            duration: task.fadeInDuration,
            targetVolume: playback.targetVolume,
            onComplete: () => {
              if (this.activePlaybacks.has(taskId)) this.emitPhaseChange(taskId, 'playing');
            }
          });
        } else {
          // 直接设置音量
          this.volumeFader.setVolume(playback, playback.targetVolume);
        }
        await playback.audio.play();
        // 更新媒体会话状态
        this.mediaSessionManager.updateMetadata(task);
        this.mediaSessionManager.updatePlaybackState(true);
        log(taskId, 'info', `恢复后播放成功，音量:${playback.audio.volume}`);
        return;
      } catch (e) {
        log(taskId, 'warn', '恢复后仍然无法播放，继续重试流程');
      }
    }

    const now = Date.now();
    const end = playback.scheduledStartAt + task.playDurationMinutes * 60000;
    const fadeOut = (task.enableFade ? (task.fadeOutDuration || 0) : 0) * 1000;
    // 只有真正执行过的任务才会在过期后停止
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
          // 确保音量正确设置
          this.volumeFader.setVolume(playback, playback.targetVolume);
          await playback.audio.play();
          // 更新媒体会话状态
          this.mediaSessionManager.updateMetadata(task);
          this.mediaSessionManager.updatePlaybackState(true);
          log(taskId, 'info', `重试播放成功，音量:${playback.audio.volume}`);
        }
      } catch (e) {
        log(taskId, 'warn', `重试失败: ${e}`);
        setTimeout(() => this.handleError(taskId, e instanceof Error ? e : new Error(String(e))), 0);
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
        this.wakeLockManager.acquire();
        this.audioPlayer.resumeAudioContext();

        // 更新媒体会话状态
        this.mediaSessionManager.updatePlaybackState(this.activePlaybacks.size > 0);

        // 恢复所有暂停的音频播放
        this.activePlaybacks.forEach((playback, taskId) => {
          const task = getAllTasks().find(t => t.id === taskId);
          if (!task) return;

          if (playback.audio.paused && playback.audio.readyState >= 2) {
            // 确保音量正确设置
            if (playback.phase === 'playing') {
              this.volumeFader.setVolume(playback, playback.targetVolume);
            } else if (playback.phase === 'fading-in') {
              // 重新开始渐入
              const now = Date.now();
              const fadeInMs = task.enableFade ? (task.fadeInDuration || 0) * 1000 : 0;
              const audioStartAt = playback.scheduledStartAt - fadeInMs;
              const alreadyElapsed = Math.max(0, now - audioStartAt);

              this.volumeFader.fadeIn(playback, {
                duration: task.fadeInDuration || 0,
                targetVolume: playback.targetVolume,
                onComplete: () => {
                  if (this.activePlaybacks.has(taskId)) this.emitPhaseChange(taskId, 'playing');
                }
              }, alreadyElapsed);
            }

            playback.audio.play().catch((e) => {
              log(taskId, 'warn', `可见性变化后播放失败:${e}`);
            });
          }
        });
      } else {
        // 页面进入后台
        this.mediaSessionManager.updatePlaybackState(this.activePlaybacks.size > 0);
      }
    };
    document.addEventListener('visibilitychange', handle);
  }

  private computeRemaining(playback: PlaybackState): number {
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

  private emitPhaseChange(taskId: string, phase: PlayPhase): void {
    const playback = this.activePlaybacks.get(taskId);
    if (playback) {
      playback.phase = phase;
      this.emit('phase-change', taskId, phase, this.computeRemaining(playback));
    }
  }

  private emit(type: string, taskId: string, phase: PlayPhase, remainingMs: number, taskName?: string): void {
    this.eventEmitter.emit({ type: type as any, taskId, phase, remainingMs, taskName });
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
  const s = getTaskScheduler();
  await s.start();
  return s;
}

export function stopTaskScheduler(): void {
  if (schedulerInstance) {
    schedulerInstance.stop();
    schedulerInstance = null;
  }
}

// 导出日志供调试
export function getPlaybackLogs(): PlaybackLog[] {
  return [...logs];
}

export type { SchedulerEvent, PlayPhase, HighPerformanceScheduler };

