"use client";

import React, { useState, useCallback, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import type { TaskAudio } from "@/lib/task-types";
import { AUDIO_ACCEPT, formatFileSize, formatDuration } from "@/lib/audio";
import { toast } from "@/components/sonner";
import type { PlaybackController } from "@/hooks/use-playback-controller";
import {
  Upload,
  Music2,
  Play,
  Pause,
  Trash2,
  CheckCircle2,
  Volume2,
  Clock,
  GripVertical,
  Volume1,
  VolumeX,
} from "lucide-react";

interface AudioUploadProps {
  controller: PlaybackController;
  onAudioUploaded?: (audios: TaskAudio[]) => void;
  onAudioRemoved?: (id: string) => void;
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
      data-preview-audio={audio.id}
      {...(source ? { src: source } : {})}
      preload="metadata"
      onTimeUpdate={event => updateTime(audio.id, event.currentTarget.currentTime)}
      onLoadedMetadata={event => loadedMetadata(audio.id, event.currentTarget.duration)}
      onEnded={() => ended(audio.id)}
      className="hidden"
    />
  );
}

function AudioItemErrors({ uploadState }: { uploadState?: { uploadError?: string | null; libraryError?: string | null } }) {
  const shown = useRef<Set<string>>(new Set());
  useEffect(() => {
    if (uploadState?.uploadError && !shown.current.has(uploadState.uploadError)) {
      shown.current.add(uploadState.uploadError);
      toast.error(uploadState.uploadError);
    }
    if (uploadState?.libraryError && !shown.current.has(uploadState.libraryError)) {
      shown.current.add(uploadState.libraryError);
      toast.error(uploadState.libraryError);
    }
  }, [uploadState?.uploadError, uploadState?.libraryError]);
  return null;
}

