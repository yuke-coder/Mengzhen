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
  AlertCircle,
  RefreshCw,
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
    retryUpload,
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
        "relative block min-h-32 p-4 sm:p-6 transition-all duration-200 cursor-pointer rounded-xl bg-muted/25 dark:bg-white/[0.025] select-none touch-manipulation active:bg-muted/45",
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
      <div className="flex min-h-24 flex-col items-center justify-center gap-2 sm:gap-3">
        <div className={cn("p-2.5 sm:p-3 rounded-full bg-[var(--brand-glow)]/10 transition-transform duration-300", dragOver && !disabled && "scale-110")}>
          <Upload className="w-5 h-5 sm:w-6 sm:h-6 text-[var(--brand-glow)]" />
        </div>
        <div className="text-center">
          <p className="text-sm font-medium text-foreground leading-relaxed px-2">
            {disabled ? (
              "请先登录"
            ) : dragOver ? (
              "松开以添加"
            ) : (
              <>
                <span className="sm:hidden">点击添加音频文件</span>
                <span className="hidden sm:inline">点击或拖拽添加音频文件</span>
              </>
            )}
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
        <div className="flex flex-col gap-2 px-1 sm:flex-row sm:items-center sm:justify-between">
          <h3 className="flex min-w-0 items-center gap-2 text-sm font-medium text-foreground">
            <Music2 className="w-4 h-4 text-[var(--brand-start)]" />
            <span>音频列表</span>
            <span className="rounded-md bg-muted/70 px-1.5 py-0.5 text-[11px] tabular-nums text-muted-foreground">
              {audios.length}
            </span>
            {audios.length > 1 && (
              <span className="hidden text-xs font-normal text-muted-foreground sm:inline">拖动手柄调整顺序</span>
            )}
          </h3>
          {!allSavedToLibrary && user && (
            <Button
              type="button"
              size="sm"
              variant="secondary"
              onClick={() => void saveAllToLibrary()}
              disabled={anySavingToLibrary}
              className="h-11 w-full justify-center rounded-lg text-sm sm:h-8 sm:w-auto sm:text-xs"
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
            const itemError = uploadState?.uploadError || uploadState?.libraryError;

            return (
              <div
                key={audio.id}
                data-audio-item={audio.id}
                ref={el => { itemRefs.current[audio.id] = el; }}
                onClick={() => togglePlay(audio.id)}
                className={cn(
                  "group/audio relative cursor-pointer select-none rounded-xl bg-muted/20 transition-[background-color,opacity,transform] duration-200 active:bg-muted/45 dark:bg-white/[0.025]",
                  isDraggingThis && "scale-[1.02] opacity-80 z-10",
                  disabled && "opacity-50 pointer-events-none"
                )}
                style={{
                  ...(isPlaying && isDraggingThis && {
                    background: "radial-gradient(ellipse at 20% 50%, color-mix(in srgb, var(--brand-start)) 12%, transparent) 40%, color-mix(in srgb, var(--brand-end)) 6% transparent 70%, transparent 100%"
                  }),
                }}
              >
                <div className="space-y-2.5 p-2 sm:p-3">
                  <div className="flex items-center gap-2 sm:gap-3">
                    {audios.length > 1 && (
                      <button
                        type="button"
                        aria-label={`调整「${audio.name}」的播放顺序`}
                        onClick={event => event.stopPropagation()}
                        onPointerDown={event => handlePointerDown(event, audio.id)}
                        onKeyDown={event => handleReorderKeyDown(event, index)}
                        disabled={disabled}
                        className="flex size-11 flex-shrink-0 cursor-grab touch-none items-center justify-center rounded-lg text-muted-foreground/60 active:cursor-grabbing active:bg-muted disabled:cursor-not-allowed sm:size-8"
                      >
                        <GripVertical className="w-4 h-4 transition-colors group-hover/audio:text-muted-foreground" />
                      </button>
                    )}

                    <button
                      type="button"
                      aria-label={`${isPlaying ? "暂停" : "试听"}「${audio.name}」`}
                      onClick={e => { e.stopPropagation(); togglePlay(audio.id); }}
                      className={cn(
                      "flex size-11 flex-shrink-0 items-center justify-center rounded-lg bg-muted/80 transition-[background-color,color,transform] active:scale-95 sm:size-8",
                      isPlaying && "bg-[var(--brand-fill-active)] text-[var(--brand-start)]",
                      !isPlaying && "text-foreground hover:text-[var(--brand-start)]"
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
                      <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-muted-foreground">
                        <span className="flex shrink-0 items-center gap-1"><VolumeIcon className="w-3 h-3" />{formatFileSize(audio.size)}</span>
                        {audio.duration > 0 && <span className="flex shrink-0 items-center gap-1"><Clock className="w-3 h-3" />{formatDuration(audio.duration)}</span>}
                        {audio.savedToLibrary && <span className="flex shrink-0 items-center gap-1 text-emerald-500"><CheckCircle2 className="w-3 h-3" />已存音频库</span>}
                      </div>
                    </div>

                    <div className="hidden items-center gap-1 flex-shrink-0 opacity-0 group-hover/audio:opacity-100 focus-within:opacity-100 transition-opacity lg:flex">
                      {!audio.savedToLibrary && !uploadState?.savingToLibrary && user && <Button variant="ghost" size="icon" onClick={e => { e.stopPropagation(); void saveToLibrary(audio.id); }} className="w-8 h-8 text-muted-foreground hover:text-[var(--brand-start)]" title="存入音频库"><Upload className="w-3.5 h-3.5" /></Button>}
                      <Button variant="ghost" size="icon" onClick={e => { e.stopPropagation(); handleRemove(audio.id); }} className="w-8 h-8 text-muted-foreground hover:text-red-500" title="移除"><Trash2 className="w-3.5 h-3.5" /></Button>
                    </div>

                    <button
                      type="button"
                      aria-label={`移除「${audio.name}」`}
                      onClick={e => { e.stopPropagation(); handleRemove(audio.id); }}
                      className="flex size-11 flex-shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:text-red-500 active:bg-red-500/10 lg:hidden"
                    >
                      <Trash2 className="w-4 h-4" />
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

                  {itemError && (
                    <div
                      className="flex items-center gap-2 rounded-lg bg-red-500/10 py-1.5 pl-3 pr-1 text-xs text-red-600 dark:text-red-300"
                      role="alert"
                    >
                      <AlertCircle className="h-4 w-4 shrink-0" />
                      <span className="min-w-0 flex-1 text-pretty leading-relaxed">{itemError}</span>
                      <button
                        type="button"
                        onClick={event => {
                          event.stopPropagation();
                          if (uploadState?.uploadError) void retryUpload(audio.id);
                          else void saveToLibrary(audio.id);
                        }}
                        className="flex min-h-11 shrink-0 items-center gap-1 rounded-md px-2 font-medium text-red-700 transition-colors active:bg-red-500/10 dark:text-red-200"
                      >
                        <RefreshCw className="h-3.5 w-3.5" />
                        重试
                      </button>
                    </div>
                  )}

                  {audio.duration > 0 && (
                    <div className="space-y-1">
                      <input type="range" min={0} max={audio.duration} value={currentTime} step={0.1} onChange={e => {
                        e.stopPropagation();
                        seekPreview(audio.id, parseFloat(e.target.value));
                      }} onClick={e => e.stopPropagation()} className={cn(
                        "h-11 w-full cursor-pointer appearance-none rounded-full bg-transparent bg-[length:100%_6px] bg-center bg-no-repeat [&::-webkit-slider-thumb]:h-[18px] [&::-webkit-slider-thumb]:w-[18px] [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:rounded-full sm:h-8 sm:[&::-webkit-slider-thumb]:h-3 sm:[&::-webkit-slider-thumb]:w-3",
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

                </div>

                <PreviewAudio audio={audio} source={audioSource} preview={preview} />
              </div>
            );
          })}
        </div>

        <div className="flex items-center justify-between px-1 pt-1">
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
          <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
            <VolumeIcon className="w-3.5 h-3.5" />音量控制
          </label>
          <span className="text-sm font-mono font-semibold tabular-nums text-foreground">{volume}%</span>
        </div>
        <div className="relative pt-1 pb-1">
          <input
            type="range"
            aria-label="音量控制"
            min={0} max={100}
            value={volume} step={1}
            onInput={e => setVolume(parseInt((e.target as HTMLInputElement).value, 10))}
            className="h-11 w-full cursor-pointer appearance-none rounded-full bg-transparent bg-[length:100%_10px] bg-center bg-no-repeat [&::-webkit-slider-thumb]:h-6 [&::-webkit-slider-thumb]:w-6 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:shadow-md [&::-webkit-slider-thumb]:ring-2 [&::-webkit-slider-thumb]:ring-[var(--brand-start)] sm:h-8 sm:[&::-webkit-slider-thumb]:h-5 sm:[&::-webkit-slider-thumb]:w-5"
            style={{
              background: `linear-gradient(to right, var(--brand-start) ${volume}%, rgba(128,128,128,0.2) ${volume}%)`,
            }}
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
          <div role="dialog" aria-modal="true" aria-labelledby="clear-confirm-title" aria-describedby="clear-confirm-desc" className="w-[calc(100%-2rem)] max-w-sm space-y-4 rounded-xl bg-background p-6 shadow-xl" onClick={e => e.stopPropagation()}>
            <div className="flex flex-col items-center gap-3 text-center">
              <div className="w-12 h-12 rounded-full bg-red-500/10 flex items-center justify-center"><Trash2 className="w-6 h-6 text-red-500" /></div>
              <div><h3 id="clear-confirm-title" className="text-base font-semibold text-foreground">确认清空当前音频列表？</h3><p id="clear-confirm-desc" className="text-sm text-muted-foreground mt-1.5">只会移除当前配置中的引用，已经保存的任务不会受到影响。</p></div>
            </div>
            <div className="flex gap-3">
              <button onClick={() => setShowClearConfirm(false)} className="h-11 flex-1 cursor-pointer rounded-lg bg-muted text-sm font-medium text-muted-foreground transition-colors hover:text-foreground active:bg-muted/80">取消</button>
              <button onClick={handleClearAll} className="h-11 flex-1 cursor-pointer rounded-lg bg-red-500 text-sm font-bold text-white transition-colors hover:bg-red-500/90 active:bg-red-500/80">确认清空</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
