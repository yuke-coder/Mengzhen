"use client";

import React, { useState, useCallback, useRef, useEffect, type Dispatch, type SetStateAction } from "react";
import Image from "next/image";
import { useAuth } from "@/lib/auth-context";
import { getAudioBlob, saveAudioBlob } from "@/lib/audio/db";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { WheelDateTimePicker, type DateTimeValue } from "@/components/wheel-date-time-picker";
import { FadeControls } from "@/components/fade-controls";

import { useRouter } from "next/navigation";
import { toast } from "@/components/sonner";
import { ModeSwitch } from "@/components/mode-switch";
import { PlayMode, type PlaybackDraft, type TaskAudio } from "@/lib/task-types";
import { AUDIO_ACCEPT, AUDIO_EXTENSIONS, MAX_FILES, formatFileSize, formatDuration } from "@/lib/audio";
import {
  Upload,
  Music2,
  X,
  Play,
  Pause,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Volume2,
  Clock,
  GripVertical,
  Volume1,
  VolumeX,
  Timer,
} from "lucide-react";

// 拖拽功能的样式
const dragStyles = `
  .audio-list-container [data-audio-item] {
    touch-action: pan-y;
    -webkit-user-select: none;
    user-select: none;
  }

  .audio-list-container [data-audio-item].dragging-item {
    touch-action: none;
    -webkit-user-select: none;
    user-select: none;
    cursor: grabbing !important;
  }

  @keyframes dragFeedback {
    0% { transform: scale(1); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }
    50% { transform: scale(1.02); box-shadow: 0 12px 28px rgba(0, 0, 0, 0.15); }
    100% { transform: scale(1.02); box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2); }
  }

  .audio-list-container [data-audio-item]:active:not(.dragging-item) {
    animation: dragFeedback 0.15s ease-out;
  }
`;

interface UploadState {
  uploading: boolean;
  uploadProgress: number;
  uploadError: string | null;
}

interface AudioUploadProps {
  playbackDraft: PlaybackDraft;
  onPlaybackDraftChange: Dispatch<SetStateAction<PlaybackDraft>>;
  onAudioUploaded?: (audios: TaskAudio[]) => void;
  onAudioRemoved?: (id: string) => void;
  onOrderChange?: (orderedIds: string[]) => void;
  onAudioCountChange?: (count: number) => void;
  disabled?: boolean;
  importFileKey?: string;
  mode?: PlayMode;
  onModeChange?: (mode: PlayMode) => void;
  children?: React.ReactNode;
}

let globalIdCounter = 0;

function useClientOnly() {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  return mounted;
}

