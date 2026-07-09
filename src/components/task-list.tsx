"use client";

import React, { useState, useCallback, useEffect, useMemo, useRef } from "react";
import {
  ScheduledTask,
  TaskStatus,
  TaskExecPhase,
  REPEAT_TYPE_LABELS,
  getTaskStatus,
  getNextExecuteDate,
  TaskAudio,
} from "@/lib/task-types";
import { deleteTask, getAllTasks, saveAllTasks } from "@/lib/task-store";
import { getTaskScheduler, type SchedulerEvent } from "@/lib/task-scheduler";
import { FeedbackModal } from "@/components/feedback-modal";
import { cn } from "@/lib/utils";
import { toast } from "@/components/sonner";
import { formatFileSize, formatDuration } from "@/lib/audio-utils";
import {
  Trash2,
  Edit3,
  XCircle,
  Clock,
  Music2,
  Repeat,
  CalendarClock,
  CheckCircle2,
  PauseCircle,
  Volume2,
  HardDrive,
  GripVertical,
  Play,
  PlayCircle,
  TrendingUp,
  TrendingDown,
  RotateCcw,
  Plus,
} from "lucide-react";
import { useIsMobile } from "@/hooks/use-mobile";

interface TaskListProps {
  tasks: ScheduledTask[];
  onEdit: (task: ScheduledTask) => void;
  onCreate?: () => void;
  onRefresh: () => void;
}

