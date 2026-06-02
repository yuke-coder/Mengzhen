type LogLevel = 'info' | 'warn' | 'error' | 'debug';

interface LogEntry {
  ts: number;
  level: LogLevel;
  tag: string;
  msg: string;
  data?: Record<string, unknown>;
}

const MAX_LOG_ENTRIES = 500;

const entries: LogEntry[] = [];

const listeners = new Set<(entry: LogEntry) => void>();

function addEntry(level: LogLevel, tag: string, msg: string, data?: Record<string, unknown>) {
  const entry: LogEntry = {
    ts: Date.now(),
    level,
    tag,
    msg,
    data,
  };

  entries.push(entry);
  if (entries.length > MAX_LOG_ENTRIES) {
    entries.splice(0, entries.length - MAX_LOG_ENTRIES);
  }

  listeners.forEach(cb => {
    try { cb(entry); } catch {}
  });

  if (level === 'error') {
    console.error(`[SchedulerLog] ${tag}: ${msg}`, data || '');
  } else if (level === 'warn') {
    console.warn(`[SchedulerLog] ${tag}: ${msg}`, data || '');
  }
}

export const schedulerLog = {
  info: (tag: string, msg: string, data?: Record<string, unknown>) => addEntry('info', tag, msg, data),
  warn: (tag: string, msg: string, data?: Record<string, unknown>) => addEntry('warn', tag, msg, data),
  error: (tag: string, msg: string, data?: Record<string, unknown>) => addEntry('error', tag, msg, data),
  debug: (tag: string, msg: string, data?: Record<string, unknown>) => addEntry('debug', tag, msg, data),

  getAll: (): LogEntry[] => [...entries],
  clear: () => { entries.length = 0; },
  onEntry: (cb: (entry: LogEntry) => void): (() => void) => {
    listeners.add(cb);
    return () => { listeners.delete(cb); };
  },
  getEntriesSince: (sinceTs: number): LogEntry[] => {
    const idx = entries.findIndex(e => e.ts > sinceTs);
    if (idx < 0) return [];
    return entries.slice(idx);
  },
};

export type { LogEntry, LogLevel };
