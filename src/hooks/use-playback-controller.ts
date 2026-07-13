"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type Dispatch, type SetStateAction } from "react";
import { useAuth } from "@/lib/auth-context";
import { deleteAudioBlob, getAudioBlob, saveAudioBlob } from "@/lib/audio/db";
import { AUDIO_EXTENSIONS, MAX_FILES } from "@/lib/audio";
import type { PlaybackDraft, TaskAudio } from "@/lib/task-types";

export interface UploadState {
  uploading: boolean;
  uploadProgress: number;
  uploadError: string | null;
}

export type CommitBlocker =
  | "no-audio"
  | "uploading"
  | "upload-failed"
  | "unavailable-audio"
  | null;

interface AddFilesResult {
  added: TaskAudio[];
  allAudios: TaskAudio[];
  errors: string[];
}

interface UsePlaybackControllerOptions {
  value: PlaybackDraft;
  onChange: Dispatch<SetStateAction<PlaybackDraft>>;
}

export interface PlaybackController {
  draft: PlaybackDraft;
  getDraft: () => PlaybackDraft;
  setVolume: (volume: number) => void;
  setFade: (patch: Partial<Pick<PlaybackDraft, "enableFade" | "fadeInDuration" | "fadeOutDuration">>) => void;
  moveAudio: (fromIndex: number, toIndex: number) => void;
  assets: {
    uploads: Readonly<Record<string, UploadState>>;
    error: string | null;
    isUploading: boolean;
    allUploaded: boolean;
    commitBlocker: CommitBlocker;
    isAuthenticated: boolean;
    sourceFor: (audio: TaskAudio) => string | undefined;
    addFiles: (files: FileList | File[]) => Promise<AddFilesResult>;
    importByFileKey: (fileKey: string) => Promise<TaskAudio | null>;
    retryUpload: (audioId: string) => Promise<void>;
    uploadAll: () => Promise<void>;
    waitForUploads: () => Promise<number>;
    removeAudio: (audioId: string) => void;
    clearAudios: () => void;
    clearError: () => void;
  };
  preview: {
    playingId: string | null;
    currentTimes: Readonly<Record<string, number>>;
    bindAudioElement: (audioId: string, element: HTMLAudioElement | null) => (() => void) | void;
    toggle: (audioId: string) => void;
    seek: (audioId: string, time: number) => void;
    updateTime: (audioId: string, time: number) => void;
    loadedMetadata: (audioId: string, duration: number) => void;
    ended: (audioId: string) => void;
    stop: () => void;
  };
}

let audioIdCounter = 0;

function mergeAudios(current: TaskAudio[], additions: TaskAudio[]): TaskAudio[] {
  const ids = new Set(current.map(audio => audio.id));
  const fileKeys = new Set(current.map(audio => audio.fileKey).filter(Boolean));
  const names = new Set(current.map(audio => audio.name));
  const merged = [...current];

  for (const audio of additions) {
    if (ids.has(audio.id) || names.has(audio.name) || (audio.fileKey && fileKeys.has(audio.fileKey))) continue;
    merged.push(audio);
    ids.add(audio.id);
    names.add(audio.name);
    if (audio.fileKey) fileKeys.add(audio.fileKey);
    if (merged.length >= MAX_FILES) break;
  }

  return merged;
}

function seekAudioElement(element: HTMLAudioElement, time: number) {
  element.currentTime = time;
}