export function AudioUpload({
  playbackDraft,
  onPlaybackDraftChange,
  onAudioUploaded,
  onAudioRemoved,
  onOrderChange,
  onAudioCountChange,
  disabled = false,
  importFileKey,
  mode,
  onModeChange,
  children,
}: AudioUploadProps) {
  const { user } = useAuth();
  const router = useRouter();
  const mounted = useClientOnly();

  const { audios, volume, fadeInDuration, fadeOutDuration, enableFade } = playbackDraft;

  const setAudios = useCallback((update: SetStateAction<TaskAudio[]>) => {
    onPlaybackDraftChange(previous => ({
      ...previous,
      audios: typeof update === "function" ? update(previous.audios) : update,
    }));
  }, [onPlaybackDraftChange]);

  const setVolume = useCallback((nextVolume: number) => {
    onPlaybackDraftChange(previous => ({ ...previous, volume: nextVolume }));
  }, [onPlaybackDraftChange]);

  const setFadeInDuration = useCallback((duration: number) => {
    onPlaybackDraftChange(previous => ({ ...previous, fadeInDuration: duration }));
  }, [onPlaybackDraftChange]);

  const setFadeOutDuration = useCallback((duration: number) => {
    onPlaybackDraftChange(previous => ({ ...previous, fadeOutDuration: duration }));
  }, [onPlaybackDraftChange]);

  const setEnableFade = useCallback((enabled: boolean) => {
    onPlaybackDraftChange(previous => ({ ...previous, enableFade: enabled }));
  }, [onPlaybackDraftChange]);

  useEffect(() => {
    isMountedRef.current = mounted;
  }, [mounted]);

  const [playingId, setPlayingId] = useState<string | null>(null);
  const [currentTimes, setCurrentTimes] = useState<Record<string, number>>({});
  const [dragOver, setDragOver] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [showGuestTip, setShowGuestTip] = useState(false);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [portalReady, setPortalReady] = useState(false);
  const audioRefs = useRef<Record<string, HTMLAudioElement>>({});
  const isMountedRef = useRef(false);
  const fileRefs = useRef<Record<string, File>>({});
  const objectUrlRefs = useRef<Record<string, string>>({});
  const [localUrls, setLocalUrls] = useState<Record<string, string>>({});
  const uploadStatesRef = useRef<Record<string, UploadState>>({});
  const [uploadStates, setUploadStates] = useState<Record<string, UploadState>>({});

  // 仅完整模式需要的状态
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [isSwapAnimating, setIsSwapAnimating] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [draggingY, setDraggingY] = useState(0);
  const [dragOffsetY, setDragOffsetY] = useState(0);
  const [dragStartIndex, setDragStartIndex] = useState<number | null>(null);
  const longPressTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const itemRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const lastMoveYRef = useRef(0);
  const lastMoveTimeRef = useRef(0);

  // 完整模式专用的时间和渐变相关状态
  const [startTime, setStartTime] = useState<DateTimeValue>(() => {
    const now = new Date();
    return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate(), hour: now.getHours(), minute: now.getMinutes(), second: now.getSeconds() };
  });
  const [endTime, setEndTime] = useState<DateTimeValue>(() => {
    const now = new Date();
    return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate(), hour: now.getHours(), minute: now.getMinutes(), second: now.getSeconds() };
  });
  const [isStartTimeValid, setIsStartTimeValid] = useState(true);
  const [startTimeError, setStartTimeError] = useState<string | null>(null);
  const [isEndTimeValid, setIsEndTimeValid] = useState(true);
  const [endTimeError, setEndTimeError] = useState<string | null>(null);

  const VolumeIcon = volume === 0 ? VolumeX : volume < 50 ? Volume1 : Volume2;

  // 在用户交互时激活音频上下文
  useEffect(() => {
    if (!mounted) return;
    const resumeAudioOnInteraction = async () => {
      try {
        const { getTaskScheduler } = await import("@/lib/task-scheduler");
        const scheduler = getTaskScheduler();
        if (scheduler && scheduler.resumeAudioContext) {
          scheduler.resumeAudioContext();
        }
      } catch (e) {
        console.warn("AudioContext resume failed:", e);
      }
    };

    const events = ["click", "touchstart", "mousedown", "keydown"];
    events.forEach(event => {
      document.addEventListener(event, resumeAudioOnInteraction, { once: true });
    });

    return () => {
      events.forEach(event => {
        document.removeEventListener(event, resumeAudioOnInteraction);
      });
    };
  }, [mounted]);

  // 处理从我的音频导入音频
  useEffect(() => {
    if (!mounted || !importFileKey || !user) return;

    async function importAudio() {
      try {
        const res = await fetch(`/api/audio/get-by-key?fileKey=${importFileKey}`);
        const data = await res.json();
        if (!data.success || !data.audio) {
          console.error("导入音频失败:", data.error);
          return;
        }

        const audioInfo = data.audio;
        const id = `imported-${Date.now()}-${Math.random().toString(36).slice(2)}`;

        const newAudio: TaskAudio = {
          id,
          name: audioInfo.name,
          duration: audioInfo.metadata?.duration || 0,
          size: audioInfo.size || 0,
          fileKey: importFileKey,
          serverUrl: audioInfo.serverUrl,
        };

        if (audios.some(audio => audio.fileKey === newAudio.fileKey)) return;
        const updated = [...audios, newAudio];
        setAudios(updated);
        onAudioUploaded?.(updated);
        onAudioCountChange?.(updated.length);
      } catch (err) {
        console.error("导入音频异常:", err);
      }
    }

    importAudio();
  }, [audios, importFileKey, mounted, onAudioCountChange, onAudioUploaded, setAudios, user]);

  useEffect(() => setPortalReady(true), []);

  // 本地音频只恢复预览 URL；业务配置始终由父组件提供。
  useEffect(() => {
    if (!mounted) return;
    let cancelled = false;

    for (const audio of audios) {
      if (!audio.dbKey || audio.serverUrl || audio.fileKey || objectUrlRefs.current[audio.id]) continue;
      void getAudioBlob(audio.dbKey).then(blob => {
        if (!blob || cancelled || objectUrlRefs.current[audio.id]) return;
        const url = URL.createObjectURL(blob);
        objectUrlRefs.current[audio.id] = url;
        setLocalUrls(previous => ({ ...previous, [audio.id]: url }));
      }).catch(() => {});
    }

    return () => { cancelled = true; };
  }, [mounted, audios]);

  useEffect(() => () => {
    Object.values(audioRefs.current).forEach(audio => audio.pause());
    Object.values(objectUrlRefs.current).forEach(url => URL.revokeObjectURL(url));
  }, []);

  // 纯验证函数（返回布尔值，不设状态）- 用于 handleDreamPillow
  const isStartTimeValidFn = useCallback((time: DateTimeValue, fadeInSec?: number, useFade?: boolean): boolean => {
    if (!time || !time.year) return false;
    const now = new Date(Date.now() - 2000);
    const targetDate = new Date(time.year, time.month - 1, time.day, time.hour, time.minute, time.second);
    const fadeInMs = useFade ? (fadeInSec || 0) * 1000 : 0;
    const actualStartDate = new Date(targetDate.getTime() - fadeInMs);
    return actualStartDate >= now;
  }, []);

  const isEndTimeValidFn = useCallback((time: DateTimeValue, startTimeVal?: DateTimeValue): boolean => {
    if (!time || !time.year) return false;
    const now = new Date(Date.now() - 2000);
    const targetDate = new Date(time.year, time.month - 1, time.day, time.hour, time.minute, time.second);
    if (targetDate < now) return false;
    if (startTimeVal && startTimeVal.year) {
      const startDate = new Date(startTimeVal.year, startTimeVal.month - 1, startTimeVal.day, startTimeVal.hour, startTimeVal.minute, startTimeVal.second);
      if (targetDate.getTime() === startDate.getTime()) return false;
      return targetDate >= startDate;
    }
    return true;
  }, []);

  const validateStartTime = useCallback((time: DateTimeValue, fadeInSec?: number, useFade?: boolean) => {
    if (!time || !time.year) {
      setIsStartTimeValid(false);
      setStartTimeError("请设置开始时间");
      return;
    }
    const now = new Date(Date.now() - 2000);
    const targetDate = new Date(time.year, time.month - 1, time.day, time.hour, time.minute, time.second);
    const fadeInMs = useFade ? (fadeInSec || 0) * 1000 : 0;
    const actualStartDate = new Date(targetDate.getTime() - fadeInMs);

    if (actualStartDate < now) {
      setIsStartTimeValid(false);
      setStartTimeError(useFade && fadeInMs > 0 ? "距离开始时间不足以完成渐入，请调整开始时间或缩短渐入时长" : "开始时间不能早于当前时间");
    } else {
      setIsStartTimeValid(true);
      setStartTimeError(null);
    }
  }, []);

  const validateEndTime = useCallback((time: DateTimeValue, startTimeVal?: DateTimeValue) => {
    if (!time || !time.year) {
      setIsEndTimeValid(false);
      setEndTimeError("请设置结束时间");
      return;
    }
    const now = new Date(Date.now() - 2000);
    const targetDate = new Date(time.year, time.month - 1, time.day, time.hour, time.minute, time.second);

    if (targetDate < now) {
      setIsEndTimeValid(false);
      setEndTimeError("结束时间不能早于当前时间");
    } else if (startTimeVal && startTimeVal.year) {
      const startDate = new Date(startTimeVal.year, startTimeVal.month - 1, startTimeVal.day, startTimeVal.hour, startTimeVal.minute, startTimeVal.second);
      if (targetDate.getTime() === startDate.getTime()) {
        setIsEndTimeValid(false);
        setEndTimeError("结束时间不能与开始时间相同，请设置不同的时间");
      } else if (targetDate < startDate) {
        setIsEndTimeValid(false);
        setEndTimeError("结束时间不能早于开始时间");
      } else {
        setIsEndTimeValid(true);
        setEndTimeError(null);
      }
    } else {
      setIsEndTimeValid(true);
      setEndTimeError(null);
    }
  }, []);

  // 初始加载后 + startTime/endTime 变化时自动触发验证
  const currentFadeInSec = fadeInDuration || 0;
  useEffect(() => {
    validateStartTime(startTime, currentFadeInSec, enableFade);
  }, [startTime, currentFadeInSec, enableFade, validateStartTime]);

  useEffect(() => {
    validateEndTime(endTime, startTime);
  }, [endTime, startTime, validateEndTime]);

  // 每秒实时检查时间有效性
  useEffect(() => {
    const timer = setInterval(() => {
      validateStartTime(startTime, currentFadeInSec, enableFade);
      validateEndTime(endTime, startTime);
    }, 1000);
    return () => clearInterval(timer);
  }, [startTime, endTime, currentFadeInSec, enableFade, validateStartTime, validateEndTime]);

  const validateFile = useCallback((file: File): string | null => {
    const ext = "." + file.name.split(".").pop()?.toLowerCase();
    if (!AUDIO_EXTENSIONS.includes(ext) && !file.type.startsWith('audio/'))
      return `不支持的音频格式，请上传 ${AUDIO_EXTENSIONS.join(", ")} 文件`;
    if (audios.some(a => a.name === file.name))
      return `文件「${file.name}」已存在`;
    return null;
  }, [audios]);

  const updateUploadState = useCallback((id: string, updates: Partial<UploadState>) => {
    const current = uploadStatesRef.current[id];
    const nextState: UploadState = {
      uploading: updates.uploading ?? current?.uploading ?? false,
      uploadProgress: updates.uploadProgress ?? current?.uploadProgress ?? 0,
      uploadError: updates.uploadError !== undefined
        ? updates.uploadError
        : current?.uploadError ?? null,
    };
    uploadStatesRef.current = { ...uploadStatesRef.current, [id]: nextState };
    setUploadStates(uploadStatesRef.current);
  }, []);

  const uploadFile = useCallback(async (id: string, file: File) => {
    if (!user) return;

    updateUploadState(id, { uploading: true, uploadProgress: 0, uploadError: null });

    const formData = new FormData();
    formData.append("audio", file);

    try {
      const xhr = new XMLHttpRequest();

      await new Promise<void>((resolve, reject) => {
        xhr.upload.onprogress = (e) => e.lengthComputable &&
          updateUploadState(id, { uploadProgress: Math.round((e.loaded / e.total) * 100) });
        xhr.onload = () => xhr.status >= 200 && xhr.status < 300 ? resolve() : reject();
        xhr.onerror = () => reject();
        xhr.open("POST", "/api/audio/upload?save_to_files=true");
        xhr.send(formData);
      });

      const resp = JSON.parse(xhr.responseText);
      if (resp.success) {
        setAudios(previous => previous.map(audio => audio.id === id ? {
          ...audio,
          serverUrl: resp.audio_url,
          fileKey: resp.file_key,
        } : audio));
        updateUploadState(id, { uploading: false, uploadProgress: 100, uploadError: null });
      } else {
        updateUploadState(id, { uploading: false, uploadError: resp.error || "上传失败" });
      }
    } catch {
      updateUploadState(id, { uploading: false, uploadError: "上传失败" });
    }
  }, [setAudios, updateUploadState, user]);

  const processFiles = useCallback(async (files: FileList | File[]) => {
    setUploadError(null);
    const newAudios: TaskAudio[] = [];

    for (const file of Array.from(files).slice(0, MAX_FILES - audios.length)) {
      const error = validateFile(file);
      if (error) { setUploadError(error); continue; }

      const id = `audio-${++globalIdCounter}-${Date.now()}`;
      const url = URL.createObjectURL(file);
      let dbKey: string | undefined;

      try { await saveAudioBlob(id, file); dbKey = id; } catch {}

      fileRefs.current[id] = file;
      objectUrlRefs.current[id] = url;
      setLocalUrls(previous => ({ ...previous, [id]: url }));
      newAudios.push({ id, name: file.name, size: file.size, duration: 0, dbKey });
      if (user) void uploadFile(id, file);
    }

    if (newAudios.length) {
      const updated = [...audios, ...newAudios];
      setAudios(updated);
      onAudioUploaded?.(updated);
      onAudioCountChange?.(updated.length);
    }
  }, [audios, onAudioCountChange, onAudioUploaded, setAudios, uploadFile, user, validateFile]);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) void processFiles(e.target.files);
    e.target.value = "";
  }, [processFiles]);

  const handleRemove = useCallback((id: string) => {
    const audio = audios.find(a => a.id === id);
    if (audio) {
      if (audioRefs.current[id]) {
        audioRefs.current[id].pause();
        audioRefs.current[id].removeAttribute("src");
        audioRefs.current[id].load();
        delete audioRefs.current[id];
      }
      const objectUrl = objectUrlRefs.current[id];
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
        delete objectUrlRefs.current[id];
        setLocalUrls(previous => {
          const next = { ...previous };
          delete next[id];
          return next;
        });
      }
      delete fileRefs.current[id];
      delete uploadStatesRef.current[id];
      setUploadStates({ ...uploadStatesRef.current });
    }
    setAudios(prev => prev.filter(a => a.id !== id));
    setCurrentTimes(prev => { const next = { ...prev }; delete next[id]; return next; });
    if (playingId === id) setPlayingId(null);
    onAudioRemoved?.(id);
  }, [audios, onAudioRemoved, playingId, setAudios]);

  const togglePlay = useCallback((id: string) => {
    const el = audioRefs.current[id];
    if (!el) return;

    Object.values(audioRefs.current).forEach(ael => {
      if (ael && !ael.paused) ael.pause();
    });

    if (playingId === id) {
      el.pause();
      setPlayingId(null);
    } else {
      el.currentTime = 0;
      el.volume = volume / 100;
      el.play().catch(() => {});
      setPlayingId(id);
    }
  }, [playingId, volume]);

  // 播放进度更新
  useEffect(() => {
    const cleanupFunctions: (() => void)[] = [];

    Object.entries(audioRefs.current).forEach(([id, el]) => {
      if (!el || !isMountedRef.current) return;

      const handleTimeUpdate = () => {
        if (!isMountedRef.current) return;
        setCurrentTimes(prev => ({ ...prev, [id]: el.currentTime }));
      };
      const handleEnded = () => {
        if (isMountedRef.current) setPlayingId(null);
      };
      const handleLoadedMetadata = () => {
        if (!isMountedRef.current) return;
        setAudios(prev => prev.map(a => a.id === id ? { ...a, duration: el.duration || 0 } : a));
      };

      el.addEventListener("timeupdate", handleTimeUpdate);
      el.addEventListener("ended", handleEnded);
      el.addEventListener("loadedmetadata", handleLoadedMetadata);

      cleanupFunctions.push(() => {
        el.removeEventListener("timeupdate", handleTimeUpdate);
        el.removeEventListener("ended", handleEnded);
        el.removeEventListener("loadedmetadata", handleLoadedMetadata);
      });
    });

    return () => {
      cleanupFunctions.forEach(cleanup => cleanup());
    };
  }, [audios, setAudios]);

  // 音量变化时同步到所有音频播放器
  useEffect(() => {
    if (!mounted) return;
    Object.values(audioRefs.current).forEach(el => {
      if (el) el.volume = volume / 100;
    });
  }, [mounted, volume]);

  const handleClearAll = useCallback(() => {
    Object.entries(audioRefs.current).forEach(([id, el]) => {
      if (el) { try { el.pause(); el.src = ''; } catch {} delete audioRefs.current[id]; }
    });
    Object.values(objectUrlRefs.current).forEach(url => URL.revokeObjectURL(url));
    objectUrlRefs.current = {};
    fileRefs.current = {};
    uploadStatesRef.current = {};
    setLocalUrls({});
    setUploadStates({});
    setAudios([]);
    setCurrentTimes({});
    setPlayingId(null);
    setShowClearConfirm(false);
  }, [setAudios]);

  // 拖拽排序相关（仅完整模式）
  const handleDragStart = useCallback((index: number) => {
    setDragIndex(index);
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (dragIndex === null || dragIndex === index) return;
    setAudios(prev => {
      const next = [...prev];
      const [moved] = next.splice(dragIndex, 1);
      next.splice(index, 0, moved);
      return next;
    });
    setDragIndex(index);
  }, [dragIndex, setAudios]);

  const handleDragEnd = useCallback(() => {
    setIsSwapAnimating(true);
    setDragIndex(null);
    onOrderChange?.(audios.map(a => a.id));
    setTimeout(() => setIsSwapAnimating(false), 700);
  }, [onOrderChange, audios]);

  // 移动端触摸拖拽排序
  const LONG_PRESS_DURATION = 500;

  const handleTouchStart = useCallback((e: React.TouchEvent, audioId: string, index: number) => {
    if (disabled) return;

    if (longPressTimerRef.current) {
      clearTimeout(longPressTimerRef.current);
      longPressTimerRef.current = null;
    }

    const touch = e.touches[0];
    const startY = touch.clientY;
    setDragOffsetY(startY);
    setDragStartIndex(index);

    longPressTimerRef.current = setTimeout(() => {
      const item = itemRefs.current[audioId];
      if (item) {
        const rect = item.getBoundingClientRect();
        setDraggingY(rect.top);
        setDragOffsetY(touch.clientY - rect.top);
        setIsDragging(true);
        setDraggingId(audioId);
        setDragStartIndex(index);
        item.classList.add("dragging-item");
        if (navigator.vibrate) navigator.vibrate(50);
      }
    }, LONG_PRESS_DURATION);

    const handleTouchMove = (moveEvent: TouchEvent) => {
      const moveTouch = moveEvent.touches[0];
      const deltaY = Math.abs(moveTouch.clientY - startY);
      if (deltaY > 10) {
        if (longPressTimerRef.current) {
          clearTimeout(longPressTimerRef.current);
          longPressTimerRef.current = null;
        }
      }
    };

    const handleTouchEnd = () => {
      document.removeEventListener("touchmove", handleTouchMove);
      document.removeEventListener("touchend", handleTouchEnd);
      if (longPressTimerRef.current) {
        clearTimeout(longPressTimerRef.current);
        longPressTimerRef.current = null;
      }
    };

    document.addEventListener("touchmove", handleTouchMove, { passive: true });
    document.addEventListener("touchend", handleTouchEnd, { passive: true });
  }, [disabled]);

  const handleTouchMoveDrag = useCallback((e: TouchEvent) => {
    if (!isDragging || !draggingId || dragStartIndex === null) return;
    e.preventDefault(); e.stopPropagation();

    const touch = e.touches[0];
    const currentTime = Date.now();
    if (currentTime - lastMoveTimeRef.current < 16) return;
    lastMoveTimeRef.current = currentTime;

    const newY = touch.clientY - dragOffsetY;
    setDraggingY(newY);

    const container = document.querySelector(".audio-list-container");
    if (!container) return;

    const items = container.querySelectorAll("[data-audio-item]");
    let targetIndex = dragStartIndex;

    items.forEach((item, index) => {
      const rect = item.getBoundingClientRect();
      const itemCenterY = rect.top + rect.height / 2;
      if (touch.clientY > itemCenterY && index > dragStartIndex) targetIndex = index;
      else if (touch.clientY < itemCenterY && index < dragStartIndex) targetIndex = index;
    });

    const audioIndex = audios.findIndex(a => a.id === draggingId);
    if (audioIndex !== -1 && audioIndex !== targetIndex) {
      setAudios(prev => {
        const next = [...prev];
        const [moved] = next.splice(audioIndex, 1);
        next.splice(targetIndex, 0, moved);
        return next;
      });
      setDragStartIndex(targetIndex);
    }
  }, [isDragging, draggingId, dragStartIndex, dragOffsetY, audios, setAudios]);

  const handleTouchEndDrag = useCallback(() => {
    if (!isDragging || !draggingId) return;

    const item = itemRefs.current[draggingId];
    if (item) item.classList.remove("dragging-item");

    setIsDragging(false);
    setDraggingId(null);
    setDraggingY(0);
    setDragOffsetY(0);
    setDragStartIndex(null);
    lastMoveYRef.current = 0;
    lastMoveTimeRef.current = 0;

    onOrderChange?.(audios.map(a => a.id));
  }, [isDragging, draggingId, onOrderChange, audios]);

  // 鼠标拖拽支持（桌面端）
  const handleMouseDown = useCallback((e: React.MouseEvent, audioId: string, index: number) => {
    if (disabled) return;
    if (e.button !== 0) return;

    const item = itemRefs.current[audioId];
    if (!item) return;

    const rect = item.getBoundingClientRect();
    const offsetY = e.clientY - rect.top;
    setDragOffsetY(offsetY);
    setDragStartIndex(index);

    const handleMouseMove = (moveEvent: MouseEvent) => {
      const currentY = moveEvent.clientY;
      const currentTime = Date.now();

      if (!isDragging) {
        setIsDragging(true);
        setDraggingId(audioId);
        setDraggingY(rect.top);
      }

      if (currentTime - lastMoveTimeRef.current < 16) return;
      lastMoveTimeRef.current = currentTime;

      setDraggingY(currentY - offsetY);

      const container = document.querySelector(".audio-list-container");
      if (!container) return;

      const items = container.querySelectorAll("[data-audio-item]");
      let targetIndex = dragStartIndex || index;

      items.forEach((item, idx) => {
        const itemRect = item.getBoundingClientRect();
        const itemCenterY = itemRect.top + itemRect.height / 2;
        if (currentY > itemCenterY && idx > (dragStartIndex || index)) targetIndex = idx;
        else if (currentY < itemCenterY && idx < (dragStartIndex || index)) targetIndex = idx;
      });

      const audioIndex = audios.findIndex(a => a.id === audioId);
      if (audioIndex !== -1 && audioIndex !== targetIndex) {
        setAudios(prev => {
          const next = [...prev];
          const [moved] = next.splice(audioIndex, 1);
          next.splice(targetIndex, 0, moved);
          return next;
        });
        setDragStartIndex(targetIndex);
      }
    };

    const handleMouseUp = () => {
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);

      const item = itemRefs.current[audioId];
      if (item) item.classList.remove("dragging-item");

      setIsDragging(false);
      setDraggingId(null);
      setDraggingY(0);
      setDragOffsetY(0);
      setDragStartIndex(null);
      lastMoveYRef.current = 0;
      lastMoveTimeRef.current = 0;

      onOrderChange?.(audios.map(a => a.id));
    };

    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", handleMouseUp);
  }, [disabled, isDragging, dragStartIndex, audios, onOrderChange, setAudios]);

  // 全局触摸移动事件监听（用于拖拽过程中的移动）
  useEffect(() => {
    if (!mounted || !isDragging) return;

    const handleTouchMove = (e: TouchEvent) => {
      handleTouchMoveDrag(e);
    };

    const handleTouchEnd = () => {
      handleTouchEndDrag();
    };

    document.addEventListener("touchmove", handleTouchMove, { passive: false });
    document.addEventListener("touchend", handleTouchEnd);

    return () => {
      document.removeEventListener("touchmove", handleTouchMove);
      document.removeEventListener("touchend", handleTouchEnd);
    };
  }, [mounted, isDragging, handleTouchMoveDrag, handleTouchEndDrag]);

  const handleUploadSingle = useCallback(async (id: string) => {
    const audio = audios.find(a => a.id === id);
    if (!audio || !user) return;

    let file = fileRefs.current[id];
    if (!file && audio.dbKey) {
      const blob = await getAudioBlob(audio.dbKey).catch(() => null);
      if (blob) file = new File([blob], audio.name, { type: blob.type || "audio/mpeg" });
    }

    if (!file) {
      updateUploadState(id, { uploading: false, uploadError: "找不到本地音频，请重新选择文件" });
      return;
    }

    await uploadFile(id, file);
  }, [audios, updateUploadState, uploadFile, user]);

  const handleUploadAll = useCallback(async () => {
    for (const audio of audios.filter(a => !a.serverUrl && !uploadStates[a.id]?.uploading)) {
      await handleUploadSingle(audio.id);
    }
  }, [audios, handleUploadSingle, uploadStates]);

  useEffect(() => { if (!mounted) return; onAudioCountChange?.(audios.length); }, [mounted, audios.length, onAudioCountChange]);

  const handleDreamPillow = async () => {
    const fadeInSec = fadeInDuration ?? 0;
    const startValid = isStartTimeValidFn(startTime, fadeInSec, enableFade);
    const endValid = isEndTimeValidFn(endTime, startTime);

    if (!startValid) {
      toast.error(enableFade && fadeInSec > 0 ? "距离开始时间不足以完成渐入，请调整开始时间或缩短渐入时长" : "开始时间不能早于当前时间，请重新设置");
      return;
    }
    if (!endValid) {
      toast.error("结束时间不能早于当前时间或开始时间，请重新设置");
      return;
    }
    if (audios.length === 0) {
      toast.error("请先上传音频");
      return;
    }
    if (audios.some(audio => !audio.fileKey && !audio.serverUrl && !audio.dbKey)) {
      toast.error("有音频尚未保存完成，请重新上传后再试");
      return;
    }

    const uploading = audios.filter(audio => uploadStatesRef.current[audio.id]?.uploading);
    if (uploading.length > 0) {
      toast.loading(`等待 ${uploading.length} 个音频上传完成...`, { id: "dream-upload" });
      for (let i = 0; i < 60; i++) {
        await new Promise(r => setTimeout(r, 500));
        const stillUploading = Object.values(uploadStatesRef.current).some(state => state.uploading);
        if (!stillUploading) break;
      }
      toast.dismiss("dream-upload");
    }

    if (user) {
      const failedAudios = audios.filter(audio => uploadStatesRef.current[audio.id]?.uploadError);
      if (failedAudios.length > 0) {
        toast.error(`有 ${failedAudios.length} 个音频上传失败，请移除后重试`);
        return;
      }
    }

    const config = {
      audios: audios.map(a => ({
        id: a.id,
        name: a.name,
        url: a.serverUrl || (a.fileKey ? `/api/audio/proxy?key=${encodeURIComponent(a.fileKey)}` : undefined),
        fileKey: a.fileKey,
        serverUrl: a.serverUrl,
        dbKey: a.dbKey,
        duration: a.duration,
        size: a.size,
      })),
      volume,
      fadeInDuration,
      fadeOutDuration,
      enableFade,
      startTime: { year: startTime.year, month: startTime.month, day: startTime.day, hour: startTime.hour, minute: startTime.minute, second: startTime.second },
      endTime: { year: endTime.year, month: endTime.month, day: endTime.day, hour: endTime.hour, minute: endTime.minute, second: endTime.second },
      createdAt: Date.now(),
    };
    localStorage.setItem("dream_config", JSON.stringify(config));
    router.push("/templates");
  };

  const allUploaded = audios.length > 0 && audios.every(a => a.serverUrl || a.fileKey);
  const anyUploading = Object.values(uploadStates).some(state => state.uploading);

  // 上传文件 input 的 ref
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 渲染上传区域
  const renderUploadArea = () => (
    <div
      onClick={() => { if (!disabled) fileInputRef.current?.click(); }}
      onDragOver={(e) => { e.preventDefault(); if (!disabled) setDragOver(true); }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => { e.preventDefault(); if (!disabled) { setDragOver(false); processFiles(e.dataTransfer.files); } }}
      className={cn(
        "relative block p-4 sm:p-6 transition-all duration-300 cursor-pointer rounded-2xl select-none touch-manipulation active:scale-[0.98]",
        dragOver && !disabled && "scale-[1.02]",
        disabled && "opacity-50 cursor-not-allowed pointer-events-none"
      )}
    >
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept={AUDIO_ACCEPT}
        onChange={handleFileSelect}
        disabled={disabled}
        className="sr-only"
      />
      <div className="flex flex-col items-center gap-2 sm:gap-3">
        <div className={cn("p-2.5 sm:p-3 rounded-full bg-[var(--brand-glow)]/10 transition-transform duration-300", dragOver && !disabled && "scale-110")}>
          <Upload className="w-5 h-5 sm:w-6 sm:h-6 text-[var(--brand-glow)]" />
        </div>
        <div className="text-center">
          <p className="text-sm font-medium text-foreground leading-relaxed px-2">
            {disabled ? "请先登录" : dragOver ? "松开以上传" : "点击或拖拽音频文件到此处"}
          </p>
          {audios.length > 0 && (
            <p className="text-xs text-[var(--brand-start)] font-medium">已添加 {audios.length}/{MAX_FILES} 个音频</p>
          )}
        </div>
      </div>
    </div>
  );

  // 渲染音频列表
  const renderAudioList = () => (
    audios.length > 0 && (
      <div className="space-y-3">
        <div className="flex items-center justify-between px-1">
          <h3 className="text-sm font-medium text-foreground flex items-center gap-2">
            <Music2 className="w-4 h-4 text-[var(--brand-start)]" />
            音频列表（{audios.length}）
            <span className="text-xs font-normal text-muted-foreground">· 拖拽调整播放顺序</span>
          </h3>
          {!allUploaded && user && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={handleUploadAll}
              disabled={anyUploading}
              className="rounded-lg text-xs h-8"
            >
              {anyUploading ? (
                <><Spinner size="sm" className="mr-1 h-3 w-3" />上传中...</>
              ) : (
                <><Upload className="w-3 h-3 mr-1" />全部上传</>
              )}
            </Button>
          )}
        </div>

        <div className="space-y-2 audio-list-container">
          {audios.map((audio, index) => {
            const isPlaying = playingId === audio.id;
            const currentTime = currentTimes[audio.id] || 0;
            const isDraggingThis = dragIndex === index;
            const isCurrentlyDragging = isDraggingThis && draggingId === audio.id;
            const uploadState = uploadStates[audio.id];
            const audioSource = audio.serverUrl
              || (audio.fileKey ? `/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}` : undefined)
              || localUrls[audio.id];

            return (
              <div
                key={audio.id}
                data-audio-item={audio.id}
                ref={el => { itemRefs.current[audio.id] = el; }}
                draggable={!disabled}
                onDragStart={() => handleDragStart(index)}
                onDragOver={(e) => handleDragOver(e, index)}
                onDragEnd={handleDragEnd}
                onTouchStart={(e) => handleTouchStart(e, audio.id, index)}
                onMouseDown={(e) => handleMouseDown(e, audio.id, index)}
                onClick={() => togglePlay(audio.id)}
                className={cn(
                  "group/audio relative cursor-pointer select-none transition-all duration-200",
                  isSwapAnimating && "transition-all duration-[700ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
                  isDraggingThis && "scale-[1.02] opacity-80 z-10",
                  disabled && "opacity-50 pointer-events-none"
                )}
                style={{
                  ...(isPlaying && isCurrentlyDragging && {
                    background: "radial-gradient(ellipse at 20% 50%, color-mix(in srgb, var(--brand-start)) 12%, transparent) 40%, color-mix(in srgb, var(--brand-end)) 6% transparent 70%, transparent 100%"
                  }),
                  ...(isCurrentlyDragging && {
                    position: "fixed",
                    top: draggingY,
                    left: 0,
                    right: 0,
                    zIndex: 9999,
                    transform: "scale(1.02)",
                    boxShadow: "0 20px 40px rgba(0, 0, 0, 0.15)",
                    opacity: 0.95,
                  }),
                }}
              >
                <div className="p-3 sm:p-3 space-y-2">
                  <div className="flex items-center gap-3">
                    <div draggable={!disabled} onDragStart={() => handleDragStart(index)} onDragOver={(e) => handleDragOver(e, index)} onDragEnd={handleDragEnd} className="flex-shrink-0 w-5 cursor-grab active:cursor-grabbing touch-none">
                      <GripVertical className="w-4 h-4 text-muted-foreground/30 group-hover/audio:text-muted-foreground transition-colors" />
                    </div>

                    <button onClick={e => { e.stopPropagation(); togglePlay(audio.id); }} className={cn(
                      "flex-shrink-0 w-9 h-9 sm:w-8 sm:h-8 rounded-lg bg-muted/80 border border-border/40 flex items-center justify-center active:scale-95 transition-all",
                      isPlaying && "text-[var(--brand-start)] border-[var(--brand-start)]/30",
                      !isPlaying && "text-foreground hover:text-[var(--brand-start)] hover:border-[var(--brand-start)]/30"
                    )}>
                      {isPlaying ? <Pause className="w-3.5 h-3.5 sm:w-3 sm:h-3" fill="currentColor" /> : <Play className="w-3.5 h-3.5 sm:w-3 sm:h-3 ml-0.5" fill="currentColor" />}
                    </button>

                    <div className="flex-1 min-w-0">
                      <p className={cn(
                        "font-medium text-sm truncate transition-colors",
                        isPlaying && "text-[var(--brand-start)]",
                        !isPlaying && "text-foreground group-hover/audio:text-[var(--brand-start)]"
                      )}>
                        {audio.name}
                      </p>
                      <div className="flex items-center gap-2 mt-0.5 text-[11px] text-muted-foreground">
                        <span className="flex items-center gap-1"><VolumeIcon className="w-3 h-3" />{formatFileSize(audio.size)}</span>
                        {audio.duration > 0 && <span className="flex items-center gap-1"><Clock className="w-3 h-3" />{formatDuration(audio.duration)}</span>}
                        {audio.serverUrl && <span className="flex items-center gap-1 text-green-500"><CheckCircle2 className="w-3 h-3" />已上传</span>}
                        {uploadState?.uploading && <span className="flex items-center gap-1 text-[var(--brand-start)]"><Spinner size="sm" className="h-3 w-3" />{uploadState.uploadProgress}%</span>}
                      </div>
                    </div>

                    <div className="flex items-center gap-1 flex-shrink-0 opacity-0 group-hover/audio:opacity-100 focus-within:opacity-100 transition-opacity">
                      {!audio.serverUrl && !uploadState?.uploading && user && <Button variant="ghost" size="icon" onClick={e => { e.stopPropagation(); handleUploadSingle(audio.id); }} className="w-8 h-8 text-muted-foreground hover:text-[var(--brand-start)]" title="上传此文件"><Upload className="w-3.5 h-3.5" /></Button>}
                      <Button variant="ghost" size="icon" onClick={e => { e.stopPropagation(); handleRemove(audio.id); }} className="w-8 h-8 text-muted-foreground hover:text-red-500" title="移除"><Trash2 className="w-3.5 h-3.5" /></Button>
                    </div>

                    <button onClick={e => { e.stopPropagation(); handleRemove(audio.id); }} className="flex-shrink-0 w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-500 hover:bg-muted/60 active:bg-muted transition-all lg:hidden">
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {audio.duration > 0 && (
                    <div className="space-y-1">
                      <input type="range" min={0} max={audio.duration} value={currentTime} step={0.1} onChange={e => {
                        e.stopPropagation();
                        const el = audioRefs.current[audio.id];
                        if (el) el.currentTime = parseFloat(e.target.value);
                      }} onClick={e => e.stopPropagation()} className={cn(
                        "w-full h-1.5 rounded-full appearance-none bg-border/40 cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:transition-transform [&::-webkit-slider-thumb]:hover:scale-125",
                        isPlaying ? "[&::-webkit-slider-thumb]:bg-[var(--brand-start)]" : "[&::-webkit-slider-thumb]:bg-[var(--brand-start)]"
                      )} style={{
                        background: `linear-gradient(to right, var(--brand-start) ${(currentTime / audio.duration) * 100}%, rgba(128,128,128,0.25) ${(currentTime / audio.duration) * 100}%)`
                      }} />
                      <div className="flex justify-between text-[10px] text-muted-foreground font-mono">
                        <span>{formatDuration(currentTime)}</span>
                        <span>{formatDuration(audio.duration)}</span>
                      </div>
                    </div>
                  )}

                  {uploadState?.uploading && (
                    <div className="space-y-1">
                      <div className="w-full h-1.5 rounded-full bg-border/40 overflow-hidden">
                        <div className="h-full rounded-full bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] transition-all duration-200" style={{ width: `${uploadState.uploadProgress}%` }} />
                      </div>
                    </div>
                  )}

                  {uploadState?.uploadError && (
                    <div className="flex items-center gap-2 p-2 rounded-lg bg-red-950/15 border border-red-900/30">
                      <AlertCircle className="w-3.5 h-3.5 text-red-400 flex-shrink-0" />
                      <span className="text-xs text-red-400">{uploadState.uploadError}</span>
                    </div>
                  )}
                </div>

                <audio
                  ref={el => { if (el) audioRefs.current[audio.id] = el; }}
                  {...(audioSource ? { src: audioSource } : {})}
                  preload="metadata"
                  onLoadedMetadata={() => {
                    const el = audioRefs.current[audio.id];
                    if (el && audio.duration === 0 && el.duration) {
                      setAudios(prev => prev.map(a => a.id === audio.id ? { ...a, duration: el.duration } : a));
                    }
                  }}
                  className="hidden"
                />
              </div>
            );
          })}
        </div>

        <div className="flex items-center justify-between pt-2 px-1">
          <p className="text-xs text-muted-foreground">共 {audios.length} 个音频{audios.length > 1 && " · 拖拽调整播放顺序"}{allUploaded && user && " · 全部已上传"}</p>
          {audios.length > 1 && <button onClick={() => setShowClearConfirm(true)} className="text-xs text-muted-foreground hover:text-red-500 transition-colors">清空全部</button>}
        </div>
      </div>
    )
  );

  // 渲染音量控制（两种模式都有）
  const renderVolumeControl = () => (
    <div>
      <div className="p-3 sm:p-4 space-y-2.5">
        <div className="flex items-center justify-between">
          <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
            <VolumeIcon className="w-3.5 h-3.5" />音量控制
          </label>
          <span className="text-sm font-mono font-semibold tabular-nums text-foreground">{volume}%</span>
        </div>
        <div className="relative pt-1 pb-1">
          <input
            type="range"
            min={0} max={100}
            value={volume} step={1}
            onInput={e => setVolume(parseInt((e.target as HTMLInputElement).value, 10))}
            className="w-full h-2.5 rounded-full appearance-none bg-border/30 cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-6 [&::-webkit-slider-thumb]:h-6 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-[var(--brand-start)] sm:[&::-webkit-slider-thumb]:w-5 sm:[&::-webkit-slider-thumb]:h-5"
            style={{
              background: `linear-gradient(to right, var(--brand-start) ${volume}%, rgba(128,128,128,0.2) ${volume}%)`
            }}
          />
        </div>
      </div>
    </div>
  );

  // 渲染完整模式的特有内容
  const renderFullModeContent = () => {
    // 如果没有提供 mode/onModeChange，说明是 TaskForm 中使用的简单版本
    const isSimpleMode = !mode || !onModeChange;

    if (isSimpleMode) {
      return (
        <>
          {renderUploadArea()}

          {showGuestTip && (
            <div className="flex items-center justify-between gap-3 p-3 rounded-xl bg-amber-950/20 border border-amber-900/30 animate-in fade-in slide-in-from-top-2 duration-200">
              <p className="text-sm text-amber-400">请先登录后再上传音频文件</p>
              <button onClick={() => setShowGuestTip(false)} className="text-amber-400/60 hover:text-amber-400 transition-colors"><X className="w-4 h-4" /></button>
            </div>
          )}

          {uploadError && (
            <div className="flex items-center gap-2 p-3 rounded-xl bg-red-950/20 border border-red-900/30">
              <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />
              <span className="text-sm text-red-400">{uploadError}</span>
              <button onClick={() => setUploadError(null)} className="ml-auto text-red-400/60 hover:text-red-400"><X className="w-4 h-4" /></button>
            </div>
          )}

          {renderAudioList()}

          {renderVolumeControl()}
        </>
      );
    }

    // 完整的设置页面版本
    return (
      <>
        <div className="flex flex-col items-center gap-2.5 pb-4 sm:pb-0">
          <ModeSwitch mode={mode || "default"} onModeChange={onModeChange || (() => {})} />
          <p className="text-xs text-muted-foreground/60 text-center leading-relaxed px-4">
            {mode === "default" ? "设置开始和结束时间，快速启动播放" : "创建定时任务，支持重复执行和任务管理"}
          </p>
        </div>

        {/* 默认模式内容 */}
        {mode === "default" && (
          <>
            {renderUploadArea()}

            {showGuestTip && (
              <div className="flex items-center justify-between gap-3 p-3 rounded-xl bg-amber-950/20 border border-amber-900/30 animate-in fade-in slide-in-from-top-2 duration-200">
                <p className="text-sm text-amber-400">请先登录后再上传音频文件</p>
                <button onClick={() => setShowGuestTip(false)} className="text-amber-400/60 hover:text-amber-400 transition-colors"><X className="w-4 h-4" /></button>
              </div>
            )}

            {uploadError && (
              <div className="flex items-center gap-2 p-3 rounded-xl bg-red-950/20 border border-red-900/30">
                <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />
                <span className="text-sm text-red-400">{uploadError}</span>
                <button onClick={() => setUploadError(null)} className="ml-auto text-red-400/60 hover:text-red-400"><X className="w-4 h-4" /></button>
              </div>
            )}

            {renderAudioList()}

            {renderVolumeControl()}

            <div>
              <div className="p-3 sm:p-5 space-y-3 sm:space-y-4">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5"><Timer className="w-3.5 h-3.5" />播放时段</label>
                  {startTime.year && endTime.year && (() => {
                    const startMs = new Date(startTime.year, startTime.month - 1, startTime.day, startTime.hour, startTime.minute, startTime.second).getTime();
                    const endMs = new Date(endTime.year, endTime.month - 1, endTime.day, endTime.hour, endTime.minute, endTime.second).getTime();
                    const diffMin = Math.round((endMs - startMs) / 60000);
                    if (diffMin > 0) {
                      const h = Math.floor(diffMin / 60);
                      const m = diffMin % 60;
                      return <span className="text-xs font-mono text-[var(--brand-start)] tabular-nums">共 {h > 0 ? `${h}小时` : ""}{m > 0 ? `${m}分钟` : (h > 0 ? "" : "0分钟")}</span>;
                    }
                    return null;
                  })()}
                </div>

                <div className="space-y-2 sm:space-y-3">
                  <WheelDateTimePicker label="开始时间" value={startTime} onChange={setStartTime} />
                  <WheelDateTimePicker label="结束时间" value={endTime} onChange={setEndTime} />
                </div>

                {!isStartTimeValid && (
                  <div className="mt-1 px-3 py-1.5 bg-red-500/10 border border-red-500/30 rounded-lg flex items-center gap-2">
                    <AlertCircle className="w-3.5 h-3.5 text-red-400" />
                    <span className="text-xs text-red-400">{startTimeError}</span>
                  </div>
                )}
                {!isEndTimeValid && (
                  <div className="mt-1 px-3 py-1.5 bg-red-500/10 border border-red-500/30 rounded-lg flex items-center gap-2">
                    <AlertCircle className="w-3.5 h-3.5 text-red-400" />
                    <span className="text-xs text-red-400">{endTimeError}</span>
                  </div>
                )}

                <FadeControls
                  className="pt-3"
                  enabled={enableFade}
                  fadeInDuration={fadeInDuration}
                  fadeOutDuration={fadeOutDuration}
                  onEnabledChange={setEnableFade}
                  onFadeInDurationChange={setFadeInDuration}
                  onFadeOutDurationChange={setFadeOutDuration}
                  showHint
                />
              </div>
            </div>

            <button onClick={handleDreamPillow} disabled={audios.length === 0 || !isStartTimeValid || !isEndTimeValid} className={cn(
              "w-full mt-4 relative overflow-hidden px-6 py-4 rounded-xl font-bold text-lg transition-all duration-300 transform",
              (audios.length === 0 || !isStartTimeValid || !isEndTimeValid)
                ? "text-white/50 cursor-not-allowed opacity-60"
                : "text-[#050510] hover:-translate-y-1 fill-btn"
            )} style={{
              background: (audios.length === 0 || !isStartTimeValid || !isEndTimeValid)
                ? "linear-gradient(135deg, #666666 0%, #555555 50%, #666666 100%)"
                : "linear-gradient(135deg, #00d4aa 0%, #00b894 50%, #00d4aa 100%)",
              boxShadow: (audios.length === 0 || !isStartTimeValid || !isEndTimeValid)
                ? "0 2px 8px rgba(0, 0, 0, 0.2)"
                : "0 4px 15px rgba(0, 212, 170, 0.3)",
            }}>
              <span className="relative flex items-center justify-center gap-2">
                <Image src="/logo.png" alt="梦枕" width={24} height={24} className={cn("rounded shadow-[inset_0_1px_3px_rgba(0,0,0,0.3)]", (audios.length === 0 || !isStartTimeValid) && "opacity-50")} />
                <span>{!isStartTimeValid ? "开始时间无效" : !isEndTimeValid ? "结束时间无效" : audios.length === 0 ? "请先上传音频" : "一键梦枕"}</span>
              </span>
            </button>
          </>
        )}

        {/* 自定义任务模式才有的 children */}
        {mode === "custom" && children}
      </>
    );
  };

  return (
    <>
      <style dangerouslySetInnerHTML={{ __html: dragStyles }} />
      <div className="space-y-3 sm:space-y-6">
        {renderFullModeContent()}
      </div>

      {showClearConfirm && portalReady && (
        <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/50 backdrop-blur-sm" style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0 }} onClick={() => setShowClearConfirm(false)}>
          <div role="dialog" aria-modal="true" aria-labelledby="clear-confirm-title" aria-describedby="clear-confirm-desc" className="bg-background border border-border/60 rounded-2xl shadow-2xl p-6 max-w-sm w-[calc(100%-2rem)] space-y-4" onClick={e => e.stopPropagation()}>
            <div className="flex flex-col items-center gap-3 text-center">
              <div className="w-12 h-12 rounded-full bg-red-500/10 flex items-center justify-center"><Trash2 className="w-6 h-6 text-red-500" /></div>
              <div><h3 id="clear-confirm-title" className="text-base font-semibold text-foreground">确认清空当前音频列表？</h3><p id="clear-confirm-desc" className="text-sm text-muted-foreground mt-1.5">只会移除当前配置中的引用，已经保存的任务不会受到影响。</p></div>
            </div>
            <div className="flex gap-3">
              <button onClick={() => setShowClearConfirm(false)} className="flex-1 h-10 rounded-xl border border-border/60 text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted/80 active:bg-muted transition-all cursor-pointer">取消</button>
              <button onClick={handleClearAll} className="flex-1 h-10 rounded-xl text-sm font-bold bg-red-500 text-white hover:bg-red-500/90 active:bg-red-500/80 transition-all cursor-pointer">确认清空</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
