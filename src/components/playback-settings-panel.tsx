"use client";

import { useEffect, useMemo, useState, type ReactNode } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { AlertCircle, Timer } from "lucide-react";
import { AudioUpload } from "@/components/audio-upload";
import { FadeControls } from "@/components/fade-controls";
import { ModeSwitch } from "@/components/mode-switch";
import { toast } from "@/components/sonner";
import { WheelDateTimePicker, type DateTimeValue } from "@/components/wheel-date-time-picker";
import { cn } from "@/lib/utils";
import type { PlaybackController } from "@/hooks/use-playback-controller";
import type { PlayMode, TaskAudio } from "@/lib/task-types";

interface PlaybackSettingsPanelProps {
  controller: PlaybackController;
  mode: PlayMode;
  onModeChange: (mode: PlayMode) => void;
  importFileKey?: string;
  onAudioUploaded?: (audios: TaskAudio[]) => void;
  onAudioRemoved?: (audioId: string) => void;
  children?: ReactNode;
}

function currentDateTime(): DateTimeValue {
  const now = new Date();
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    day: now.getDate(),
    hour: now.getHours(),
    minute: now.getMinutes(),
    second: now.getSeconds(),
  };
}

function startTimeError(
  time: DateTimeValue,
  fadeInDuration: number,
  enableFade: boolean,
  nowMs: number,
): string | null {
  if (!time?.year) return "请设置开始时间";
  const targetMs = new Date(time.year, time.month - 1, time.day, time.hour, time.minute, time.second).getTime();
  const actualStartMs = targetMs - (enableFade ? fadeInDuration * 1000 : 0);
  if (actualStartMs >= nowMs - 2000) return null;
  return enableFade && fadeInDuration > 0
    ? "距离开始时间不足以完成渐入，请调整开始时间或缩短渐入时长"
    : "开始时间不能早于当前时间";
}

function endTimeError(time: DateTimeValue, startTime: DateTimeValue, nowMs: number): string | null {
  if (!time?.year) return "请设置结束时间";
  const targetMs = new Date(time.year, time.month - 1, time.day, time.hour, time.minute, time.second).getTime();
  const startMs = new Date(
    startTime.year,
    startTime.month - 1,
    startTime.day,
    startTime.hour,
    startTime.minute,
    startTime.second,
  ).getTime();
  if (targetMs < nowMs - 2000) return "结束时间不能早于当前时间";
  if (targetMs === startMs) return "结束时间不能与开始时间相同，请设置不同的时间";
  if (targetMs < startMs) return "结束时间不能早于开始时间";
  return null;
}

function InlineError({ children }: { children: ReactNode }) {
  return (
    <div className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border border-red-500/20 bg-red-500/[0.05] dark:bg-red-500/[0.08] backdrop-blur-sm text-xs text-red-500 dark:text-red-400 animate-in fade-in slide-in-from-top-1 duration-200">
      <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
      <span className="leading-relaxed">{children}</span>
    </div>
  );
}

export function PlaybackSettingsPanel({
  controller,
  mode,
  onModeChange,
  importFileKey,
  onAudioUploaded,
  onAudioRemoved,
  children,
}: PlaybackSettingsPanelProps) {
  const [startTime, setStartTime] = useState<DateTimeValue>(currentDateTime);
  const [endTime, setEndTime] = useState<DateTimeValue>(currentDateTime);
  const importByFileKey = controller.assets.importByFileKey;
  const isAuthenticated = controller.assets.isAuthenticated;

  useEffect(() => {
    if (!importFileKey || !isAuthenticated) return;
    void importByFileKey(importFileKey).then(audio => {
      if (audio) onAudioUploaded?.([audio]);
    });
  }, [importByFileKey, importFileKey, isAuthenticated, onAudioUploaded]);

  return (
    <div className="space-y-3 sm:space-y-6">
      <div className="flex flex-col items-center gap-2.5 pb-4 sm:pb-0">
        <ModeSwitch mode={mode} onModeChange={onModeChange} />
        <p className="px-4 text-center text-xs leading-relaxed text-muted-foreground">
          {mode === "default" ? "设置开始和结束时间，快速启动播放" : "创建定时任务，支持重复执行和任务管理"}
        </p>
      </div>

      {mode === "default" ? (
        <DefaultPlaybackContent
          controller={controller}
          startTime={startTime}
          endTime={endTime}
          onStartTimeChange={setStartTime}
          onEndTimeChange={setEndTime}
          onAudioUploaded={onAudioUploaded}
          onAudioRemoved={onAudioRemoved}
        />
      ) : children}
    </div>
  );
}

