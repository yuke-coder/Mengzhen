"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { schedulerLog, type LogEntry, type LogLevel } from "@/lib/scheduler-log";
import { getTaskScheduler } from "@/lib/task-scheduler";
import { cn } from "@/lib/utils";

const LEVEL_COLORS: Record<LogLevel, string> = {
  info: "text-blue-400",
  warn: "text-amber-400",
  error: "text-red-400",
  debug: "text-zinc-400",
};

function formatTime(ts: number): string {
  const d = new Date(ts);
  return `${d.getHours().toString().padStart(2, "0")}:${d.getMinutes().toString().padStart(2, "0")}:${d.getSeconds().toString().padStart(2, "0")}.${d.getMilliseconds().toString().padStart(3, "0")}`;
}

export function SchedulerDebugPanel() {
  const [open, setOpen] = useState(false);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [filter, setFilter] = useState<LogLevel | "all">("all");
  const [autoScroll, setAutoScroll] = useState(true);
  const logEndRef = useRef<HTMLDivElement>(null);
  const tapCountRef = useRef(0);
  const tapTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setLogs(schedulerLog.getAll());
    const unsub = schedulerLog.onEntry(() => {
      setLogs(schedulerLog.getAll());
    });
    return unsub;
  }, []);

  useEffect(() => {
    if (autoScroll && logEndRef.current) {
      logEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [logs, autoScroll]);

  const handleSecretTap = useCallback(() => {
    tapCountRef.current++;
    if (tapTimerRef.current) clearTimeout(tapTimerRef.current);
    tapTimerRef.current = setTimeout(() => {
      tapCountRef.current = 0;
    }, 1000);
    if (tapCountRef.current >= 5) {
      tapCountRef.current = 0;
      setOpen(prev => !prev);
    }
  }, []);

  const filteredLogs = filter === "all" ? logs : logs.filter(l => l.level === filter);

  const scheduler = getTaskScheduler();
  const activeIds = scheduler.getActiveTaskIds();

  const handleExport = useCallback(() => {
    const text = logs
      .map(l => {
        const dataStr = l.data ? ` ${JSON.stringify(l.data)}` : "";
        return `[${formatTime(l.ts)}] [${l.level.toUpperCase()}] [${l.tag}] ${l.msg}${dataStr}`;
      })
      .join("\n");
    const blob = new Blob([text], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `scheduler-log-${new Date().toISOString().slice(0, 19).replace(/:/g, "-")}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }, [logs]);

  if (!open) {
    return (
      <div
        onClick={handleSecretTap}
        className="fixed bottom-2 right-2 w-8 h-8 z-50 cursor-pointer"
        title="连点5次打开调试面板"
      />
    );
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex flex-col">
      <div className="flex items-center justify-between px-3 py-2 border-b border-border/40 bg-zinc-900/90">
        <div className="flex items-center gap-2">
          <h2 className="text-sm font-medium text-foreground">调度器调试</h2>
          <span className="text-[11px] text-muted-foreground">
            活跃: {activeIds.length} | 日志: {filteredLogs.length}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleExport}
            className="text-[11px] px-2 py-1 rounded bg-zinc-700 text-zinc-300 active:bg-zinc-600"
          >
            导出
          </button>
          <button
            onClick={() => schedulerLog.clear()}
            className="text-[11px] px-2 py-1 rounded bg-zinc-700 text-zinc-300 active:bg-zinc-600"
          >
            清空
          </button>
          <button
            onClick={() => setOpen(false)}
            className="text-[11px] px-2 py-1 rounded bg-red-500/20 text-red-400 active:bg-red-500/30"
          >
            关闭
          </button>
        </div>
      </div>

      <div className="flex items-center gap-1 px-3 py-1.5 border-b border-border/20 bg-zinc-900/60">
        {(["all", "info", "warn", "error", "debug"] as const).map(lv => (
          <button
            key={lv}
            onClick={() => setFilter(lv)}
            className={cn(
              "text-[11px] px-2 py-0.5 rounded transition-colors",
              filter === lv
                ? "bg-white/10 text-white"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            {lv === "all" ? "全部" : lv.toUpperCase()}
          </button>
        ))}
        <div className="flex-1" />
        <label className="flex items-center gap-1 text-[11px] text-muted-foreground">
          <input
            type="checkbox"
            checked={autoScroll}
            onChange={e => setAutoScroll(e.target.checked)}
            className="w-3 h-3"
          />
          自动滚动
        </label>
      </div>

      {activeIds.length > 0 && (
        <div className="px-3 py-1.5 border-b border-border/20 bg-zinc-900/40">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-[11px] text-muted-foreground">活跃任务:</span>
            {activeIds.map(id => {
              const phase = scheduler.getTaskPhase(id);
              const remaining = scheduler.getTaskRemainingMs(id);
              const sec = Math.floor(remaining / 1000);
              return (
                <span
                  key={id}
                  className={cn(
                    "text-[11px] px-1.5 py-0.5 rounded",
                    phase === "playing" && "bg-emerald-500/10 text-emerald-400",
                    phase === "fading-in" && "bg-blue-500/10 text-blue-400",
                    phase === "fading-out" && "bg-amber-500/10 text-amber-400",
                    phase === "waiting" && "bg-zinc-500/10 text-zinc-400",
                  )}
                >
                  {id.slice(0, 6)} {phase} {sec}s
                </span>
              );
            })}
          </div>
        </div>
      )}

      <div className="flex-1 overflow-y-auto px-2 py-1 font-mono text-[11px] leading-relaxed">
        {filteredLogs.length === 0 && (
          <div className="text-center text-muted-foreground/50 py-8">
            暂无日志
          </div>
        )}
        {filteredLogs.map((entry, i) => (
          <div
            key={`${entry.ts}-${i}`}
            className={cn(
              "flex gap-1.5 px-1 py-0.5 rounded-sm",
              i % 2 === 0 ? "bg-transparent" : "bg-white/[0.02]"
            )}
          >
            <span className="text-muted-foreground/50 shrink-0">{formatTime(entry.ts)}</span>
            <span className={cn("shrink-0 w-10 text-right", LEVEL_COLORS[entry.level])}>
              {entry.level.toUpperCase().slice(0, 4)}
            </span>
            <span className="shrink-0 text-[var(--brand-start)]/70 w-14 text-right">{entry.tag}</span>
            <span className="text-foreground/80">{entry.msg}</span>
            {entry.data && (
              <span className="text-muted-foreground/50 truncate">
                {JSON.stringify(entry.data)}
              </span>
            )}
          </div>
        ))}
        <div ref={logEndRef} />
      </div>
    </div>
  );
}