export function AudioUpload({
  controller,
  onAudioUploaded,
  onAudioRemoved,
  onAudioCountChange,
  disabled = false,
}: AudioUploadProps) {
  const mounted = useClientOnly();

  const { audios, volume } = controller.draft;
  const { assets, preview } = controller;
  const {
    uploads: uploadStates,
    error: uploadError,
    isSavingToLibrary: anySavingToLibrary,
    allSavedToLibrary,
    isAuthenticated: user,
    sourceFor,
    addFiles,
    saveToLibrary,
    saveAllToLibrary,
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

  const [draggingId, setDraggingId] = useState<string | null>(null);
  const activeDragRef = useRef<{ pointerId: number; audioId: string } | null>(null);
  const suppressNextClickRef = useRef(false);
  const listRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const lastMoveTimeRef = useRef(0);

  const setVolumeFromPointer = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
    const { left, width } = event.currentTarget.getBoundingClientRect();
    if (!width) return;
    setVolume(Math.round(Math.min(1, Math.max(0, (event.clientX - left) / width)) * 100));
  }, [setVolume]);

  const handleVolumeKeyDown = useCallback((event: React.KeyboardEvent<HTMLDivElement>) => {
    let nextVolume = volume;
    switch (event.key) {
      case "Home": nextVolume = 0; break;
      case "End": nextVolume = 100; break;
      case "ArrowLeft":
      case "ArrowDown": nextVolume -= 1; break;
      case "ArrowRight":
      case "ArrowUp": nextVolume += 1; break;
      default: return;
    }
    event.preventDefault();
    setVolume(Math.max(0, Math.min(100, nextVolume)));
  }, [setVolume, volume]);

  const VolumeIcon = volume === 0 ? VolumeX : volume < 50 ? Volume1 : Volume2;

  useEffect(() => setPortalReady(true), []);

  useEffect(() => {
    if (uploadError) {
      toast.error(uploadError);
      clearError();
    }
  }, [uploadError, clearError]);

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

  const finishReorder = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
    const activeDrag = activeDragRef.current;
    if (!activeDrag || activeDrag.pointerId !== event.pointerId) return;

    event.stopPropagation();
    activeDragRef.current = null;
    if (event.type === "pointercancel") suppressNextClickRef.current = false;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }

    lastMoveTimeRef.current = 0;
    setDraggingId(null);
  }, []);

  const handlePointerDown = useCallback((event: React.PointerEvent<HTMLButtonElement>, audioId: string) => {
    if (disabled || !event.isPrimary || (event.pointerType === "mouse" && event.button !== 0)) return;

    event.preventDefault();
    event.stopPropagation();
    const list = listRef.current;
    if (!list) return;
    activeDragRef.current = { pointerId: event.pointerId, audioId };
    suppressNextClickRef.current = true;
    lastMoveTimeRef.current = 0;
    setDraggingId(audioId);
    list.setPointerCapture(event.pointerId);
  }, [disabled]);

  const handlePointerMove = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
    const activeDrag = activeDragRef.current;
    if (!activeDrag || activeDrag.pointerId !== event.pointerId) return;

    event.preventDefault();
    event.stopPropagation();
    const now = performance.now();
    if (now - lastMoveTimeRef.current < 16) return;
    lastMoveTimeRef.current = now;

    const currentAudios = controller.getDraft().audios;
    const currentIndex = currentAudios.findIndex(audio => audio.id === activeDrag.audioId);
    if (currentIndex === -1) return;

    let targetIndex = currentIndex;
    for (let index = 0; index < currentAudios.length; index += 1) {
      if (index === currentIndex) continue;
      const item = itemRefs.current[currentAudios[index].id];
      if (!item) continue;
      const rect = item.getBoundingClientRect();
      const centerY = rect.top + rect.height / 2;
      if (index < currentIndex && event.clientY < centerY) {
        targetIndex = index;
        break;
      }
      if (index > currentIndex && event.clientY > centerY) targetIndex = index;
    }

    if (targetIndex !== currentIndex) moveAudio(currentIndex, targetIndex);
  }, [controller, moveAudio]);

  const handleReorderKeyDown = useCallback((event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (disabled || (event.key !== "ArrowUp" && event.key !== "ArrowDown")) return;
    const targetIndex = event.key === "ArrowUp" ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= audios.length) return;

    event.preventDefault();
    event.stopPropagation();
    moveAudio(index, targetIndex);
  }, [audios.length, disabled, moveAudio]);

  const suppressReorderClick = useCallback((event: React.MouseEvent<HTMLDivElement>) => {
    if (!suppressNextClickRef.current) return;
    suppressNextClickRef.current = false;
    event.preventDefault();
    event.stopPropagation();
  }, []);

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
        aria-label="选择音频文件"
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
            {disabled ? "请先登录" : dragOver ? "松开以添加" : "点击或拖拽添加音频文件"}
          </p>
          {audios.length > 0 && (
            <p className="text-xs text-[var(--brand-start)] font-medium">已添加 {audios.length} 个音频</p>
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
            <span className="text-xs font-normal text-muted-foreground">· 拖动左侧手柄调整顺序</span>
          </h3>
          {!allSavedToLibrary && user && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => void saveAllToLibrary()}
              disabled={anySavingToLibrary}
              className="rounded-lg text-xs h-8"
            >
              {anySavingToLibrary ? (
                <><Spinner size="sm" className="mr-1 h-3 w-3" />保存中...</>
              ) : (
                <><Upload className="w-3 h-3 mr-1" />全部存入音频库</>
              )}
            </Button>
          )}
        </div>

        <div
          ref={listRef}
          className="space-y-2 audio-list-container"
          onPointerMove={handlePointerMove}
          onPointerUp={finishReorder}
          onPointerCancel={finishReorder}
          onLostPointerCapture={finishReorder}
          onPointerDownCapture={() => { suppressNextClickRef.current = false; }}
          onClickCapture={suppressReorderClick}
        >
          {audios.map((audio, index) => {
            const isPlaying = playingId === audio.id;
            const currentTime = currentTimes[audio.id] || 0;
            const isDraggingThis = draggingId === audio.id;
            const uploadState = uploadStates[audio.id];
            const audioSource = sourceFor(audio);
            const uploadStatus = uploadState?.uploading
              ? uploadState.queued
                ? "等待上传"
                : uploadState.processing
                  ? "正在确认音频"
                  : "正在上传"
              : uploadState?.savingToLibrary
                ? "正在存入音频库"
                : null;

            return (
              <div
                key={audio.id}
                data-audio-item={audio.id}
                ref={el => { itemRefs.current[audio.id] = el; }}
                onClick={() => togglePlay(audio.id)}
                className={cn(
                  "group/audio relative cursor-pointer select-none transition-all duration-200",
                  isDraggingThis && "scale-[1.02] opacity-80 z-10",
                  disabled && "opacity-50 pointer-events-none"
                )}
                style={{
                  ...(isPlaying && isDraggingThis && {
                    background: "radial-gradient(ellipse at 20% 50%, color-mix(in srgb, var(--brand-start)) 12%, transparent) 40%, color-mix(in srgb, var(--brand-end)) 6% transparent 70%, transparent 100%"
                  }),
                }}
              >
                <div className="p-3 sm:p-3 space-y-2">
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      aria-label={`调整「${audio.name}」的播放顺序`}
                      onClick={event => event.stopPropagation()}
                      onPointerDown={event => handlePointerDown(event, audio.id)}
                      onKeyDown={event => handleReorderKeyDown(event, index)}
                      disabled={disabled}
                      className="flex flex-shrink-0 w-5 cursor-grab items-center justify-center touch-none active:cursor-grabbing disabled:cursor-not-allowed"
                    >
                      <GripVertical className="w-4 h-4 text-muted-foreground/30 group-hover/audio:text-muted-foreground transition-colors" />
                    </button>

                    <button
                      type="button"
                      aria-label={`${isPlaying ? "暂停" : "试听"}「${audio.name}」`}
                      onClick={e => { e.stopPropagation(); togglePlay(audio.id); }}
                      className={cn(
                      "flex-shrink-0 w-9 h-9 sm:w-8 sm:h-8 rounded-lg bg-muted/80 border border-border/40 flex items-center justify-center active:scale-95 transition-all",
                      isPlaying && "text-[var(--brand-start)] border-[var(--brand-start)]/30",
                      !isPlaying && "text-foreground hover:text-[var(--brand-start)] hover:border-[var(--brand-start)]/30"
                    )}
                    >
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
                        {audio.savedToLibrary && <span className="flex items-center gap-1 text-green-500"><CheckCircle2 className="w-3 h-3" />已存音频库</span>}
                      </div>
                    </div>

                    <div className="flex items-center gap-1 flex-shrink-0 opacity-0 group-hover/audio:opacity-100 focus-within:opacity-100 transition-opacity">
                      {!audio.savedToLibrary && !uploadState?.savingToLibrary && user && <Button variant="ghost" size="icon" onClick={e => { e.stopPropagation(); void saveToLibrary(audio.id); }} className="w-8 h-8 text-muted-foreground hover:text-[var(--brand-start)]" title="存入音频库"><Upload className="w-3.5 h-3.5" /></Button>}
                      <Button variant="ghost" size="icon" onClick={e => { e.stopPropagation(); handleRemove(audio.id); }} className="w-8 h-8 text-muted-foreground hover:text-red-500" title="移除"><Trash2 className="w-3.5 h-3.5" /></Button>
                    </div>

                    <button onClick={e => { e.stopPropagation(); handleRemove(audio.id); }} className="flex-shrink-0 w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:text-red-500 hover:bg-muted/60 active:bg-muted transition-all lg:hidden">
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {uploadStatus && (
                    <div className="px-1" role="status" aria-live="polite">
                      <div className="mb-1.5 flex items-center justify-between gap-3 text-xs text-[var(--brand-start)]">
                        <span className="flex min-w-0 items-center gap-1.5 whitespace-nowrap">
                          <Spinner size="sm" className="h-3 w-3" />
                          {uploadStatus}
                        </span>
                        {uploadState?.uploading && !uploadState.queued && !uploadState.processing && (
                          <span className="shrink-0 font-mono tabular-nums">{uploadState.uploadProgress}%</span>
                        )}
                      </div>
                      {uploadState?.uploading && !uploadState.processing && !uploadState.queued && (
                        <div className="h-1.5 w-full overflow-hidden rounded-full bg-border/40">
                          <div
                            className="h-full rounded-full bg-[var(--brand-start)] transition-[width] duration-200"
                            style={{ width: `${uploadState.uploadProgress}%` }}
                          />
                        </div>
                      )}
                    </div>
                  )}

                  {audio.duration > 0 && (
                    <div className="space-y-1">
                      <input type="range" min={0} max={audio.duration} value={currentTime} step={0.1} onChange={e => {
                        e.stopPropagation();
                        seekPreview(audio.id, parseFloat(e.target.value));
                      }} onClick={e => e.stopPropagation()} className={cn(
                        "w-full h-1.5 rounded-full appearance-none bg-border/40 cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:transition-transform [&::-webkit-slider-thumb]:hover:scale-125",
                        isPlaying ? "[&::-webkit-slider-thumb]:bg-[var(--brand-start)]" : "[&::-webkit-slider-thumb]:bg-[var(--brand-start)]"
                      )} style={{
                        background: `linear-gradient(to right, var(--brand-start) ${(currentTime / audio.duration) * 100}%, rgba(128,128,128,0.25) ${(currentTime / audio.duration) * 100}%)`,
                      }} />
                      <div className="flex justify-between text-[10px] text-muted-foreground font-mono">
                        <span>{formatDuration(currentTime)}</span>
                        <span>{formatDuration(audio.duration)}</span>
                      </div>
                    </div>
                  )}

                  <AudioItemErrors uploadState={uploadState} />
                </div>

                <PreviewAudio audio={audio} source={audioSource} preview={preview} />
              </div>
            );
          })}
        </div>

        <div className="flex items-center justify-between pt-2 px-1">
          <p className="text-xs text-muted-foreground">共 {audios.length} 个音频{audios.length > 1 && " · 拖动手柄调整顺序"}{allSavedToLibrary && user && " · 已全部存入音频库"}</p>
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
          <span className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
            <VolumeIcon className="w-3.5 h-3.5" />音量控制
          </span>
          <span className="text-sm font-mono font-semibold tabular-nums text-foreground">{volume}%</span>
        </div>
        <div
          role="slider"
          tabIndex={0}
          aria-label="音量控制"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={volume}
          aria-valuetext={`${volume}%`}
          onPointerDown={event => {
            if (!event.isPrimary || (event.pointerType === "mouse" && event.button !== 0)) return;
            event.currentTarget.setPointerCapture(event.pointerId);
            setVolumeFromPointer(event);
          }}
          onPointerMove={event => {
            if (event.currentTarget.hasPointerCapture(event.pointerId)) setVolumeFromPointer(event);
          }}
          onPointerUp={event => {
            if (!event.currentTarget.hasPointerCapture(event.pointerId)) return;
            setVolumeFromPointer(event);
            event.currentTarget.releasePointerCapture(event.pointerId);
          }}
          onPointerCancel={event => {
            if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
          }}
          onKeyDown={handleVolumeKeyDown}
          className="flex h-11 w-full cursor-pointer touch-none items-center rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--brand-start)]/35"
        >
          <div
            aria-hidden="true"
            className="h-2 w-full rounded-full"
            style={{ background: `linear-gradient(to right, var(--brand-start) ${volume}%, rgba(128,128,128,0.2) ${volume}%)` }}
          />
        </div>
      </div>
    </div>
  );

  return (
    <>
      <div className="space-y-3 sm:space-y-6">
        {renderUploadArea()}
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