interface DefaultPlaybackContentProps {
  controller: PlaybackController;
  startTime: DateTimeValue;
  endTime: DateTimeValue;
  onStartTimeChange: (value: DateTimeValue) => void;
  onEndTimeChange: (value: DateTimeValue) => void;
  onAudioUploaded?: (audios: TaskAudio[]) => void;
  onAudioRemoved?: (audioId: string) => void;
}

function DefaultPlaybackContent({
  controller,
  startTime,
  endTime,
  onStartTimeChange,
  onEndTimeChange,
  onAudioUploaded,
  onAudioRemoved,
}: DefaultPlaybackContentProps) {
  const router = useRouter();
  const [nowMs, setNowMs] = useState(() => Date.now());
  const { audios, fadeInDuration, fadeOutDuration, enableFade } = controller.draft;

  useEffect(() => {
    const timer = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const currentStartTimeError = useMemo(
    () => startTimeError(startTime, fadeInDuration, enableFade, nowMs),
    [enableFade, fadeInDuration, nowMs, startTime],
  );
  const currentEndTimeError = useMemo(
    () => endTimeError(endTime, startTime, nowMs),
    [endTime, nowMs, startTime],
  );
  const isStartTimeValid = currentStartTimeError === null;
  const isEndTimeValid = currentEndTimeError === null;

  const handleDreamPillow = async () => {
    const freshNow = Date.now();
    const freshStartError = startTimeError(startTime, fadeInDuration, enableFade, freshNow);
    const freshEndError = endTimeError(endTime, startTime, freshNow);
    if (freshStartError) {
      toast.error(freshStartError);
      return;
    }
    if (freshEndError) {
      toast.error(freshEndError);
      return;
    }

    const blocker = controller.assets.commitBlocker;
    if (blocker === "no-audio") {
      toast.error("请先上传音频");
      return;
    }
    if (blocker === "unavailable-audio") {
      toast.error("有音频尚未保存完成，请重新上传后再试");
      return;
    }
    if (blocker === "upload-failed") {
      toast.error("有音频上传失败，请移除后重试");
      return;
    }

    if (blocker === "uploading") {
      const uploadingCount = Object.values(controller.assets.uploads).filter(upload => upload.uploading).length;
      toast.loading(`等待 ${uploadingCount} 个音频上传完成...`, { id: "dream-upload" });
      const failedUploadCount = await controller.assets.waitForUploads();
      toast.dismiss("dream-upload");
      if (failedUploadCount > 0) {
        toast.error(`有 ${failedUploadCount} 个音频上传失败，请移除后重试`);
        return;
      }
    }

    const latestDraft = controller.getDraft();
    if (
      controller.assets.isAuthenticated
      && latestDraft.audios.some(audio => !audio.fileKey && !audio.serverUrl)
    ) {
      toast.error("有音频尚未上传完成，请重试后再继续");
      return;
    }

    localStorage.setItem("dream_config", JSON.stringify({
      audios: latestDraft.audios.map(audio => ({
        id: audio.id,
        name: audio.name,
        url: audio.serverUrl || (audio.fileKey ? `/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}` : undefined),
        fileKey: audio.fileKey,
        serverUrl: audio.serverUrl,
        dbKey: audio.dbKey,
        duration: audio.duration,
        size: audio.size,
      })),
      volume: latestDraft.volume,
      fadeInDuration: latestDraft.fadeInDuration,
      fadeOutDuration: latestDraft.fadeOutDuration,
      enableFade: latestDraft.enableFade,
      startTime,
      endTime,
      createdAt: Date.now(),
    }));
    router.push("/templates");
  };

  const startMs = new Date(
    startTime.year,
    startTime.month - 1,
    startTime.day,
    startTime.hour,
    startTime.minute,
    startTime.second,
  ).getTime();
  const endMs = new Date(
    endTime.year,
    endTime.month - 1,
    endTime.day,
    endTime.hour,
    endTime.minute,
    endTime.second,
  ).getTime();
  const durationMinutes = Math.round((endMs - startMs) / 60000);

  return (
    <>
      <AudioUpload
        controller={controller}
        onAudioUploaded={onAudioUploaded}
        onAudioRemoved={onAudioRemoved}
      />

      <div>
        <div className="p-3 sm:p-5 space-y-3 sm:space-y-4">
          <div className="flex items-center justify-between">
            <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
              <Timer className="w-3.5 h-3.5" />播放时段
            </label>
            {durationMinutes > 0 && (
              <span className="text-xs font-mono text-[var(--brand-start)] tabular-nums">
                共 {Math.floor(durationMinutes / 60) > 0 ? `${Math.floor(durationMinutes / 60)}小时` : ""}
                {durationMinutes % 60 > 0 ? `${durationMinutes % 60}分钟` : (durationMinutes >= 60 ? "" : "0分钟")}
              </span>
            )}
          </div>

          <div className="space-y-2 sm:space-y-3">
            <WheelDateTimePicker label="开始时间" value={startTime} onChange={onStartTimeChange} />
            <WheelDateTimePicker label="结束时间" value={endTime} onChange={onEndTimeChange} />
          </div>

          {currentStartTimeError && <InlineError>{currentStartTimeError}</InlineError>}
          {currentEndTimeError && <InlineError>{currentEndTimeError}</InlineError>}

          <FadeControls
            className="pt-3"
            enabled={enableFade}
            fadeInDuration={fadeInDuration}
            fadeOutDuration={fadeOutDuration}
            onEnabledChange={enabled => controller.setFade({ enableFade: enabled })}
            onFadeInDurationChange={duration => controller.setFade({ fadeInDuration: duration })}
            onFadeOutDurationChange={duration => controller.setFade({ fadeOutDuration: duration })}
            showHint
          />
        </div>
      </div>

      <button
        onClick={() => void handleDreamPillow()}
        disabled={audios.length === 0 || !isStartTimeValid || !isEndTimeValid}
        className={cn(
          "w-full mt-4 relative overflow-hidden px-6 py-4 rounded-xl font-bold text-lg transition-all duration-300 transform",
          audios.length === 0 || !isStartTimeValid || !isEndTimeValid
            ? "text-white/50 cursor-not-allowed opacity-60"
            : "text-[#050510] hover:-translate-y-1 fill-btn",
        )}
        style={{
          background: audios.length === 0 || !isStartTimeValid || !isEndTimeValid
            ? "linear-gradient(135deg, #666666 0%, #555555 50%, #666666 100%)"
            : "linear-gradient(135deg, #00d4aa 0%, #00b894 50%, #00d4aa 100%)",
          boxShadow: audios.length === 0 || !isStartTimeValid || !isEndTimeValid
            ? "0 2px 8px rgba(0, 0, 0, 0.2)"
            : "0 4px 15px rgba(0, 212, 170, 0.3)",
        }}
      >
        <span className="relative flex items-center justify-center gap-2">
          <Image
            src="/logo.png"
            alt="梦枕"
            width={24}
            height={24}
            className={cn(
              "rounded shadow-[inset_0_1px_3px_rgba(0,0,0,0.3)]",
              (audios.length === 0 || !isStartTimeValid) && "opacity-50",
            )}
          />
          <span>
            {!isStartTimeValid
              ? "开始时间无效"
              : !isEndTimeValid
                ? "结束时间无效"
                : audios.length === 0
                  ? "请先上传音频"
                  : "一键梦枕"}
          </span>
        </span>
      </button>
    </>
  );
}
