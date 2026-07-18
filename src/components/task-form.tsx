"use client";

import React, { useState, useCallback, useEffect, useMemo } from "react";
import {
  ScheduledTask,
  TaskRepeatType,
} from "@/lib/task-types";
import { createTask, updateTask } from "@/lib/task-store";
import { clonePlaybackDraft, playbackDraftFromTask } from "@/lib/playback-draft";
import { usePlaybackController, type CommitBlocker, type PlaybackController } from "@/hooks/use-playback-controller";
import { WheelDateTimePicker, type DateTimeValue } from "@/components/wheel-date-time-picker";
import { DurationSetter } from "@/components/duration-setter";
import { AudioUpload } from "@/components/audio-upload";
import { FadeControls } from "@/components/fade-controls";
import { toast } from "@/components/sonner";
import { cn } from "@/lib/utils";
import {
  AlertCircle,
  Save,
  Repeat,
} from "lucide-react";

interface TaskFormProps {
  editTask?: ScheduledTask | null;
  sharedPlaybackController: PlaybackController;
  active?: boolean;
  onSave?: (task: ScheduledTask) => void;
  onCancel?: () => void;
}

const REPEAT_OPTIONS: { value: TaskRepeatType; label: string; desc: string }[] = [
  { value: "once", label: "一次性", desc: "仅执行一次" },
  { value: "daily", label: "每天", desc: "每天重复执行" },
  { value: "workday", label: "法定工作日", desc: "周一至周五（含节假日调休）" },
  { value: "holiday", label: "法定节假日", desc: "国家法定节假日" },
];

const COMMIT_BLOCKER_MESSAGES: Record<Exclude<CommitBlocker, null>, string> = {
  "no-audio": "请先上传音频",
  uploading: "音频正在上传，请稍候",
  "upload-failed": "有音频上传失败，请移除或重试",
  "unavailable-audio": "有音频尚未保存完成，请重新上传后再保存任务",
};

const COMMIT_BLOCKER_LABELS: Record<Exclude<CommitBlocker, null>, string> = {
  "no-audio": "请先上传音频",
  uploading: "音频上传中",
  "upload-failed": "音频上传失败",
  "unavailable-audio": "音频尚未保存完成",
};

export function TaskForm({
  editTask,
  sharedPlaybackController,
  active = true,
  onSave,
  onCancel,
}: TaskFormProps) {
  if (editTask) {
    return (
      <EditTaskForm
        key={editTask.id}
        editTask={editTask}
        active={active}
        onSave={onSave}
        onCancel={onCancel}
      />
    );
  }

  return (
    <TaskFormContent
      controller={sharedPlaybackController}
      active={active}
      onSave={onSave}
      onCancel={onCancel}
    />
  );
}

interface EditTaskFormProps {
  editTask: ScheduledTask;
  active: boolean;
  onSave?: (task: ScheduledTask) => void;
  onCancel?: () => void;
}

function EditTaskForm({ editTask, active, onSave, onCancel }: EditTaskFormProps) {
  const [editPlaybackDraft, setEditPlaybackDraft] = useState(() => playbackDraftFromTask(editTask));
  const editPlaybackController = usePlaybackController({
    value: editPlaybackDraft,
    onChange: setEditPlaybackDraft,
  });

  return (
    <TaskFormContent
      editTask={editTask}
      controller={editPlaybackController}
      active={active}
      onSave={onSave}
      onCancel={onCancel}
    />
  );
}

interface TaskFormContentProps {
  editTask?: ScheduledTask | null;
  controller: PlaybackController;
  active: boolean;
  onSave?: (task: ScheduledTask) => void;
  onCancel?: () => void;
}

