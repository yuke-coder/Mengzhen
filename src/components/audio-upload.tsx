"use client";

import React, { useState, useCallback, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import type { TaskAudio } from "@/lib/task-types";
import { AUDIO_ACCEPT, MAX_FILES, formatFileSize, formatDuration } from "@/lib/audio";
import type { PlaybackController } from "@/hooks/use-playback-controller";
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

interface AudioUploadProps {
  controller: PlaybackController;
  onAudioUploaded?: (audios: TaskAudio[]) => void;
  onAudioRemoved?: (id: string) => void;
  onOrderChange?: (orderedIds: string[]) => void;
  onAudioCountChange?: (count: number) => void;
  disabled?: boolean;
}

function useClientOnly() {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  return mounted;
}

function PreviewAudio({
  audio,
  source,
  preview,
}: {
  audio: TaskAudio;
  source?: string;
  preview: PlaybackController["preview"];
}) {
  const { bindAudioElement, updateTime, loadedMetadata, ended } = preview;
  const bindElement = useCallback((element: HTMLAudioElement | null) => (
    bindAudioElement(audio.id, element)
  ), [audio.id, bindAudioElement]);

  return (
    <audio
      ref={bindElement}
      {...(source ? { src: source } : {})}
      preload="metadata"
      onTimeUpdate={event => updateTime(audio.id, event.currentTarget.currentTime)}
      onLoadedMetadata={event => loadedMetadata(audio.id, event.currentTarget.duration)}
      onEnded={() => ended(audio.id)}
      className="hidden"
    />
  );
}

export function AudioUpload({
  controller,
  onAudioUploaded,
  onAudioRemoved,
  onOrderChange,
  onAudioCountChange,
  disabled = false,
}: AudioUploadProps) {
  const mounted = useClientOnly();

  const { audios, volume } = controller.draft;
  const { assets, preview } = controller;
  const {
    uploads: uploadStates,
    error: uploadError,
    isUploading: anyUploading,
    allUploaded,
    isAuthenticated: user,
    sourceFor,
    addFiles,
    retryUpload,
    uploadAll,
    removeAudio,
    clearAudios,
    clearError,
  } = assets;
  const {
    playingId,
    currentTimes,
    toggle: togglePreview,
    seek: seekPreview,
  } = preview;
  const { setVolume, moveAudio } = controller;

  const [dragOver, setDragOver] = useState(false);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [portalReady, setPortalReady] = useState(false);

  // 仅完整模式需要的状态
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [isSwapAnimating, setIsSwapAnimating] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [draggingY, setDraggingY] = useState(0);
  const [dragOffsetY, setDragOffsetY] = useState(0);
  const [dragStartIndex, setDragStartIndex] = useState<number | null>(null);
  const longPressTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const gestureCleanupRef = useRef<(() => void) | null>(null);
  const swapAnimationTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const itemRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const lastMoveYRef = useRef(0);
  const lastMoveTimeRef = useRef(0);

  useEffect(() => () => {
    gestureCleanupRef.current?.();
    if (longPressTimerRef.current) clearTimeout(longPressTimerRef.current);
    if (swapAnimationTimerRef.current) clearTimeout(swapAnimationTimerRef.current);
  }, []);

  const VolumeIcon = volume === 0 ? VolumeX : volume < 50 ? Volume1 : Volume2;

  useEffect(() => setPortalReady(true), []);

  const processFiles = useCallback(async (files: FileList | File[]) => {
    const result = await addFiles(files);
    if (result.added.length > 0) onAudioUploaded?.(result.allAudios);
    onAudioCountChange?.(result.allAudios.length);
  }, [addFiles, onAudioCountChange, onAudioUploaded]);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) void processFiles(e.target.files);
    e.target.value = "";
  }, [processFiles]);

  const handleRemove = useCallback((id: string) => {
    removeAudio(id);
    onAudioRemoved?.(id);
  }, [onAudioRemoved, removeAudio]);

  const togglePlay = useCallback((id: string) => {
    togglePreview(id);
  }, [togglePreview]);

  const handleClearAll = useCallback(() => {
    clearAudios();
    setShowClearConfirm(false);
  }, [clearAudios]);

  // 拖拽排序相关（仅完整模式）
  const handleDragStart = useCallback((index: number) => {
    setDragIndex(index);
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (dragIndex === null || dragIndex === index) return;
    moveAudio(dragIndex, index);
    setDragIndex(index);
  }, [dragIndex, moveAudio]);

  const handleDragEnd = useCallback(() => {
    setIsSwapAnimating(true);
    setDragIndex(null);
    onOrderChange?.(audios.map(a => a.id));
    if (swapAnimationTimerRef.current) clearTimeout(swapAnimationTimerRef.current);
    swapAnimationTimerRef.current = setTimeout(() => {
      setIsSwapAnimating(false);
      swapAnimationTimerRef.current = null;
    }, 700);
  }, [onOrderChange, audios]);

  // 移动端触摸拖拽排序
  const LONG_PRESS_DURATION = 500;

  const handleTouchStart = useCallback((e: React.TouchEvent, audioId: string, index: number) => {
    if (disabled) return;
    gestureCleanupRef.current?.();

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

    const cleanupTouchListeners = () => {
      document.removeEventListener("touchmove", handleTouchMove);
      document.removeEventListener("touchend", handleTouchEnd);
      if (longPressTimerRef.current) {
        clearTimeout(longPressTimerRef.current);
        longPressTimerRef.current = null;
      }
      if (gestureCleanupRef.current === cleanupTouchListeners) gestureCleanupRef.current = null;
    };

    const handleTouchEnd = () => cleanupTouchListeners();

    document.addEventListener("touchmove", handleTouchMove, { passive: true });
    document.addEventListener("touchend", handleTouchEnd, { passive: true });
    gestureCleanupRef.current = cleanupTouchListeners;
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
      moveAudio(audioIndex, targetIndex);
      setDragStartIndex(targetIndex);
    }
  }, [isDragging, draggingId, dragStartIndex, dragOffsetY, audios, moveAudio]);

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
        moveAudio(audioIndex, targetIndex);
        setDragStartIndex(targetIndex);
      }
    };

    const cleanupMouseListeners = () => {
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);
      if (gestureCleanupRef.current === cleanupMouseListeners) gestureCleanupRef.current = null;
    };

    const handleMouseUp = () => {
      cleanupMouseListeners();

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

    gestureCleanupRef.current?.();
    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", handleMouseUp);
    gestureCleanupRef.current = cleanupMouseListeners;
  }, [disabled, isDragging, dragStartIndex, audios, moveAudio, onOrderChange]);

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

  useEffect(() => { if (!mounted) return; onAudioCountChange?.(audios.length); }, [mounted, audios.length, onAudioCountChange]);

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
              onClick={() => void uploadAll()}
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
            const audioSource = sourceFor(audio);

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
                      {!audio.serverUrl && !uploadState?.uploading && user && <Button variant="ghost" size="icon" onClick={e => { e.stopPropagation(); void retryUpload(audio.id); }} className="w-8 h-8 text-muted-foreground hover:text-[var(--brand-start)]" title="上传此文件"><Upload className="w-3.5 h-3.5" /></Button>}
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
                        seekPreview(audio.id, parseFloat(e.target.value));
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

                <PreviewAudio audio={audio} source={audioSource} preview={preview} />
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

  return (
    <>
      <style dangerouslySetInnerHTML={{ __html: dragStyles }} />
      <div className="space-y-3 sm:space-y-6">
        {renderUploadArea()}
        {uploadError && (
          <div className="flex items-center gap-2 p-3 rounded-xl bg-red-950/20 border border-red-900/30">
            <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />
            <span className="text-sm text-red-400">{uploadError}</span>
            <button onClick={clearError} className="ml-auto text-red-400/60 hover:text-red-400"><X className="w-4 h-4" /></button>
          </div>
        )}
        {renderAudioList()}
        {renderVolumeControl()}
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
