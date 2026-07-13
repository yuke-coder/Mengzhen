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
  enableFade: boolean; // 新增：是否启用音量渐入渐出
  volume: number;
  repeatType: TaskRepeatType;
  audios: TaskAudio[];
  status: TaskStatus;
  createdAt: number;
  lastExecutedAt?: number;
  nextExecuteAt?: number;
  completedAt?: number;
  skipUntil?: number;
  updatedAt: number; // 添加更新时间戳用于缓存失效
}

/** 默认设置与新建任务共同编辑的播放配置。 */
export type PlaybackDraft = Pick<
  ScheduledTask,
  'audios' | 'volume' | 'fadeInDuration' | 'fadeOutDuration' | 'enableFade'
>;

export type PlayMode = 'default' | 'custom';

// 播放配置接口
export interface AudioPlayConfig {
  enableFade: boolean;
  fadeInDuration: number; // 秒，仅当enableFade为true时生效
  fadeOutDuration: number; // 秒，仅当enableFade为true时生效
  volume: number; // 0-100
}

export const STEP_DURATION = 5;

// 节假日数据缓存
const HOLIDAYS = new Map<string, boolean>([
  // 2025
  ['2025-01-01', true],
  ['2025-01-28', true], ['2025-01-29', true], ['2025-01-30', true],
  ['2025-01-31', true], ['2025-02-01', true], ['2025-02-02', true],
  ['2025-02-03', true], ['2025-02-04', true],
  ['2025-04-04', true], ['2025-04-05', true], ['2025-04-06', true],
  ['2025-05-01', true], ['2025-05-02', true], ['2025-05-03', true],
  ['2025-05-04', true], ['2025-05-05', true],
  ['2025-05-31', true], ['2025-06-01', true], ['2025-06-02', true],
  ['2025-10-01', true], ['2025-10-02', true], ['2025-10-03', true],
  ['2025-10-04', true], ['2025-10-05', true], ['2025-10-06', true],
  ['2025-10-07', true], ['2025-10-08', true],
  // 2026
  ['2026-01-01', true], ['2026-01-02', true], ['2026-01-03', true],
  ['2026-02-16', true], ['2026-02-17', true], ['2026-02-18', true],
  ['2026-02-19', true], ['2026-02-20', true],
  ['2026-04-04', true], ['2026-04-05', true], ['2026-04-06', true],
  ['2026-05-01', true], ['2026-05-02', true], ['2026-05-03', true],
  ['2026-05-04', true], ['2026-05-05', true],
  ['2026-06-19', true], ['2026-06-20', true], ['2026-06-21', true],
  ['2026-10-01', true], ['2026-10-02', true], ['2026-10-03', true],
  ['2026-10-04', true], ['2026-10-05', true], ['2026-10-06', true],
  ['2026-10-07', true],
]);

// 日期键缓存
function getDateKey(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function generateTaskId(): string {
  return `task_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

export function isWorkday(date: Date): boolean {
  const day = date.getDay();
  if (day === 0 || day === 6) return false;
  return !isChineseHoliday(date);
}

export function isChineseHoliday(date: Date): boolean {
  return HOLIDAYS.has(getDateKey(date));
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

// 下次执行时间缓存
const nextExecCache = new Map<string, { value: number; at: number; taskUpdatedAt: number }>();
const CACHE_TTL = 3600000; // 1小时

export function getNextExecuteDate(task: ScheduledTask, fromDate?: Date): Date | null {
  const now = Date.now();
  const cacheKey = `${task.id}-${task.updatedAt}`;

  // 尝试使用缓存（只对非一次性任务缓存）
  if (task.repeatType !== 'once') {
    const cached = nextExecCache.get(cacheKey);
    if (cached && cached.taskUpdatedAt === task.updatedAt && now - cached.at < CACHE_TTL) {
      // 验证缓存是否仍然有效
      if (cached.value > now - 60000) {
        return new Date(cached.value);
      }
    }
  }

  const start = fromDate ?? new Date();
  const checkDate = new Date(start);
  const taskTime = task.startTime;

  // 直接设置时间，避免多次 setDate
  if (task.repeatType === 'once') {
    const candidate = new Date(
      taskTime.year, taskTime.month - 1, taskTime.day,
      taskTime.hour, taskTime.minute, taskTime.second
    );
    // 对于未执行过的一次性任务，即使过期了也仍然返回执行时间
    if (candidate.getTime() >= start.getTime() - 60000 || !task.lastExecutedAt) {
      return candidate;
    }
    return null;
  }

  // 对于重复任务，快速查找
  for (let i = 0; i < 366; i++) {
    if (i > 0) {
      checkDate.setDate(checkDate.getDate() + 1);
    }
    checkDate.setHours(taskTime.hour, taskTime.minute, taskTime.second, 0);

    if (shouldExecuteOnDate(task, checkDate)) {
      const candidateTime = checkDate.getTime();
      if (candidateTime >= start.getTime() - 60000) {
        const result = new Date(candidateTime);
        // 缓存结果（这里只处理非一次性任务）
        nextExecCache.set(cacheKey, { value: candidateTime, at: now, taskUpdatedAt: task.updatedAt });
        return result;
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
    // 只有真正执行过的任务（有lastExecutedAt）才会在过期后标记为completed
    if (now >= endTime && task.lastExecutedAt) return 'completed';
    // 如果还没执行过，即使过了时间也仍然是pending/executing状态
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