function TaskFormContent({ editTask, controller, active, onSave, onCancel }: TaskFormContentProps) {
  const isEditing = !!editTask;
  const playbackDraft = controller.draft;

  const [taskName, setTaskName] = useState(editTask?.name || "");
  const [startTime, setStartTime] = useState<DateTimeValue>(() => {
    if (editTask?.startTime) {
      return {
        year: editTask.startTime.year,
        month: editTask.startTime.month,
        day: editTask.startTime.day,
        hour: editTask.startTime.hour,
        minute: editTask.startTime.minute,
        second: editTask.startTime.second,
      };
    }
    const now = new Date();
    return {
      year: now.getFullYear(),
      month: now.getMonth() + 1,
      day: now.getDate(),
      hour: now.getHours(),
      minute: now.getMinutes(),
      second: now.getSeconds(),
    };
  });
  const [playDurationMinutes, setPlayDurationMinutes] = useState(
    Math.max(1, editTask?.playDurationMinutes ?? 30)
  );
  const [repeatType, setRepeatType] = useState<TaskRepeatType>(editTask?.repeatType || "once");
  const [nowMs, setNowMs] = useState(() => Date.now());

  const { fadeInDuration, fadeOutDuration, enableFade } = playbackDraft;
  const { commitBlocker } = controller.assets;
  const stopPreview = controller.preview.stop;

  useEffect(() => {
    const timer = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!active) stopPreview();
  }, [active, stopPreview]);

  const timeError = useMemo<string | null>(() => {
    if (repeatType !== "once") {
      return null;
    }
    const target = new Date(
      startTime.year,
      startTime.month - 1,
      startTime.day,
      startTime.hour,
      startTime.minute,
      startTime.second
    ).getTime();

    // 仅在启用渐入渐出时才考虑渐入时长
    const fadeInMs = enableFade ? fadeInDuration * 1000 : 0;
    const audioStartAt = target - fadeInMs;

    if (audioStartAt < nowMs - 2000) {
      if (enableFade && fadeInDuration > 0) {
        return "开始时间不足以完成渐入效果，请选择稍后的时间点";
      }
      return "开始时间不能早于当前时间";
    }
    return null;
  }, [startTime, repeatType, fadeInDuration, enableFade, nowMs]);

  const validateTime = useCallback((): { valid: boolean; error: string | null } => {
    if (repeatType !== "once") {
      return { valid: true, error: null };
    }
    const now = Date.now();
    const target = new Date(
      startTime.year,
      startTime.month - 1,
      startTime.day,
      startTime.hour,
      startTime.minute,
      startTime.second
    ).getTime();

    // 仅在启用渐入渐出时才考虑渐入时长
    const fadeInMs = enableFade ? fadeInDuration * 1000 : 0;
    const audioStartAt = target - fadeInMs;

    if (audioStartAt < now - 2000) {
      if (enableFade && fadeInDuration > 0) {
        return { valid: false, error: "开始时间不足以完成渐入效果，请选择稍后的时间点" };
      }
      return { valid: false, error: "开始时间不能早于当前时间" };
    }
    return { valid: true, error: null };
  }, [startTime, repeatType, fadeInDuration, enableFade]);

  const handleSave = useCallback(() => {
    const playbackSnapshot = clonePlaybackDraft(controller.getDraft());

    if (!taskName.trim()) {
      toast.error("请输入任务名称");
      return;
    }

    if (commitBlocker) {
      toast.error(COMMIT_BLOCKER_MESSAGES[commitBlocker]);
      return;
    }

    if (playDurationMinutes <= 0) {
      toast.error("播放时长不能为0");
      return;
    }

    const timeResult = validateTime();
    if (!timeResult.valid) {
      toast.error(timeResult.error || "时间设置无效");
      return;
    }

    const taskData = {
      name: taskName.trim(),
      startTime: {
        year: startTime.year,
        month: startTime.month,
        day: startTime.day,
        hour: startTime.hour,
        minute: startTime.minute,
        second: startTime.second,
      },
      playDurationMinutes,
      fadeInDuration: playbackSnapshot.fadeInDuration,
      fadeOutDuration: playbackSnapshot.fadeOutDuration,
      enableFade: playbackSnapshot.enableFade,
      volume: playbackSnapshot.volume,
      repeatType,
      audios: playbackSnapshot.audios,
    };

    try {
      let savedTask: ScheduledTask;
      if (isEditing && editTask) {
        const updateData = {
          ...taskData,
          status: 'pending' as const,
          skipUntil: undefined,
          completedAt: undefined,
        };
        savedTask = updateTask(editTask.id, updateData) || editTask;
        toast.success(`任务「${taskName}」已成功更新`);
      } else {
        savedTask = createTask(taskData);
        toast.success(`任务「${taskName}」已成功创建`);
      }
      onSave?.(savedTask);
    } catch (err) {
      console.error("[TaskForm] Save failed:", err);
      toast.error(err instanceof Error ? err.message : "保存失败");
    }
  }, [
    taskName, validateTime, startTime,
    playDurationMinutes,
    repeatType, isEditing, editTask, onSave,
    controller, commitBlocker,
  ]);

  const isSaveDisabled = !taskName.trim() || commitBlocker !== null || !!timeError || playDurationMinutes <= 0;

  return (
    <div className="space-y-5 sm:space-y-6">
      <AudioUpload
        controller={controller}
      />

      <div className="space-y-4">
        <div className="space-y-1.5">
          <label className="text-xs font-medium text-muted-foreground">任务名称</label>
          <input
            type="text"
            value={taskName}
            onChange={(e) => setTaskName(e.target.value)}
            placeholder="输入任务名称"
            maxLength={50}
            className="w-full h-11 sm:h-10 px-3 rounded-lg border border-border/60 bg-transparent text-sm text-foreground placeholder:text-muted-foreground/50 focus:outline-none focus:border-[var(--brand-start)]/60 focus:ring-1 focus:ring-[var(--brand-start)]/30 transition-all"
          />
        </div>

        <div className="space-y-1.5">
          <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
            <Repeat className="w-3.5 h-3.5" />
            重复类型
          </label>
          <div className="grid grid-cols-2 gap-2">
            {REPEAT_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setRepeatType(opt.value)}
                className={cn(
                  "p-3 sm:p-3 text-left transition-all cursor-pointer min-h-[52px] sm:min-h-0",
                  repeatType === opt.value
                    ? "text-[var(--brand-start)]"
                    : ""
                )}
              >
                <div className={cn(
                  "text-sm font-medium",
                  repeatType === opt.value ? "text-[var(--brand-start)]" : "text-foreground"
                )}>
                  {opt.label}
                </div>
                <div className="text-[10px] text-muted-foreground mt-0.5">{opt.desc}</div>
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-2 sm:space-y-3">
          <WheelDateTimePicker
            label="开始时间"
            value={startTime}
            onChange={setStartTime}
            collapseOnMobile
          />

          <DurationSetter
            value={playDurationMinutes}
            onChange={setPlayDurationMinutes}
            min={1}
          />
        </div>

        {timeError && (
          <div className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border border-red-500/20 bg-red-500/[0.05] dark:bg-red-500/[0.08] backdrop-blur-sm text-xs text-red-500 dark:text-red-400 animate-in fade-in slide-in-from-top-1 duration-200">
            <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
            <span className="leading-relaxed">{timeError}</span>
          </div>
        )}

        <FadeControls
          className="pt-2"
          enabled={enableFade}
          fadeInDuration={fadeInDuration}
          fadeOutDuration={fadeOutDuration}
          onEnabledChange={enabled => controller.setFade({ enableFade: enabled })}
          onFadeInDurationChange={duration => controller.setFade({ fadeInDuration: duration })}
          onFadeOutDurationChange={duration => controller.setFade({ fadeOutDuration: duration })}
        />
      </div>

      <div className="flex items-center gap-3 pt-2">
        <button
          onClick={onCancel}
          className="flex-1 h-12 rounded-xl border border-border/60 text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/80 active:bg-muted transition-all cursor-pointer min-h-[44px]"
        >
          {isEditing ? "取消修改" : "暂不创建"}
        </button>
        <button
          onClick={handleSave}
          disabled={isSaveDisabled}
          className={cn(
            "flex-1 h-12 rounded-xl text-sm font-bold transition-all flex items-center justify-center gap-2 min-h-[44px]",
            isSaveDisabled
              ? "bg-muted text-muted-foreground/50 cursor-default shadow-none"
              : "bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] text-white shadow-md shadow-[var(--brand-start)]/20 hover:opacity-90 active:opacity-80 cursor-pointer"
          )}
        >
          <Save className="w-4 h-4" />
          {isSaveDisabled ? (
            !taskName.trim() ? "请输入任务名称" :
            commitBlocker ? COMMIT_BLOCKER_LABELS[commitBlocker] :
            timeError || "时间无效"
          ) : "保存任务"}
        </button>
      </div>
    </div>
  );
}