export function usePlaybackController({ value, onChange }: UsePlaybackControllerOptions): PlaybackController {
  const { user } = useAuth();
  const valueRef = useRef(value);

  const mountedRef = useRef(false);
  const fileRefs = useRef<Record<string, File>>({});
  const objectUrlRefs = useRef<Record<string, string>>({});
  const xhrRefs = useRef<Record<string, XMLHttpRequest>>({});
  const uploadPromisesRef = useRef<Record<string, Promise<void>>>({});
  const uploadStatesRef = useRef<Record<string, UploadState>>({});
  const audioElementsRef = useRef<Record<string, Set<HTMLAudioElement>>>({});
  const importingFileKeysRef = useRef(new Set<string>());

  const [sources, setSources] = useState<Record<string, string>>({});
  const [uploads, setUploads] = useState<Record<string, UploadState>>({});
  const [error, setError] = useState<string | null>(null);
  const [playingId, setPlayingId] = useState<string | null>(null);
  const [currentTimes, setCurrentTimes] = useState<Record<string, number>>({});

  useEffect(() => {
    valueRef.current = value;
  }, [value]);

  const updateDraft = useCallback((update: SetStateAction<PlaybackDraft>) => {
    const current = valueRef.current;
    const next = typeof update === "function" ? update(current) : update;
    valueRef.current = next;
    onChange(next);
    return next;
  }, [onChange]);

  const updateUploadState = useCallback((audioId: string, patch: Partial<UploadState>) => {
    const current = uploadStatesRef.current[audioId];
    const next: UploadState = {
      uploading: patch.uploading ?? current?.uploading ?? false,
      uploadProgress: patch.uploadProgress ?? current?.uploadProgress ?? 0,
      uploadError: patch.uploadError !== undefined ? patch.uploadError : current?.uploadError ?? null,
    };
    uploadStatesRef.current = { ...uploadStatesRef.current, [audioId]: next };
    if (mountedRef.current) setUploads(uploadStatesRef.current);
  }, []);

  const stopPreview = useCallback(() => {
    for (const elements of Object.values(audioElementsRef.current)) {
      for (const element of elements) element.pause();
    }
    setPlayingId(null);
  }, []);

  const cleanupAssetRuntime = useCallback((audioId: string, updateState = true) => {
    xhrRefs.current[audioId]?.abort();
    delete xhrRefs.current[audioId];
    delete uploadPromisesRef.current[audioId];
    delete fileRefs.current[audioId];
    delete uploadStatesRef.current[audioId];

    const url = objectUrlRefs.current[audioId];
    if (url) {
      URL.revokeObjectURL(url);
      delete objectUrlRefs.current[audioId];
    }

    const elements = audioElementsRef.current[audioId];
    if (elements) {
      for (const element of elements) {
        element.pause();
        element.removeAttribute("src");
        element.load();
      }
      delete audioElementsRef.current[audioId];
    }

    if (!updateState || !mountedRef.current) return;
    setSources(previous => {
      const next = { ...previous };
      delete next[audioId];
      return next;
    });
    setUploads({ ...uploadStatesRef.current });
    setCurrentTimes(previous => {
      const next = { ...previous };
      delete next[audioId];
      return next;
    });
    setPlayingId(previous => previous === audioId ? null : previous);
  }, []);

  const releaseLocalSource = useCallback((audioId: string) => {
    const url = objectUrlRefs.current[audioId];
    if (!url) return;
    const elements = audioElementsRef.current[audioId];
    if (elements) {
      for (const element of elements) element.pause();
    }
    URL.revokeObjectURL(url);
    delete objectUrlRefs.current[audioId];
    delete fileRefs.current[audioId];
    if (!mountedRef.current) return;
    setPlayingId(previous => previous === audioId ? null : previous);
    setSources(previous => {
      const next = { ...previous };
      delete next[audioId];
      return next;
    });
  }, []);

  const cleanupAllAssetRuntime = useCallback(() => {
    for (const audioId of new Set([
      ...Object.keys(objectUrlRefs.current),
      ...Object.keys(xhrRefs.current),
      ...Object.keys(audioElementsRef.current),
    ])) {
      cleanupAssetRuntime(audioId, false);
    }
  }, [cleanupAssetRuntime]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      cleanupAllAssetRuntime();
    };
  }, [cleanupAllAssetRuntime]);

  useEffect(() => {
    const activeIds = new Set(value.audios.map(audio => audio.id));
    for (const audioId of new Set([
      ...Object.keys(objectUrlRefs.current),
      ...Object.keys(uploadStatesRef.current),
      ...Object.keys(fileRefs.current),
    ])) {
      if (!activeIds.has(audioId)) cleanupAssetRuntime(audioId);
    }

    for (const audio of value.audios) {
      if ((audio.serverUrl || audio.fileKey) && objectUrlRefs.current[audio.id]) {
        releaseLocalSource(audio.id);
      }
    }
  }, [cleanupAssetRuntime, releaseLocalSource, value.audios]);

  useEffect(() => {
    let cancelled = false;

    for (const audio of value.audios) {
      if (!audio.dbKey || audio.serverUrl || audio.fileKey || objectUrlRefs.current[audio.id]) continue;
      void getAudioBlob(audio.dbKey).then(blob => {
        if (!blob || cancelled || !mountedRef.current || objectUrlRefs.current[audio.id]) return;
        if (!valueRef.current.audios.some(current => current.id === audio.id)) return;
        const url = URL.createObjectURL(blob);
        objectUrlRefs.current[audio.id] = url;
        setSources(previous => ({ ...previous, [audio.id]: url }));
      }).catch(() => {});
    }

    return () => { cancelled = true; };
  }, [value.audios]);

  useEffect(() => {
    for (const elements of Object.values(audioElementsRef.current)) {
      for (const element of elements) element.volume = value.volume / 100;
    }
  }, [value.volume]);

  const setVolume = useCallback((volume: number) => {
    updateDraft(current => ({ ...current, volume: Math.min(100, Math.max(0, volume)) }));
  }, [updateDraft]);

  const getDraft = useCallback(() => valueRef.current, []);

  const setFade = useCallback((patch: Partial<Pick<PlaybackDraft, "enableFade" | "fadeInDuration" | "fadeOutDuration">>) => {
    updateDraft(current => ({ ...current, ...patch }));
  }, [updateDraft]);

  const moveAudio = useCallback((fromIndex: number, toIndex: number) => {
    if (fromIndex === toIndex) return;
    updateDraft(current => {
      if (fromIndex < 0 || toIndex < 0 || fromIndex >= current.audios.length || toIndex >= current.audios.length) return current;
      const audios = [...current.audios];
      const [moved] = audios.splice(fromIndex, 1);
      audios.splice(toIndex, 0, moved);
      return { ...current, audios };
    });
  }, [updateDraft]);

  const patchAudio = useCallback((audioId: string, patch: Partial<TaskAudio>) => {
    updateDraft(current => ({
      ...current,
      audios: current.audios.map(audio => audio.id === audioId ? { ...audio, ...patch } : audio),
    }));
  }, [updateDraft]);

  const performUpload = useCallback(async (audioId: string, file: File) => {
    if (!user || !mountedRef.current) return;
    const activeUpload = uploadPromisesRef.current[audioId];
    if (activeUpload) return activeUpload;
    updateUploadState(audioId, { uploading: true, uploadProgress: 0, uploadError: null });

    const promise = new Promise<void>((resolve) => {
      const xhr = new XMLHttpRequest();
      xhrRefs.current[audioId] = xhr;

      xhr.upload.onprogress = event => {
        if (!event.lengthComputable || !mountedRef.current) return;
        updateUploadState(audioId, { uploadProgress: Math.round((event.loaded / event.total) * 100) });
      };
      xhr.onload = () => {
        if (!mountedRef.current) return resolve();
        try {
          const response = JSON.parse(xhr.responseText);
          if (xhr.status >= 200 && xhr.status < 300 && response.success) {
            patchAudio(audioId, { serverUrl: response.audio_url, fileKey: response.file_key });
            updateUploadState(audioId, { uploading: false, uploadProgress: 100, uploadError: null });
          } else {
            updateUploadState(audioId, { uploading: false, uploadError: response.error || "上传失败" });
          }
        } catch {
          updateUploadState(audioId, { uploading: false, uploadError: "上传失败" });
        }
        resolve();
      };
      xhr.onerror = () => {
        if (mountedRef.current) updateUploadState(audioId, { uploading: false, uploadError: "上传失败" });
        resolve();
      };
      xhr.onabort = () => {
        if (mountedRef.current && valueRef.current.audios.some(audio => audio.id === audioId)) {
          updateUploadState(audioId, { uploading: false, uploadError: "上传已取消" });
        }
        resolve();
      };
      xhr.open("POST", "/api/audio/upload?save_to_files=true");
      const formData = new FormData();
      formData.append("audio", file);
      xhr.send(formData);
    });

    uploadPromisesRef.current[audioId] = promise;
    await promise;
    if (uploadPromisesRef.current[audioId] === promise) delete uploadPromisesRef.current[audioId];
    delete xhrRefs.current[audioId];
  }, [patchAudio, updateUploadState, user]);

  const validateFile = useCallback((file: File, knownNames: Set<string>): string | null => {
    const extension = `.${file.name.split(".").pop()?.toLowerCase()}`;
    if (!AUDIO_EXTENSIONS.includes(extension) && !file.type.startsWith("audio/")) {
      return `不支持的音频格式，请上传 ${AUDIO_EXTENSIONS.join(", ")} 文件`;
    }
    if (knownNames.has(file.name)) return `文件「${file.name}」已存在`;
    return null;
  }, []);

  const addFiles = useCallback(async (files: FileList | File[]): Promise<AddFilesResult> => {
    setError(null);
    const currentAudios = valueRef.current.audios;
    const knownNames = new Set(currentAudios.map(audio => audio.name));
    const available = Math.max(0, MAX_FILES - currentAudios.length);
    const additions: TaskAudio[] = [];
    const errors: string[] = [];

    for (const file of Array.from(files).slice(0, available)) {
      const validationError = validateFile(file, knownNames);
      if (validationError) {
        errors.push(validationError);
        continue;
      }

      knownNames.add(file.name);
      const audioId = `audio-${++audioIdCounter}-${Date.now()}`;
      const url = URL.createObjectURL(file);
      let dbKey: string | undefined;
      try {
        await saveAudioBlob(audioId, file);
        dbKey = audioId;
      } catch {
        URL.revokeObjectURL(url);
        if (!mountedRef.current) {
          return { added: [], allAudios: valueRef.current.audios, errors: [] };
        }
        errors.push(`文件「${file.name}」无法保存到本地，请重试`);
        continue;
      }

      if (!mountedRef.current) {
        URL.revokeObjectURL(url);
        await deleteAudioBlob(audioId).catch(() => {});
        return { added: [], allAudios: valueRef.current.audios, errors: [] };
      }

      fileRefs.current[audioId] = file;
      objectUrlRefs.current[audioId] = url;
      setSources(previous => ({ ...previous, [audioId]: url }));
      additions.push({ id: audioId, name: file.name, size: file.size, duration: 0, dbKey });
    }

    const next = updateDraft(current => ({ ...current, audios: mergeAudios(current.audios, additions) }));
    const acceptedIds = new Set(next.audios.map(audio => audio.id));
    const accepted = additions.filter(audio => acceptedIds.has(audio.id));
    for (const audio of additions) {
      if (!acceptedIds.has(audio.id)) {
        cleanupAssetRuntime(audio.id);
        if (audio.dbKey) void deleteAudioBlob(audio.dbKey).catch(() => {});
      }
    }

    for (const audio of accepted) {
      const file = fileRefs.current[audio.id];
      if (user && file) void performUpload(audio.id, file);
    }

    if (errors.length > 0) setError(errors[errors.length - 1]);
    return { added: accepted, allAudios: next.audios, errors };
  }, [cleanupAssetRuntime, performUpload, updateDraft, user, validateFile]);

  const importByFileKey = useCallback(async (fileKey: string): Promise<TaskAudio | null> => {
    if (
      !user
      || importingFileKeysRef.current.has(fileKey)
      || valueRef.current.audios.some(audio => audio.fileKey === fileKey)
    ) return null;
    importingFileKeysRef.current.add(fileKey);
    try {
      const response = await fetch(`/api/audio/get-by-key?fileKey=${encodeURIComponent(fileKey)}`);
      const data = await response.json();
      if (!mountedRef.current) return null;
      if (!data.success || !data.audio) {
        setError(data.error || "导入音频失败");
        return null;
      }
      const audio: TaskAudio = {
        id: `imported-${Date.now()}-${Math.random().toString(36).slice(2)}`,
        name: data.audio.name,
        duration: data.audio.metadata?.duration ?? 0,
        size: data.audio.size ?? 0,
        fileKey,
        serverUrl: data.audio.serverUrl,
      };
      const next = updateDraft(current => ({ ...current, audios: mergeAudios(current.audios, [audio]) }));
      return next.audios.some(current => current.id === audio.id) ? audio : null;
    } catch {
      if (mountedRef.current) setError("导入音频失败");
      return null;
    } finally {
      importingFileKeysRef.current.delete(fileKey);
    }
  }, [updateDraft, user]);

  const retryUpload = useCallback(async (audioId: string) => {
    const audio = valueRef.current.audios.find(item => item.id === audioId);
    if (!audio || !user) return;
    let file = fileRefs.current[audioId];
    if (!file && audio.dbKey) {
      const blob = await getAudioBlob(audio.dbKey).catch(() => null);
      if (blob) file = new File([blob], audio.name, { type: blob.type || "audio/mpeg" });
    }
    if (!file) {
      updateUploadState(audioId, { uploading: false, uploadError: "找不到本地音频，请重新选择文件" });
      return;
    }
    await performUpload(audioId, file);
  }, [performUpload, updateUploadState, user]);

  const uploadAll = useCallback(async () => {
    const pending = valueRef.current.audios.filter(audio => !audio.serverUrl && !audio.fileKey && !uploadStatesRef.current[audio.id]?.uploading);
    for (const audio of pending) await retryUpload(audio.id);
  }, [retryUpload]);

  const waitForUploads = useCallback(async () => {
    await Promise.allSettled(Object.values(uploadPromisesRef.current));
    return Object.values(uploadStatesRef.current).filter(upload => upload.uploadError).length;
  }, []);

  const clearError = useCallback(() => setError(null), []);

  const removeAudio = useCallback((audioId: string) => {
    updateDraft(current => ({ ...current, audios: current.audios.filter(audio => audio.id !== audioId) }));
    cleanupAssetRuntime(audioId);
  }, [cleanupAssetRuntime, updateDraft]);

  const clearAudios = useCallback(() => {
    stopPreview();
    const audioIds = valueRef.current.audios.map(audio => audio.id);
    updateDraft(current => ({ ...current, audios: [] }));
    for (const audioId of audioIds) cleanupAssetRuntime(audioId);
  }, [cleanupAssetRuntime, stopPreview, updateDraft]);

  const sourceFor = useCallback((audio: TaskAudio) => (
    audio.serverUrl
      || (audio.fileKey ? `/api/audio/proxy?key=${encodeURIComponent(audio.fileKey)}` : undefined)
      || sources[audio.id]
  ), [sources]);

  const bindAudioElement = useCallback((audioId: string, element: HTMLAudioElement | null) => {
    if (!element) return;
    const elements = audioElementsRef.current[audioId] ?? new Set<HTMLAudioElement>();
    elements.add(element);
    audioElementsRef.current[audioId] = elements;
    element.volume = valueRef.current.volume / 100;
    return () => {
      element.pause();
      element.removeAttribute("src");
      element.load();
      elements.delete(element);
      if (elements.size === 0) {
        delete audioElementsRef.current[audioId];
        if (mountedRef.current) {
          setCurrentTimes(previous => {
            const next = { ...previous };
            delete next[audioId];
            return next;
          });
        }
      }
      if (mountedRef.current) {
        setPlayingId(previous => previous === audioId ? null : previous);
      }
    };
  }, []);

  const togglePreview = useCallback((audioId: string) => {
    const targetElements = audioElementsRef.current[audioId];
    const target = targetElements ? Array.from(targetElements).at(-1) : undefined;
    if (!target) return;

    for (const elements of Object.values(audioElementsRef.current)) {
      for (const element of elements) {
        if (element !== target && !element.paused) element.pause();
      }
    }

    if (playingId === audioId && !target.paused) {
      target.pause();
      setPlayingId(null);
      return;
    }

    target.currentTime = 0;
    target.volume = valueRef.current.volume / 100;
    void target.play().then(() => setPlayingId(audioId)).catch(() => setPlayingId(null));
  }, [playingId]);

  const seekPreview = useCallback((audioId: string, time: number) => {
    const elements = audioElementsRef.current[audioId];
    if (!elements) return;
    for (const element of elements) seekAudioElement(element, time);
    setCurrentTimes(previous => ({ ...previous, [audioId]: time }));
  }, []);

  const updatePreviewTime = useCallback((audioId: string, time: number) => {
    setCurrentTimes(previous => ({ ...previous, [audioId]: time }));
  }, []);

  const loadedMetadata = useCallback((audioId: string, duration: number) => {
    if (!Number.isFinite(duration) || duration <= 0) return;
    patchAudio(audioId, { duration });
  }, [patchAudio]);

  const ended = useCallback((audioId: string) => {
    setPlayingId(previous => previous === audioId ? null : previous);
    setCurrentTimes(previous => ({ ...previous, [audioId]: 0 }));
  }, []);

  const commitBlocker = useMemo<CommitBlocker>(() => {
    if (value.audios.length === 0) return "no-audio";
    if (value.audios.some(audio => !audio.fileKey && !audio.serverUrl && !audio.dbKey)) return "unavailable-audio";
    if (user && Object.values(uploads).some(upload => upload.uploading)) return "uploading";
    if (user && Object.values(uploads).some(upload => upload.uploadError)) return "upload-failed";
    if (user && value.audios.some(audio => !audio.fileKey && !audio.serverUrl)) return "unavailable-audio";
    return null;
  }, [uploads, user, value.audios]);

  return {
    draft: value,
    getDraft,
    setVolume,
    setFade,
    moveAudio,
    assets: {
      uploads,
      error,
      isUploading: Object.values(uploads).some(upload => upload.uploading),
      allUploaded: value.audios.length > 0 && value.audios.every(audio => audio.serverUrl || audio.fileKey),
      commitBlocker,
      isAuthenticated: !!user,
      sourceFor,
      addFiles,
      importByFileKey,
      retryUpload,
      uploadAll,
      waitForUploads,
      removeAudio,
      clearAudios,
      clearError,
    },
    preview: {
      playingId,
      currentTimes,
      bindAudioElement,
      toggle: togglePreview,
      seek: seekPreview,
      updateTime: updatePreviewTime,
      loadedMetadata,
      ended,
      stop: stopPreview,
    },
  };
}
