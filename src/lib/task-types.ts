export type TaskRepeatType = 'once' | 'workday' | 'holiday' | 'daily';

export interface TaskAudio {
  id: string;
  name: string;
  duration: number;
  size: number;
  fileKey?: string;
  serverUrl?: string;
  dbKey?: string;
}

export type TaskStatus = 'pending' | 'executing' | 'completed' | 'cancelled';

export type TaskExecPhase = 'waiting' | 'fading-in' | 'playing' | 'fading-out';

export interface ScheduledTask {
  id: string;
  name: string;
  startTime: {
    year: number;
    month: number;
    day: number;
    hour: number;
    minute: number;
    second: number;
  };
  playDurationMinutes: number;
  fadeInDuration: number;
  fadeOutDuration: number;
  volume: number;
  repeatType: TaskRepeatType;
  audios: TaskAudio[];
  status: TaskStatus;
  createdAt: number;
  lastExecutedAt?: number;
  nextExecuteAt?: number;
  completedAt?: number;
  skipUntil?: number;
}

export type PlayMode = 'default' | 'custom';

export interface PlayConfig {
  mode: PlayMode;
  startTime: {
    year: number;
    month: number;
    day: number;
    hour: number;
    minute: number;
    second: number;
  };
  playDurationMinutes: number;
  fadeInDuration: number;
  fadeOutDuration: number;
  volume: number;
  audios: TaskAudio[];
  taskId?: string;
}

export const STEP_DURATION = 5;

export function generateTaskId(): string {
  return `task_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

export function isWorkday(date: Date): boolean {
  const day = date.getDay();
  if (day === 0 || day === 6) return false;
  return !isChineseHoliday(date);
}

export function isChineseHoliday(date: Date): boolean {
  const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  const holidays2025: Record<string, boolean> = {
    '2025-01-01': true,
    '2025-01-28': true, '2025-01-29': true, '2025-01-30': true,
    '2025-01-31': true, '2025-02-01': true, '2025-02-02': true,
    '2025-02-03': true, '2025-02-04': true,
    '2025-04-04': true, '2025-04-05': true, '2025-04-06': true,
    '2025-05-01': true, '2025-05-02': true, '2025-05-03': true,
    '2025-05-04': true, '2025-05-05': true,
    '2025-05-31': true, '2025-06-01': true, '2025-06-02': true,
    '2025-10-01': true, '2025-10-02': true, '2025-10-03': true,
    '2025-10-04': true, '2025-10-05': true, '2025-10-06': true,
    '2025-10-07': true, '2025-10-08': true,
  };
  const holidays2026: Record<string, boolean> = {
    '2026-01-01': true, '2026-01-02': true, '2026-01-03': true,
    '2026-02-16': true, '2026-02-17': true, '2026-02-18': true,
    '2026-02-19': true, '2026-02-20': true,
    '2026-04-04': true, '2026-04-05': true, '2026-04-06': true,
    '2026-05-01': true, '2026-05-02': true, '2026-05-03': true,
    '2026-05-04': true, '2026-05-05': true,
    '2026-06-19': true, '2026-06-20': true, '2026-06-21': true,
    '2026-10-01': true, '2026-10-02': true, '2026-10-03': true,
    '2026-10-04': true, '2026-10-05': true, '2026-10-06': true,
    '2026-10-07': true,
  };
  return !!(holidays2025[key] || holidays2026[key]);
}

export function shouldExecuteOnDate(task: ScheduledTask, date: Date): boolean {
  switch (task.repeatType) {
    case 'once':
      return date.getFullYear() === task.startTime.year &&
        date.getMonth() + 1 === task.startTime.month &&
        date.getDate() === task.startTime.day;
    case 'daily':
      return true;
    case 'workday':
      return isWorkday(date);
    case 'holiday':
      return isChineseHoliday(date) || date.getDay() === 0 || date.getDay() === 6;
    default:
      return false;
  }
}

export function getNextExecuteDate(task: ScheduledTask, fromDate?: Date): Date | null {
  const start = fromDate ?? new Date();
  const checkDate = new Date(start);

  for (let i = 0; i < 366; i++) {
    if (i > 0) {
      checkDate.setDate(checkDate.getDate() + 1);
      checkDate.setHours(task.startTime.hour, task.startTime.minute, task.startTime.second);
    }

    if (shouldExecuteOnDate(task, checkDate)) {
      if (checkDate.getTime() >= start.getTime() - 60000) {
        return new Date(
          checkDate.getFullYear(),
          checkDate.getMonth(),
          checkDate.getDate(),
          task.startTime.hour,
          task.startTime.minute,
          task.startTime.second
        );
      }
    }
  }
  return null;
}

export function getTaskStatus(task: ScheduledTask): TaskStatus {
  const now = Date.now();
  const startTime = new Date(
    task.startTime.year,
    task.startTime.month - 1,
    task.startTime.day,
    task.startTime.hour,
    task.startTime.minute,
    task.startTime.second
  ).getTime();
  const endTime = startTime + task.playDurationMinutes * 60 * 1000;

  if (task.status === 'cancelled') return 'cancelled';

  if (task.skipUntil && now < task.skipUntil) return 'cancelled';

  if (task.repeatType === 'once') {
    if (now >= endTime) return 'completed';
    if (now >= startTime) return 'executing';
    return 'pending';
  }

  const nextExec = getNextExecuteDate(task);
  if (!nextExec) return 'completed';

  const nextStart = nextExec.getTime();
  const nextEnd = nextStart + task.playDurationMinutes * 60 * 1000;

  if (now >= nextStart && now < nextEnd) return 'executing';
  return 'pending';
}

export function formatRelativeTime(ms: number): string {
  if (ms <= 0) return '0秒';
  const absMs = Math.abs(ms);
  const days = Math.floor(absMs / (24 * 3600 * 1000));
  const hours = Math.floor((absMs % (24 * 3600 * 1000)) / (3600 * 1000));
  const minutes = Math.floor((absMs % (3600 * 1000)) / (60 * 1000));
  const seconds = Math.floor((absMs % (60 * 1000)) / 1000);

  const parts: string[] = [];
  if (days > 0) parts.push(`${days}天`);
  if (hours > 0) parts.push(`${hours}小时`);
  if (minutes > 0) parts.push(`${minutes}分钟`);
  if (seconds > 0 || parts.length === 0) parts.push(`${seconds}秒`);
  return parts.join('');
}

export function formatElapsedTime(ms: number): string {
  const absMs = Math.abs(ms);
  const hours = Math.floor(absMs / (3600 * 1000));
  const minutes = Math.floor((absMs % (3600 * 1000)) / (60 * 1000));
  const seconds = Math.floor((absMs % (60 * 1000)) / 1000);

  const parts: string[] = [];
  if (hours > 0) parts.push(`${hours}小时`);
  parts.push(`${minutes}分钟`);
  parts.push(`${seconds}秒`);
  return `已执行${parts.join('')}`;
}

export const REPEAT_TYPE_LABELS: Record<TaskRepeatType, string> = {
  once: '一次性',
  workday: '法定工作日',
  holiday: '法定节假日',
  daily: '每天',
};