function formatCountdownTime(ms: number): string {
  if (ms <= 0) return "0:00";
  const totalSec = Math.floor(ms / 1000);
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  if (h > 0) {
    return `${h}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  }
  return `${m}:${s.toString().padStart(2, "0")}`;
}

function StatusBadge({ task, status, phase, remainingMs }: { task: ScheduledTask; status: TaskStatus; phase: TaskExecPhase | "idle"; remainingMs: number }) {

  const relativeTime = useMemo(() => {
    if (status === "pending") {
      return `等待中 ${formatCountdownTime(remainingMs)}`;
    } else if (status === "executing") {
      if (phase === "fading-in") {
        return `渐入中 ${formatCountdownTime(remainingMs)}`;
      } else if (phase === "playing") {
        return `播放中 ${formatCountdownTime(remainingMs)}`;
      } else if (phase === "fading-out") {
        return `渐出中 ${formatCountdownTime(remainingMs)}`;
      } else {
        return `播放中 ${formatCountdownTime(remainingMs)}`;
      }
    } else if (status === "completed") {
      if (task.repeatType !== "once") {
        return `已完成 ${formatCountdownTime(remainingMs)}`;
      }
      return "已完成";
    } else {
      if (task.skipUntil && remainingMs > 0) {
        return `已取消 ${formatCountdownTime(remainingMs)}`;
      }
      return "已取消";
    }
  }, [status, phase, remainingMs, task.repeatType, task.skipUntil]);

  const config = useMemo(() => {
    if (status === "pending") {
      return { icon: Clock, color: "text-blue-400", bg: "bg-blue-500/10", border: "border-blue-500/20" };
    }
    if (status === "executing") {
      if (phase === "fading-in") {
        return { icon: TrendingUp, color: "text-cyan-400", bg: "bg-cyan-500/10", border: "border-cyan-500/20" };
      }
      if (phase === "fading-out") {
        return { icon: TrendingDown, color: "text-amber-400", bg: "bg-amber-500/10", border: "border-amber-500/20" };
      }
      return { icon: Play, color: "text-emerald-400", bg: "bg-emerald-500/10", border: "border-emerald-500/20" };
    }
    if (status === "completed") {
      return { icon: CheckCircle2, color: "text-zinc-400", bg: "bg-zinc-500/10", border: "border-zinc-500/20" };
    }
    return { icon: PauseCircle, color: "text-amber-400", bg: "bg-amber-500/10", border: "border-amber-500/20" };
  }, [status, phase]);

  const Icon = config.icon;

  return (
    <div className={cn("inline-flex items-center gap-1.5 px-2 py-1 rounded-md text-xs font-medium border", config.bg, config.border, config.color)}>
      <Icon className={cn("w-3 h-3", status === "executing" && phase === "playing" && "animate-pulse")} />
      <span>{relativeTime}</span>
    </div>
  );
}

function audioInfoDisplay(audio: TaskAudio) {
  const info = [];
  if (audio.size > 0) {
    info.push(
      <span key="size" className="flex items-center gap-0.5 text-[11px] sm:text-[10px] text-muted-foreground/60">
        <HardDrive className="w-3 h-3 sm:w-2.5 sm:h-2.5" />
        {formatFileSize(audio.size)}
      </span>
    );
  }
  if (audio.duration > 0) {
    info.push(
      <span key="duration" className="text-[11px] sm:text-[10px] text-muted-foreground/60">
        {formatDuration(audio.duration)}
      </span>
    );
  }
  if (info.length > 0) {
    return (
      <span className="flex items-center gap-1 ml-2">
        {info.map((item, idx) => (
          <React.Fragment key={idx}>
            {idx > 0 && <span className="text-muted-foreground/30">·</span>}
            {item}
          </React.Fragment>
        ))}
      </span>
    );
  }
  return null;
}

function useTaskState(task: ScheduledTask) {
  const scheduler = useMemo(() => getTaskScheduler(), []);
  const [state, setState] = useState<{ status: TaskStatus; phase: TaskExecPhase | "idle"; remainingMs: number }>(() => {
    const phase = scheduler.getTaskPhase(task.id);
    let status: TaskStatus;
    if (phase === 'fading-in' || phase === 'playing' || phase === 'fading-out') {
      status = 'executing';
    } else {
      status = getTaskStatus(task);
    }
    return {
      status,
      phase: phase === 'waiting' ? 'waiting' : phase === 'idle' ? 'idle' : phase,
      remainingMs: 0
    };
  });

  // 使用 refs 避免闭包陷阱
  const taskRef = useRef(task);

  // 同步最新值到 ref
  useEffect(() => {
    taskRef.current = task;
  }, [task]);

  // 计算剩余时间
  const calculateRemaining = useCallback((currentTask: ScheduledTask, currentStatus: TaskStatus, currentPhase: TaskExecPhase | "idle") => {
    const now = Date.now();

    if (currentStatus === "cancelled" && currentTask.skipUntil && now < currentTask.skipUntil) {
      return Math.max(0, currentTask.skipUntil - now);
    }

    if (currentStatus === "pending") {
      const nextExec = getNextExecuteDate(currentTask);
      if (nextExec) {
        // 仅在启用渐入渐出时才考虑渐入时长
        const fadeInMs = (currentTask.enableFade ? (currentTask.fadeInDuration || 0) : 0) * 1000;
        const audioStartAt = nextExec.getTime() - fadeInMs;
        return Math.max(0, audioStartAt - now);
      }
      return 0;
    }

    if (currentStatus === "executing") {
      // 优先从调度器获取最新信息
      const schedulerRemaining = scheduler.getTaskRemainingMs(currentTask.id);
      if (schedulerRemaining > 0) return schedulerRemaining;

      // 备用计算
      const getTaskStartTimestamp = (t: ScheduledTask) => {
        return new Date(
          t.startTime.year, t.startTime.month - 1, t.startTime.day,
          t.startTime.hour, t.startTime.minute, t.startTime.second
        ).getTime();
      };
      const actualStart = currentTask.lastExecutedAt || getTaskStartTimestamp(currentTask);
      const endTime = actualStart + currentTask.playDurationMinutes * 60 * 1000;
      const fadeInMs = (currentTask.fadeInDuration || 0) * 1000;
      const fadeOutMs = (currentTask.fadeOutDuration || 0) * 1000;

      const schedulerPhase = scheduler.getTaskPhase(currentTask.id);
      if (schedulerPhase === "fading-in" || currentPhase === "fading-in") {
        const fadeInEnd = actualStart + fadeInMs;
        return Math.max(0, fadeInEnd - now);
      }
      if (schedulerPhase === "playing" || currentPhase === "playing") {
        return Math.max(0, endTime - now);
      }
      if (schedulerPhase === "fading-out" || currentPhase === "fading-out") {
        return Math.max(0, endTime + fadeOutMs - now);
      }
      return Math.max(0, endTime - now);
    }

    if (currentStatus === "completed" && currentTask.repeatType !== "once") {
      const nextExec = getNextExecuteDate(currentTask);
      if (nextExec) {
        return Math.max(0, nextExec.getTime() - now);
      }
    }

    return 0;
  }, [scheduler]);

  // 简单的轮询更新 - 不依赖当前状态避免循环
  useEffect(() => {
    const update = () => {
      const currentTask = taskRef.current;
      const currentPhase = scheduler.getTaskPhase(currentTask.id);
      let newStatus: TaskStatus;
      if (currentPhase === 'fading-in' || currentPhase === 'playing' || currentPhase === 'fading-out') {
        newStatus = 'executing';
      } else {
        const latestTask = getAllTasks().find(t => t.id === currentTask.id);
        newStatus = getTaskStatus(latestTask || currentTask);
      }
      const newRemaining = calculateRemaining(currentTask, newStatus, currentPhase);
      setState({
        status: newStatus,
        phase: currentPhase === 'waiting' ? 'waiting' : currentPhase === 'idle' ? 'idle' : currentPhase,
        remainingMs: newRemaining
      });
    };

    update();
    const timer = setInterval(update, 1000); // 每秒更新一次
    return () => clearInterval(timer);
  }, [task.id, scheduler, calculateRemaining]);

  // 调度器事件监听
  useEffect(() => {
    const handleEvent = (event: SchedulerEvent) => {
      if (event.taskId !== task.id) return;

      if (event.type === "tick" || event.type === "phase-change" || event.type === "task-started") {
        const newPhase = event.phase as TaskExecPhase | "idle";
        let newStatus: TaskStatus;
        if (newPhase === "fading-in" || newPhase === "playing" || newPhase === "fading-out") {
          newStatus = "executing";
        } else {
          const latestTask = getAllTasks().find(t => t.id === task.id);
          newStatus = getTaskStatus(latestTask || task);
        }
        setState({
          status: newStatus,
          phase: newPhase,
          remainingMs: event.remainingMs
        });
      } else if (event.type === "task-completed") {
        setState(prev => {
          const latestTask = getAllTasks().find(t => t.id === task.id);
          const newStatus = getTaskStatus(latestTask || task);
          const newRemaining = calculateRemaining(latestTask || task, newStatus, "idle");
          return { status: newStatus, phase: "idle", remainingMs: newRemaining };
        });
      } else if (event.type === "task-cancelled") {
        setState(prev => {
          const latestTask = getAllTasks().find(t => t.id === task.id);
          const newStatus = getTaskStatus(latestTask || task);
          const newRemaining = calculateRemaining(latestTask || task, newStatus, "idle");
          return { status: newStatus, phase: "idle", remainingMs: newRemaining };
        });
      } else if (event.type === "task-resumed") {
        setState(prev => {
          const latestTask = getAllTasks().find(t => t.id === task.id);
          const newStatus = getTaskStatus(latestTask || task);
          const newRemaining = calculateRemaining(latestTask || task, newStatus, "waiting");
          return { status: newStatus, phase: "waiting", remainingMs: newRemaining };
        });
      }
    };
    const unsub = scheduler.on(handleEvent);
    return () => { unsub(); };
  }, [task.id, task, scheduler]);

  return state;
}

interface TaskItemProps {
  task: ScheduledTask;
  index: number;
  isDragging: boolean;
  isSwapAnimating: boolean;
  isMobileDragTarget: boolean;
  onDragStart: (index: number) => void;
  onDragOver: (e: React.DragEvent, index: number) => void;
  onDragEnd: () => void;
  onEdit: (task: ScheduledTask) => void;
  onDelete: (task: ScheduledTask) => void;
  onCancel: (task: ScheduledTask) => void;
  onRefresh: () => void;
  onTouchStart?: (e: React.TouchEvent, index: number) => void;
}

function TaskItem({
  task,
  index,
  isDragging,
  isSwapAnimating,
  isMobileDragTarget,
  onDragStart,
  onDragOver,
  onDragEnd,
  onEdit,
  onDelete,
  onCancel,
  onRefresh,
  onTouchStart,
}: TaskItemProps) {
  const { status, phase, remainingMs } = useTaskState(task);
  const isMobile = useIsMobile();
  const firstAudio = task.audios[0];

  const borderClass = isDragging
    ? "scale-[1.02] opacity-80 z-10"
    : status === "executing" && phase === "fading-in"
      ? ""
      : status === "executing" && phase === "playing"
        ? ""
        : status === "executing" && phase === "fading-out"
          ? ""
          : status === "executing"
            ? ""
            : status === "cancelled"
              ? "opacity-60"
              : "";

  const diffuseClass = isDragging
    ? ""
    : status === "executing" && phase === "fading-in"
      ? "task-diffuse-fadein"
      : status === "executing" && phase === "playing"
        ? "task-diffuse-playing"
        : status === "executing" && phase === "fading-out"
          ? "task-diffuse-fadeout"
          : status === "executing"
            ? "task-diffuse-playing"
            : status === "pending"
              ? "task-diffuse-waiting"
              : "";

  const repeatBadge = (
    <span className={cn(
      "inline-flex items-center px-1.5 sm:px-1.5 py-0.5 rounded text-[11px] sm:text-[10px] font-medium border flex-shrink-0",
      task.repeatType === "once"
        ? "bg-blue-500/10 border-blue-500/20 text-blue-400"
        : task.repeatType === "daily"
          ? "bg-purple-500/10 border-purple-500/20 text-purple-400"
          : task.repeatType === "workday"
            ? "bg-amber-500/10 border-amber-500/20 text-amber-400"
            : "bg-rose-500/10 border-rose-500/20 text-rose-400"
    )}>
      <Repeat className="w-3 h-3 sm:w-2.5 sm:h-2.5 mr-0.5" />
      {REPEAT_TYPE_LABELS[task.repeatType]}
    </span>
  );

  const infoRow = (
    <div className="mt-1.5 sm:mt-1 text-muted-foreground space-y-0.5">
      <div className="flex items-center gap-1.5 sm:gap-2 text-[13px] sm:text-[11px] leading-relaxed">
        <span className="flex items-center gap-0.5 sm:gap-1">
          <Clock className="w-3.5 h-3.5 sm:w-3 sm:h-3" />
          {task.startTime.month}/{task.startTime.day} {String(task.startTime.hour).padStart(2, "0")}:{String(task.startTime.minute).padStart(2, "0")}
        </span>
        <span className="text-muted-foreground/40">·</span>
        <span>{task.playDurationMinutes}分钟</span>
        <span className="text-muted-foreground/40">·</span>
        <span className="flex items-center gap-0.5">
          <Volume2 className="w-3.5 h-3.5 sm:w-3 sm:h-3" />
          {task.volume}%
        </span>
      </div>
      {task.enableFade && (task.fadeInDuration > 0 || task.fadeOutDuration > 0) && (
        <div className="flex items-center gap-1.5 sm:gap-2 text-[12px] sm:text-[11px] text-muted-foreground/70 leading-relaxed">
          {task.fadeInDuration > 0 && (
            <span className="flex items-center gap-0.5">
              <TrendingUp className="w-3 h-3 sm:w-3 sm:h-3" />
              渐入{task.fadeInDuration}秒
            </span>
          )}
          {task.fadeInDuration > 0 && task.fadeOutDuration > 0 && <span className="text-muted-foreground/40">·</span>}
          {task.fadeOutDuration > 0 && (
            <span className="flex items-center gap-0.5">
              <TrendingDown className="w-3 h-3 sm:w-3 sm:h-3" />
              渐出{task.fadeOutDuration}秒
            </span>
          )}
        </div>
      )}
    </div>
  );

  const audioRow = (
    <div className="mt-1.5 sm:mt-2.5 flex items-center gap-2 sm:gap-2 sm:p-0 rounded-none sm:rounded-none bg-transparent sm:bg-transparent sm:pl-0">
      {firstAudio && (
        <div className="flex-1 flex items-center gap-2 min-w-0">
          <Music2 className="w-3.5 h-3.5 sm:w-3 sm:h-3 text-muted-foreground/50 flex-shrink-0" />
          <div className="flex-1 min-w-0">
            <span className="text-[12px] sm:text-[11px] text-muted-foreground truncate leading-relaxed">{firstAudio.name}</span>
            {audioInfoDisplay(firstAudio) && (
              <span className="sm:mt-0 ml-1.5">{audioInfoDisplay(firstAudio)}</span>
            )}
          </div>
        </div>
      )}
      {task.audios.length > 1 && (
        <span className="text-[11px] sm:text-[10px] text-muted-foreground/60 flex-shrink-0 bg-muted/50 sm:bg-muted/50 px-1.5 py-0.5 rounded">
          +{task.audios.length - 1}
        </span>
      )}
    </div>
  );

  const desktopActions = (
    <div className="flex items-center gap-1 flex-shrink-0">
      {status !== "cancelled" && status !== "completed" && status !== "executing" && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            const sched = getTaskScheduler();
            sched.executeNow(task.id);
            onRefresh();
            toast.success(`任务「${task.name}」已开始执行`);
          }}
          className="p-1.5 rounded-md text-muted-foreground hover:text-emerald-400 hover:bg-emerald-500/10 transition-all cursor-pointer"
          title="立即执行"
        >
          <PlayCircle className="w-3.5 h-3.5" />
        </button>
      )}
      {status !== "cancelled" && status !== "completed" && (
        <button
          onClick={(e) => { e.stopPropagation(); onCancel(task); }}
          className="p-1.5 rounded-md text-muted-foreground hover:text-amber-400 hover:bg-amber-500/10 transition-all cursor-pointer"
          title="取消执行"
        >
          <XCircle className="w-3.5 h-3.5" />
        </button>
      )}
      {status === "cancelled" && task.skipUntil && task.repeatType !== "once" && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            const sched = getTaskScheduler();
            sched.resumeTask(task.id);
            toast.success(`任务「${task.name}」已恢复执行`);
          }}
          className="p-1.5 rounded-md text-muted-foreground hover:text-emerald-400 hover:bg-emerald-500/10 transition-all cursor-pointer"
          title="恢复执行"
        >
          <RotateCcw className="w-3.5 h-3.5" />
        </button>
      )}
      <button
        onClick={(e) => { e.stopPropagation(); onEdit(task); }}
        className="p-1.5 rounded-md text-muted-foreground hover:text-[var(--brand-start)] hover:bg-[var(--brand-start)]/10 transition-all cursor-pointer"
        title="编辑任务"
      >
        <Edit3 className="w-3.5 h-3.5" />
      </button>
      <button
        onClick={(e) => { e.stopPropagation(); onDelete(task); }}
        className="p-1.5 rounded-md text-muted-foreground hover:text-red-400 hover:bg-red-500/10 transition-all cursor-pointer"
        title="删除任务"
      >
        <Trash2 className="w-3.5 h-3.5" />
      </button>
    </div>
  );

  if (isMobile) {
    return (
      <div
        onTouchStart={onTouchStart ? (e) => onTouchStart(e, index) : undefined}
        className={cn(
          "transition-all duration-200",
          isSwapAnimating && "transition-all duration-[700ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
          isDragging && "opacity-60 scale-[0.97]",
          isMobileDragTarget && "bg-[var(--brand-start)]/5",
          borderClass,
          diffuseClass
        )}
      >
        <div className="p-3">
          <div className="flex items-center justify-between gap-2 mb-1">
            <div className="flex items-center gap-2 min-w-0">
              <p className="font-medium text-sm text-foreground truncate">{task.name}</p>
              {repeatBadge}
            </div>
            <StatusBadge task={task} status={status} phase={phase} remainingMs={remainingMs} />
          </div>

          <div className="flex items-center gap-1.5 text-[13px] text-muted-foreground leading-relaxed">
            <span className="flex items-center gap-0.5">
              <Clock className="w-3.5 h-3.5" />
              {task.startTime.month}/{task.startTime.day} {String(task.startTime.hour).padStart(2, "0")}:{String(task.startTime.minute).padStart(2, "0")}
            </span>
            <span className="text-muted-foreground/40">·</span>
            <span>{task.playDurationMinutes}分钟</span>
            <span className="text-muted-foreground/40">·</span>
            <span className="flex items-center gap-0.5">
              <Volume2 className="w-3.5 h-3.5" />
              {task.volume}%
            </span>
          </div>

          {task.enableFade && (task.fadeInDuration > 0 || task.fadeOutDuration > 0) && (
            <div className="flex items-center gap-1.5 text-[12px] text-muted-foreground/70 leading-relaxed mt-0.5">
              {task.fadeInDuration > 0 && (
                <span className="flex items-center gap-0.5">
                  <TrendingUp className="w-3 h-3" />
                  渐入{task.fadeInDuration}秒
                </span>
              )}
              {task.fadeInDuration > 0 && task.fadeOutDuration > 0 && <span className="text-muted-foreground/40">·</span>}
              {task.fadeOutDuration > 0 && (
                <span className="flex items-center gap-0.5">
                  <TrendingDown className="w-3 h-3" />
                  渐出{task.fadeOutDuration}秒
                </span>
              )}
            </div>
          )}

          {audioRow}

          <div className="mt-2 pt-2 flex items-center gap-1.5 flex-wrap">
            {status !== "cancelled" && status !== "completed" && status !== "executing" && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  const sched = getTaskScheduler();
                  sched.executeNow(task.id);
                  onRefresh();
                  toast.success(`任务「${task.name}」已开始执行`);
                }}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[13px] font-medium text-emerald-500 bg-emerald-500/10 active:bg-emerald-500/20 transition-all cursor-pointer min-h-[36px]"
              >
                <PlayCircle className="w-4 h-4" />
                执行
              </button>
            )}
            {status !== "cancelled" && status !== "completed" && (
              <button
                onClick={(e) => { e.stopPropagation(); onCancel(task); }}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[13px] font-medium text-amber-500 bg-amber-500/10 active:bg-amber-500/20 transition-all cursor-pointer min-h-[36px]"
              >
                <XCircle className="w-4 h-4" />
                取消
              </button>
            )}
            {status === "cancelled" && task.skipUntil && task.repeatType !== "once" && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  const sched = getTaskScheduler();
                  sched.resumeTask(task.id);
                  toast.success(`任务「${task.name}」已恢复执行`);
                }}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[13px] font-medium text-emerald-500 bg-emerald-500/10 active:bg-emerald-500/20 transition-all cursor-pointer min-h-[36px]"
              >
                <RotateCcw className="w-4 h-4" />
                恢复
              </button>
            )}
            <button
              onClick={(e) => { e.stopPropagation(); onEdit(task); }}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[13px] font-medium text-[var(--brand-start)] bg-[var(--brand-start)]/10 active:bg-[var(--brand-start)]/20 transition-all cursor-pointer min-h-[36px]"
            >
              <Edit3 className="w-4 h-4" />
              编辑
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); onDelete(task); }}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[13px] font-medium text-red-400 bg-red-500/10 active:bg-red-500/20 transition-all cursor-pointer min-h-[36px]"
            >
              <Trash2 className="w-4 h-4" />
              删除
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      draggable
      onDragStart={() => onDragStart(index)}
      onDragOver={(e) => onDragOver(e, index)}
      onDragEnd={onDragEnd}
      className={cn(
        "group/task cursor-pointer transition-all duration-200",
        isSwapAnimating
          ? "transition-all duration-[700ms] ease-[cubic-bezier(0.22,1,0.36,1)]"
          : "transition-all duration-200",
        borderClass,
        diffuseClass
      )}
    >
      <div className="p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2 flex-shrink-0">
            <div
              draggable
              onDragStart={() => onDragStart(index)}
              onDragOver={(e) => onDragOver(e, index)}
              onDragEnd={onDragEnd}
              className="flex-shrink-0 w-5 cursor-grab active:cursor-grabbing touch-none opacity-0 group-hover/task:opacity-100 transition-opacity"
            >
              <GripVertical className="w-4 h-4 text-muted-foreground/30 group-hover/task:text-muted-foreground transition-colors" />
            </div>
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <p className="font-medium text-sm text-foreground truncate">{task.name}</p>
              {repeatBadge}
              {desktopActions}
            </div>
            {infoRow}
          </div>
          <StatusBadge task={task} status={status} phase={phase} remainingMs={remainingMs} />
        </div>
        {audioRow}
      </div>
    </div>
  );
}

export function TaskList({ tasks, onEdit, onCreate, onRefresh }: TaskListProps) {
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [overIndex, setOverIndex] = useState<number | null>(null);
  const [isSwapAnimating, setIsSwapAnimating] = useState(false);
  const dragTaskOrderRef = useRef<string[]>([]);

  // 移动端触摸拖拽状态
  const [mobileDragIndex, setMobileDragIndex] = useState<number | null>(null);
  const [mobileOverIndex, setMobileOverIndex] = useState<number | null>(null);
  const longPressTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const touchStartYRef = useRef<number>(0);
  const isMobileDraggingRef = useRef<boolean>(false);
  const mobileDragIndexRef = useRef<number | null>(null);
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const scheduler = getTaskScheduler();
    const unsub = scheduler.on((event) => {
      if (event.type === "task-started" || event.type === "task-cancelled" || event.type === "task-resumed") {
        onRefresh();
      }
    });
    return () => { unsub(); };
  }, [onRefresh]);

  const handleDelete = useCallback((task: ScheduledTask) => {
    setDeleteConfirmId(task.id);
  }, []);

  const confirmDelete = useCallback(() => {
    if (!deleteConfirmId) return;
    const task = tasks.find((t) => t.id === deleteConfirmId);
    // 先停止调度器中正在执行的播放，再删除数据
    const scheduler = getTaskScheduler();
    scheduler.cancelTask(deleteConfirmId);
    const success = deleteTask(deleteConfirmId);
    if (success) {
      toast.success(`任务「${task?.name || "任务"}」已被删除`);
      onRefresh();
    } else {
      toast.error("删除失败，未找到该任务");
    }
    setDeleteConfirmId(null);
  }, [deleteConfirmId, tasks, onRefresh]);

  const handleCancel = useCallback((task: ScheduledTask) => {
    const scheduler = getTaskScheduler();
    scheduler.cancelTask(task.id);
    toast.info(`任务「${task.name}」已终止执行`);
    onRefresh();
  }, [onRefresh]);

  const deleteTaskData = useMemo(() => {
    if (!deleteConfirmId) return null;
    return tasks.find((t) => t.id === deleteConfirmId);
  }, [deleteConfirmId, tasks]);

  const sortedTasks = useMemo(() => {
    return [...tasks].sort((a, b) => {
      const statusOrder: Record<TaskStatus, number> = {
        executing: 0,
        pending: 1,
        completed: 2,
        cancelled: 3,
      };
      const sa = statusOrder[getTaskStatus(a)] ?? 4;
      const sb = statusOrder[getTaskStatus(b)] ?? 4;
      if (sa !== sb) return sa - sb;
      return b.createdAt - a.createdAt;
    });
  }, [tasks]);

  const handleDragStart = useCallback((index: number) => {
    setDragIndex(index);
    dragTaskOrderRef.current = sortedTasks.map(t => t.id);
  }, [sortedTasks]);

  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (dragIndex === null || dragIndex === index) return;
    setOverIndex(index);
  }, [dragIndex]);

  const handleDragEnd = useCallback(() => {
    if (dragIndex !== null && overIndex !== null && dragIndex !== overIndex) {
      setIsSwapAnimating(true);

      const allTasks = getAllTasks();
      const taskMap = new Map(allTasks.map(t => [t.id, t]));

      const reorderedIds = [...dragTaskOrderRef.current];
      const [draggedId] = reorderedIds.splice(dragIndex, 1);
      reorderedIds.splice(overIndex, 0, draggedId);

      const reorderedTasks = reorderedIds
        .map(id => taskMap.get(id))
        .filter((t): t is ScheduledTask => !!t);

      const otherTasks = allTasks.filter(t => !reorderedIds.includes(t.id));
      saveAllTasks([...otherTasks, ...reorderedTasks]);

      setTimeout(() => {
        setIsSwapAnimating(false);
        onRefresh();
      }, 700);
    }
    setDragIndex(null);
    setOverIndex(null);
  }, [dragIndex, overIndex, onRefresh]);

  // 移动端触摸拖拽：长按触发
  const handleMobileTouchStart = useCallback((e: React.TouchEvent, index: number) => {
    if (isMobileDraggingRef.current) return;

    const touch = e.touches[0];
    touchStartYRef.current = touch.clientY;

    // 清除之前的长按计时器
    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }

    // 启动长按计时器（500ms）
    longPressTimerRef.current = setTimeout(() => {
      isMobileDraggingRef.current = true;
      mobileDragIndexRef.current = index;
      setMobileDragIndex(index);
      setMobileOverIndex(null);
      dragTaskOrderRef.current = sortedTasks.map(t => t.id);

      // 触感反馈
      if (navigator.vibrate) {
        navigator.vibrate(50);
      }

      // 添加全局触摸移动和结束监听
      const handleTouchMove = (moveEvent: TouchEvent) => {
        if (!isMobileDraggingRef.current) return;
        moveEvent.preventDefault(); // 阻止页面滚动

        const moveTouch = moveEvent.touches[0];
        const moveY = moveTouch.clientY;

        // 计算手指位于哪个任务项上
        if (listRef.current) {
          const items = listRef.current.querySelectorAll('[data-task-index]');
          let targetIndex: number | null = null;

          items.forEach((item) => {
            const rect = item.getBoundingClientRect();
            const idx = parseInt(item.getAttribute('data-task-index') || '0', 10);
            if (moveY >= rect.top && moveY <= rect.bottom) {
              targetIndex = idx;
            }
          });

          if (targetIndex !== null && targetIndex !== mobileDragIndexRef.current) {
            setMobileOverIndex(targetIndex);
          }
        }
      };

      const handleTouchEnd = () => {
        document.removeEventListener('touchmove', handleTouchMove);
        document.removeEventListener('touchend', handleTouchEnd);

        const currentDragIndex = mobileDragIndexRef.current;

        // 完成排序
        setMobileOverIndex((currentOverIndex) => {
          if (currentDragIndex !== null && currentOverIndex !== null && currentDragIndex !== currentOverIndex) {
            setIsSwapAnimating(true);

            const allTasks = getAllTasks();
            const taskMap = new Map(allTasks.map(t => [t.id, t]));

            const reorderedIds = [...dragTaskOrderRef.current];
            const [draggedId] = reorderedIds.splice(currentDragIndex, 1);
            reorderedIds.splice(currentOverIndex, 0, draggedId);

            const reorderedTasks = reorderedIds
              .map(id => taskMap.get(id))
              .filter((t): t is ScheduledTask => !!t);

            const otherTasks = allTasks.filter(t => !reorderedIds.includes(t.id));
            saveAllTasks([...otherTasks, ...reorderedTasks]);

            setTimeout(() => {
              setIsSwapAnimating(false);
              onRefresh();
            }, 700);
          }
          return null;
        });

        setMobileDragIndex(null);
        mobileDragIndexRef.current = null;
        isMobileDraggingRef.current = false;
      };

      document.addEventListener('touchmove', handleTouchMove, { passive: false });
      document.addEventListener('touchend', handleTouchEnd, { passive: true });
    }, 500);

    // 监听移动，如果手指移动超过10px则取消长按（视为滚动）
    const handleEarlyTouchMove = (moveEvent: TouchEvent) => {
      const moveTouch = moveEvent.touches[0];
      const deltaY = Math.abs(moveTouch.clientY - touchStartYRef.current);
      if (deltaY > 10) {
        if (longPressTimerRef.current) {
          clearTimeout(longPressTimerRef.current);
          longPressTimerRef.current = null;
        }
        document.removeEventListener('touchmove', handleEarlyTouchMove);
        document.removeEventListener('touchend', handleEarlyTouchEnd);
      }
    };

    const handleEarlyTouchEnd = () => {
      if (longPressTimerRef.current) {
        clearTimeout(longPressTimerRef.current);
        longPressTimerRef.current = null;
      }
      document.removeEventListener('touchmove', handleEarlyTouchMove);
      document.removeEventListener('touchend', handleEarlyTouchEnd);
    };

    document.addEventListener('touchmove', handleEarlyTouchMove, { passive: true });
    document.addEventListener('touchend', handleEarlyTouchEnd, { passive: true });
  }, [sortedTasks, onRefresh]);

  // 清理长按计时器
  useEffect(() => {
    return () => {
      if (longPressTimerRef.current) {
        clearTimeout(longPressTimerRef.current);
      }
    };
  }, []);

  if (tasks.length === 0) {
    return (
      <div className="py-10 sm:py-12 text-center px-4 space-y-4">
        <CalendarClock className="mx-auto size-16 sm:size-20 p-4 rounded-xl border border-[var(--brand-start)]/20 text-[var(--brand-start)]" />
        <p className="text-lg sm:text-xl font-medium tracking-tight">暂无自定义任务</p>
        <p className="text-sm sm:text-base text-muted-foreground/80 leading-relaxed">点击新建你的第一个定时播放任务，让音频按时响起</p>
        <button
          type="button"
          onClick={onCreate}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium text-[var(--brand-start)] border border-[var(--brand-start)]/30 hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/50 active:scale-95 transition-all cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          新建任务
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between px-1">
        <h3 className="text-sm font-medium text-foreground flex items-center gap-2">
          <CalendarClock className="w-4 h-4 text-[var(--brand-start)]" />
          任务列表（{tasks.length}）
          <span className="text-xs font-normal text-muted-foreground hidden sm:inline">
              · 拖拽调整顺序
            </span>
            <span className="text-xs font-normal text-muted-foreground sm:hidden">
              · 长按拖动
            </span>
        </h3>
      </div>

      <div className="space-y-2" ref={listRef}>
        {sortedTasks.map((task, index) => {
          const isDragging = dragIndex === index || mobileDragIndex === index;
          const isMobileDragTarget = mobileOverIndex === index;

          return (
            <div key={task.id} data-task-index={index}>
              {/* 移动端拖拽占位符：在目标位置上方显示 */}
              {isMobileDragTarget && mobileDragIndex !== null && mobileDragIndex !== index && (
                <div className="h-1 rounded-full bg-[var(--brand-start)]/30 mb-2 transition-all duration-200" />
              )}
              <TaskItem
                task={task}
                index={index}
                isDragging={isDragging}
                isSwapAnimating={isSwapAnimating}
                isMobileDragTarget={isMobileDragTarget && mobileDragIndex !== null && mobileDragIndex !== index}
                onDragStart={handleDragStart}
                onDragOver={handleDragOver}
                onDragEnd={handleDragEnd}
                onEdit={onEdit}
                onDelete={handleDelete}
                onCancel={handleCancel}
                onRefresh={onRefresh}
                onTouchStart={handleMobileTouchStart}
              />
            </div>
          );
        })}
      </div>

      {/* 新建任务按钮 */}
      {onCreate && (
        <div className="pt-4 pb-2 text-center">
          <button
            type="button"
            onClick={onCreate}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium text-[var(--brand-start)] border border-[var(--brand-start)]/30 hover:bg-[var(--brand-start)]/10 hover:border-[var(--brand-start)]/50 active:scale-95 transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            新建任务
          </button>
        </div>
      )}

      <FeedbackModal
        visible={!!deleteConfirmId}
        type="warning"
        title="确认删除任务"
        message={`即将删除任务「${deleteTaskData?.name || ""}」及其所有关联数据，此操作不可撤销`}
        confirmText="确认删除"
        cancelText="取消"
        onConfirm={confirmDelete}
        onCancel={() => setDeleteConfirmId(null)}
        showCancel
      />
    </div>
  );
}
