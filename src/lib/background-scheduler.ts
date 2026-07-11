"use client";

import { ScheduledTask, TaskStatus } from "@/lib/task-types";
import UnifiedAudioManager from "./audio";

export class EnhancedTaskScheduler {
  private static instance: EnhancedTaskScheduler;
  private tasks = new Map<string, ScheduledTask>();
  private timers = new Map<string, number>();
  private audioManager = UnifiedAudioManager.getInstance();

  private constructor() {}

  public static getInstance(): EnhancedTaskScheduler {
    if (!EnhancedTaskScheduler.instance) {
      EnhancedTaskScheduler.instance = new EnhancedTaskScheduler();
    }
    return EnhancedTaskScheduler.instance;
  }

  async initializeAudioContext(): Promise<boolean> {
    return this.audioManager.initializeAudioContext();
  }

  async initialize() {
    await this.audioManager.initialize();
    if (UnifiedAudioManager.isAndroidDevice()) {
      await this.audioManager.requestWakeLock();
    }
  }

  destroy() {
    this.timers.forEach(timer => clearTimeout(timer));
    this.timers.clear();
    this.tasks.clear();
    this.audioManager.destroy();
  }

  addTask(task: ScheduledTask) {
    this.tasks.set(task.id, task);
    this.scheduleTask(task);
  }

  updateTask(taskId: string, updates: Partial<ScheduledTask>) {
    const task = this.tasks.get(taskId);
    if (task) {
      this.cancelTask(taskId);
      this.tasks.set(taskId, { ...task, ...updates });
      this.scheduleTask(this.tasks.get(taskId)!);
    }
  }

  cancelTask(taskId: string) {
    const timer = this.timers.get(taskId);
    if (timer) clearTimeout(timer);
    this.timers.delete(taskId);
    this.audioManager.stopAudio(taskId);
  }

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

  private async executeTask(taskId: string) {
    const task = this.tasks.get(taskId);
    if (!task || task.status !== 'pending') return;

    this.updateTaskStatus(taskId, 'executing');
    this.timers.delete(taskId);

    try {
      if (!this.audioManager.isInitialized()) {
        await this.audioManager.initializeAudioContext();
      }

      if (this.audioManager.getAudioState() === 'suspended') {
        await this.audioManager.resumeAudioContext();
      }

      await this.audioManager.playAudio(task, Date.now());

      const totalRuntime = (task.playDurationMinutes * 60 + task.fadeOutDuration) * 1000;
      const timerId = window.setTimeout(() => this.completeTask(taskId), totalRuntime);
      this.timers.set(taskId, timerId);

    } catch (error) {
      console.error('[Scheduler] 执行任务失败:', error);
      this.updateTaskStatus(taskId, 'cancelled');
    }
  }

  private completeTask(taskId: string) {
    this.audioManager.stopAudio(taskId);
    this.updateTaskStatus(taskId, 'completed');
    this.timers.delete(taskId);
  }

  private updateTaskStatus(taskId: string, status: TaskStatus) {
    const task = this.tasks.get(taskId);
    if (task) {
      this.tasks.set(taskId, { ...task, status });
      window.dispatchEvent(new CustomEvent('task-status-update', {
        detail: { taskId, status, task: this.tasks.get(taskId) }
      }));
    }
  }

  restoreAllSavedTasks() {}

  getAudioState(): string | undefined {
    return this.audioManager.getAudioState();
  }

  async requestWakeLock(): Promise<boolean> {
    return this.audioManager.requestWakeLock();
  }
}

export default EnhancedTaskScheduler;

