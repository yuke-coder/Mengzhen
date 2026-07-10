import { ScheduledTask, TaskStatus } from "@/lib/task-types";
import AudioManager from "./audio-manager";
import { unlockAudio } from "./audio-unlock";

export class EnhancedTaskScheduler {
  private static instance: EnhancedTaskScheduler;
  private tasks = new Map<string, ScheduledTask>();
  private timers = new Map<string, number>();
  private audioManager = AudioManager.getInstance();

  private constructor() {}

  public static getInstance(): EnhancedTaskScheduler {
    if (!EnhancedTaskScheduler.instance) {
      EnhancedTaskScheduler.instance = new EnhancedTaskScheduler();
    }
    return EnhancedTaskScheduler.instance;
  }

  // 手动初始化AudioContext（需要用户交互）
  async initializeAudioContext(): Promise<boolean> {
    return this.audioManager.initialize();
  }

  // 自动初始化
  async initialize() {
    await this.audioManager.initialize();
    if (this.isAndroidDevice()) {
      await this.audioManager.requestWakeLock();
    }
  }

  // 销毁
  destroy() {
    this.timers.forEach(timer => clearTimeout(timer));
    this.timers.clear();
    this.tasks.clear();
    this.audioManager.destroy();
  }

  // 添加任务
  addTask(task: ScheduledTask) {
    this.tasks.set(task.id, task);
    this.scheduleTask(task);
  }

  // 更新任务
  updateTask(taskId: string, updates: Partial<ScheduledTask>) {
    const task = this.tasks.get(taskId);
    if (task) {
      this.cancelTask(taskId);
      this.tasks.set(taskId, { ...task, ...updates });
      this.scheduleTask(this.tasks.get(taskId)!);
    }
  }

  // 取消任务
  cancelTask(taskId: string) {
    const timer = this.timers.get(taskId);
    if (timer) clearTimeout(timer);
    this.timers.delete(taskId);
    this.audioManager.stopAudio(taskId);
  }

  // 调度任务
  private scheduleTask(task: ScheduledTask) {
    if (task.status !== 'pending') return;

    const delay = this.getTaskExecutionDelay(task);
    if (delay <= 0) {
      this.executeTask(task.id);
      return;
    }

    const timerId = window.setTimeout(() => this.executeTask(task.id), delay);
    this.timers.set(task.id, timerId);
  }

  // 获取任务执行延迟
  private getTaskExecutionDelay(task: ScheduledTask): number {
    const startDate = new Date(
      task.startTime.year,
      task.startTime.month - 1,
      task.startTime.day,
      task.startTime.hour,
      task.startTime.minute,
      task.startTime.second
    );
    const fadeInMs = task.enableFade ? task.fadeInDuration * 1000 : 0;
    return startDate.getTime() - Date.now() - fadeInMs;
  }

  // 执行任务
  private async executeTask(taskId: string) {
    const task = this.tasks.get(taskId);
    if (!task || task.status !== 'pending') return;

    this.updateTaskStatus(taskId, 'executing');
    this.timers.delete(taskId);

    try {
      // 确保音频系统已初始化
      if (!this.audioManager.isInitialized()) {
        await unlockAudio();
        await this.audioManager.initialize();
      }

      // 确保音频上下文活跃
      if (this.audioManager.getAudioState() === 'suspended') {
        await this.audioManager.initializeAudioContext();
      }

      // 播放音频
      for (const audio of task.audios) {
        await this.audioManager.playAudio({
          id: task.id,
          url: audio.url || audio.serverUrl || '',
          volume: task.volume || 50,
          fadeInDuration: task.enableFade ? task.fadeInDuration : 0,
          fadeOutDuration: task.enableFade ? task.fadeOutDuration : 0,
          playDurationMinutes: task.playDurationMinutes
        });
      }

      // 设置结束定时器
      const totalRuntime = (task.playDurationMinutes * 60 + task.fadeOutDuration) * 1000;
      const timerId = window.setTimeout(() => this.completeTask(taskId), totalRuntime);
      this.timers.set(taskId, timerId);

    } catch (error) {
      console.error('[Scheduler] 执行任务失败:', error);
      this.updateTaskStatus(taskId, 'failed');
    }
  }

  // 完成任务
  private completeTask(taskId: string) {
    this.audioManager.stopAudio(taskId);
    this.updateTaskStatus(taskId, 'completed');
    this.timers.delete(taskId);
  }

  // 更新任务状态
  private updateTaskStatus(taskId: string, status: TaskStatus) {
    const task = this.tasks.get(taskId);
    if (task) {
      this.tasks.set(taskId, { ...task, status });
      window.dispatchEvent(new CustomEvent('task-status-update', {
        detail: { taskId, status, task: this.tasks.get(taskId) }
      }));
    }
  }

  // 工具方法
  private isAndroidDevice(): boolean {
    return /android/i.test(navigator.userAgent.toLowerCase());
  }

  // 恢复所有保存的任务播放状态
  restoreAllSavedTasks() {
    // 可以在这里实现恢复所有任务的播放状态
    // 目前由AudioManager内部处理
  }

  // 获取音频状态（用于调试）
  getAudioState(): string | undefined {
    return this.audioManager.getAudioState();
  }

  // 请求唤醒锁（安卓专用）
  async requestWakeLock(): Promise<boolean> {
    return this.audioManager.requestWakeLock();
  }
}

export default EnhancedTaskScheduler;