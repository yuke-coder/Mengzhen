"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type Dispatch, type SetStateAction } from "react";
import { Upload as TusUpload } from "tus-js-client";
import { useAuth } from "@/lib/auth-context";
import { deleteAudioBlob, getAudioBlob, saveAudioBlob } from "@/lib/audio/db";
import { AUDIO_EXTENSIONS } from "@/lib/audio";
import type { PlaybackDraft, TaskAudio } from "@/lib/task-types";

export interface UploadState {
  uploading: boolean;
  queued: boolean;
  uploadProgress: number;
  processing: boolean;
  uploadError: string | null;
  savingToLibrary: boolean;
  libraryError: string | null;
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

interface UploadTicket {
  fileKey: string;
  uploadToken: string;
  tusEndpoint: string;
  bucket: string;
}

interface UploadTicketResponse extends UploadTicket {
  success: true;
}

interface UploadCompleteResponse {
  success: true;
  audio_url: string;
  file_key: string;
}

interface UploadAbortHandle {
  abort: () => Promise<void>;
}

interface QueuedUpload {
  audioId: string;
  file: File;
  resolve: () => void;
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
    isSavingToLibrary: boolean;
    allSavedToLibrary: boolean;
    commitBlocker: CommitBlocker;
    isAuthenticated: boolean;
    sourceFor: (audio: TaskAudio) => string | undefined;
    addFiles: (files: FileList | File[]) => Promise<AddFilesResult>;
    importByFileKey: (fileKey: string) => Promise<TaskAudio | null>;
    saveToLibrary: (audioId: string) => Promise<void>;
    saveAllToLibrary: () => Promise<void>;
    retryUpload: (audioId: string) => Promise<void>;
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
const TUS_CHUNK_SIZE = 6 * 1024 * 1024;
const TUS_RESUME_WINDOW_MS = 23 * 60 * 60 * 1000;

interface NetworkInformationLike {
  effectiveType?: "slow-2g" | "2g" | "3g" | "4g";
  saveData?: boolean;
}

function getMaxParallelUploads(): number {
  const browserNavigator = navigator as Navigator & {
    connection?: NetworkInformationLike;
    mozConnection?: NetworkInformationLike;
    webkitConnection?: NetworkInformationLike;
  };
  const connection = browserNavigator.connection
    ?? browserNavigator.mozConnection
    ?? browserNavigator.webkitConnection;

  if (connection?.saveData || connection?.effectiveType === "slow-2g" || connection?.effectiveType === "2g") return 1;
  if (connection?.effectiveType === "3g") return 2;
  return 3;
}

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}

function translateUploadError(error: unknown): string {
  const message = errorMessage(error, "音频上传失败，请稍后重试");
  const normalized = message.toLowerCase();
  if (normalized.includes("maximum allowed single file size") || normalized.includes("file size limit") || normalized.includes("payload too large")) {
    return "音频文件过大，超过存储空间允许的单文件大小";
  }
  if (normalized.includes("invalid content type") || normalized.includes("mime") || normalized.includes("content-type")) {
    return "不支持的音频格式，请选择 MP3 / WAV / OGG / M4A / FLAC 等格式";
  }
  if (normalized.includes("network") || normalized.includes("failed to fetch") || normalized.includes("timeout")) {
    return "网络连接异常，请检查网络后重试";
  }
  if (
    normalized.includes("accessdenied")
    || normalized.includes("unauthorized")
    || normalized.includes("invalid compact jws")
    || normalized.includes("statuscode\":\"403")
    || normalized.includes("response code: 403")
  ) {
    return "上传凭证已失效，请点击重试";
  }
  if (
    normalized.startsWith("tus:")
    || normalized.includes("/storage/v1/upload/resumable")
    || normalized.includes("unexpected response while")
  ) {
    return "音频上传失败，请点击重试";
  }
  return message.length > 120 ? "音频上传失败，请稍后重试" : message;
}

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
  const uploadAbortRefs = useRef<Record<string, UploadAbortHandle>>({});
  const uploadTicketsRef = useRef<Record<string, UploadTicket>>({});
  const completedDirectUploadsRef = useRef(new Set<string>());
  const registeringUploadsRef = useRef(new Set<string>());
  const registeredUploadsRef = useRef(new Set<string>());
  const uploadPromisesRef = useRef<Record<string, Promise<void>>>({});
  const queuedUploadsRef = useRef<QueuedUpload[]>([]);
  const activeUploadCountRef = useRef(0);
  const drainUploadQueueRef = useRef<() => void>(() => {});
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
      queued: patch.queued ?? current?.queued ?? false,
      uploadProgress: patch.uploadProgress ?? current?.uploadProgress ?? 0,
      processing: patch.processing ?? current?.processing ?? false,
      uploadError: patch.uploadError !== undefined ? patch.uploadError : current?.uploadError ?? null,
      savingToLibrary: patch.savingToLibrary ?? current?.savingToLibrary ?? false,
      libraryError: patch.libraryError !== undefined ? patch.libraryError : current?.libraryError ?? null,
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
    const retainedQueue: QueuedUpload[] = [];
    for (const queued of queuedUploadsRef.current) {
      if (queued.audioId === audioId) queued.resolve();
      else retainedQueue.push(queued);
    }
    queuedUploadsRef.current = retainedQueue;
    void uploadAbortRefs.current[audioId]?.abort().catch(() => {});
    delete uploadAbortRefs.current[audioId];
    delete uploadTicketsRef.current[audioId];
    completedDirectUploadsRef.current.delete(audioId);
    registeringUploadsRef.current.delete(audioId);
    registeredUploadsRef.current.delete(audioId);
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
      ...Object.keys(uploadAbortRefs.current),
      ...queuedUploadsRef.current.map(upload => upload.audioId),
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
        fileRefs.current[audio.id] = new File([blob], audio.name, { type: blob.type || "audio/mpeg" });
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

  const requestUploadTicket = useCallback(async (file: File, resumeFileKey?: string): Promise<UploadTicket> => {
    const response = await fetch("/api/audio/upload-ticket", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fileName: file.name, fileSize: file.size, mimeType: file.type, resumeFileKey }),
    });
    const payload = await response.json().catch(() => null) as Partial<UploadTicketResponse> & { error?: string } | null;
    if (!response.ok || !payload?.success || !payload.fileKey || !payload.uploadToken || !payload.tusEndpoint || !payload.bucket) {
      throw new Error(payload?.error || "无法准备音频上传，请稍后重试");
    }
    return {
      fileKey: payload.fileKey,
      uploadToken: payload.uploadToken,
      tusEndpoint: payload.tusEndpoint,
      bucket: payload.bucket,
    };
  }, []);

  const completeUpload = useCallback(async (
    fileKey: string,
    file: File,
    allowMissing = false,
  ): Promise<UploadCompleteResponse | null> => {
    const response = await fetch("/api/audio/upload-complete", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fileKey, fileName: file.name, mimeType: file.type }),
    });
    if (allowMissing && response.status === 404) return null;
    const payload = await response.json().catch(() => null) as Partial<UploadCompleteResponse> & { error?: string } | null;
    if (!response.ok || !payload?.success || !payload.audio_url || !payload.file_key) {
      throw new Error(payload?.error || "音频登记失败，请重试");
    }
    return { success: true, audio_url: payload.audio_url, file_key: payload.file_key };
  }, []);

  const uploadDirectly = useCallback((audioId: string, file: File, ticket: UploadTicket): Promise<void> => (
    new Promise<void>((resolve, reject) => {
      let settled = false;
      const resolveOnce = () => {
        if (settled) return;
        settled = true;
        resolve();
      };
      const rejectOnce = (error: Error) => {
        if (settled) return;
        settled = true;
        reject(error);
      };
      const upload = new TusUpload(file, {
        endpoint: ticket.tusEndpoint,
        chunkSize: TUS_CHUNK_SIZE,
        retryDelays: [0, 1_000, 3_000, 5_000, 10_000],
        uploadDataDuringCreation: true,
        removeFingerprintOnSuccess: true,
        // 同一份本地文件必须只恢复到它自己的目标对象，不能按文件名串到另一份上传。
        fingerprint: () => Promise.resolve([
          "dream-pillow-tus",
          ticket.fileKey,
          file.name,
          file.type,
          file.size,
        ].join(":")),
        headers: {
          "x-signature": ticket.uploadToken,
          "x-upsert": "false",
        },
        metadata: {
          bucketName: ticket.bucket,
          objectName: ticket.fileKey,
          contentType: file.type || "audio/mpeg",
          cacheControl: "3600",
        },
        onProgress: (uploaded, total) => {
          if (!mountedRef.current || total <= 0) return;
          updateUploadState(audioId, {
            queued: false,
            uploadProgress: Math.min(100, Math.round((uploaded / total) * 100)),
            processing: false,
          });
        },
        onError: error => rejectOnce(error),
        onSuccess: resolveOnce,
      });

      uploadAbortRefs.current[audioId] = {
        abort: async () => {
          await upload.abort();
          rejectOnce(new Error("上传已取消"));
        },
      };

      void upload.findPreviousUploads()
        .then(previousUploads => {
          const minCreationTime = Date.now() - TUS_RESUME_WINDOW_MS;
          const previousUpload = previousUploads.find(item => (
            Number.isFinite(Date.parse(item.creationTime))
            && Date.parse(item.creationTime) >= minCreationTime
          ));
          if (previousUpload) upload.resumeFromPreviousUpload(previousUpload);
          upload.start();
        })
        .catch(error => rejectOnce(error instanceof Error ? error : new Error("无法继续上传")));
    })
  ), [updateUploadState]);

  const executeUpload = useCallback(async (audioId: string, file: File) => {
    if (!user || !mountedRef.current) return;
    const currentAudio = valueRef.current.audios.find(audio => audio.id === audioId);
    if (currentAudio?.fileKey || currentAudio?.serverUrl || registeredUploadsRef.current.has(audioId)) return;
    const isActiveAudio = () => (
      mountedRef.current && valueRef.current.audios.some(audio => audio.id === audioId)
    );
    if (registeringUploadsRef.current.has(audioId)) return;
    registeringUploadsRef.current.add(audioId);

    try {
      updateUploadState(audioId, { uploading: true, queued: false, uploadError: null });
      let ticket = uploadTicketsRef.current[audioId];
      if (!ticket) {
        const audio = valueRef.current.audios.find(item => item.id === audioId);
        const pendingUploadKey = audio?.pendingUploadKey;

        if (pendingUploadKey) {
          const completed = await completeUpload(pendingUploadKey, file, true);
          if (completed) {
            if (!isActiveAudio()) return;
            patchAudio(audioId, {
              serverUrl: completed.audio_url,
              fileKey: completed.file_key,
              pendingUploadKey: undefined,
            });
            registeredUploadsRef.current.add(audioId);
            updateUploadState(audioId, {
              uploading: false,
              queued: false,
              uploadProgress: 100,
              processing: false,
              uploadError: null,
            });
            return;
          }
        }

        ticket = await requestUploadTicket(file, pendingUploadKey);
        if (!isActiveAudio()) return;
        uploadTicketsRef.current[audioId] = ticket;
        if (!pendingUploadKey) patchAudio(audioId, { pendingUploadKey: ticket.fileKey });
      }

      if (!completedDirectUploadsRef.current.has(audioId)) {
        await uploadDirectly(audioId, file, ticket);
        if (!isActiveAudio()) return;
        completedDirectUploadsRef.current.add(audioId);
      }

      if (!isActiveAudio()) return;
      updateUploadState(audioId, { queued: false, uploadProgress: 100, processing: true });
      const completed = await completeUpload(ticket.fileKey, file);
      if (!completed) throw new Error("音频登记失败，请重试");
      if (!isActiveAudio()) return;
      patchAudio(audioId, {
        serverUrl: completed.audio_url,
        fileKey: completed.file_key,
        pendingUploadKey: undefined,
      });
      delete uploadTicketsRef.current[audioId];
      completedDirectUploadsRef.current.delete(audioId);
      registeredUploadsRef.current.add(audioId);
      updateUploadState(audioId, {
        uploading: false,
        queued: false,
        uploadProgress: 100,
        processing: false,
        uploadError: null,
      });
    } catch (uploadError) {
      registeringUploadsRef.current.delete(audioId);
      // TUS 凭证可能已过期或被存储服务拒绝；重试时必须重新签发。
      delete uploadTicketsRef.current[audioId];
      if (isActiveAudio()) {
        updateUploadState(audioId, {
          uploading: false,
          queued: false,
          processing: false,
          uploadError: translateUploadError(uploadError),
        });
      }
    } finally {
      delete uploadAbortRefs.current[audioId];
      if (!registeredUploadsRef.current.has(audioId)) registeringUploadsRef.current.delete(audioId);
    }
  }, [completeUpload, patchAudio, requestUploadTicket, updateUploadState, uploadDirectly, user]);

  const drainUploadQueue = useCallback(() => {
    const maxParallelUploads = getMaxParallelUploads();
    while (activeUploadCountRef.current < maxParallelUploads && queuedUploadsRef.current.length > 0) {
      const queued = queuedUploadsRef.current.shift();
      if (!queued) return;
      if (!mountedRef.current || !valueRef.current.audios.some(audio => audio.id === queued.audioId)) {
        queued.resolve();
        continue;
      }

      activeUploadCountRef.current += 1;
      void executeUpload(queued.audioId, queued.file).finally(() => {
        activeUploadCountRef.current -= 1;
        queued.resolve();
        drainUploadQueueRef.current();
      });
    }
  }, [executeUpload]);

  useEffect(() => {
    drainUploadQueueRef.current = drainUploadQueue;
    return () => { drainUploadQueueRef.current = () => {}; };
  }, [drainUploadQueue]);

  const performUpload = useCallback(async (audioId: string, file: File) => {
    if (!user || !mountedRef.current) return;
    const currentAudio = valueRef.current.audios.find(audio => audio.id === audioId);
    if (currentAudio?.fileKey || currentAudio?.serverUrl || registeredUploadsRef.current.has(audioId)) return;
    const activeUpload = uploadPromisesRef.current[audioId];
    if (activeUpload) return activeUpload;

    updateUploadState(audioId, {
      uploading: true,
      queued: true,
      uploadProgress: completedDirectUploadsRef.current.has(audioId) ? 100 : 0,
      processing: completedDirectUploadsRef.current.has(audioId),
      uploadError: null,
    });

    let resolveQueuedUpload!: () => void;
    const promise = new Promise<void>(resolve => { resolveQueuedUpload = resolve; });
    uploadPromisesRef.current[audioId] = promise;
    queuedUploadsRef.current.push({ audioId, file, resolve: resolveQueuedUpload });
    drainUploadQueue();

    await promise;
    if (uploadPromisesRef.current[audioId] === promise) delete uploadPromisesRef.current[audioId];
  }, [drainUploadQueue, updateUploadState, user]);

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
    const additions: TaskAudio[] = [];
    const errors: string[] = [];

    for (const file of Array.from(files)) {
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
        savedToLibrary: data.audio.savedToLibrary === true,
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
    if (!audio || !user || audio.fileKey || audio.serverUrl) return;
    let file = fileRefs.current[audioId];
    if (!file && audio.dbKey) {
      const blob = await getAudioBlob(audio.dbKey).catch(() => null);
      if (blob) {
        file = new File([blob], audio.name, { type: blob.type || "audio/mpeg" });
        fileRefs.current[audioId] = file;
      }
    }
    if (!file) {
      updateUploadState(audioId, {
        uploading: false,
        processing: false,
        uploadError: "找不到本地音频，请重新选择文件",
      });
      return;
    }
    await performUpload(audioId, file);
  }, [performUpload, updateUploadState, user]);

  useEffect(() => {
    if (!user) return;
    for (const audio of value.audios) {
      const currentAudio = valueRef.current.audios.find(item => item.id === audio.id);
      const uploadState = uploadStatesRef.current[audio.id];
      if (
        audio.pendingUploadKey
        && currentAudio?.pendingUploadKey === audio.pendingUploadKey
        && !currentAudio.fileKey
        && !currentAudio.serverUrl
        && !uploadPromisesRef.current[audio.id]
        && !uploadState?.uploading
      ) {
        void retryUpload(audio.id);
      }
    }
  }, [retryUpload, user, value.audios]);

  const saveToLibrary = useCallback(async (audioId: string) => {
    let audio = valueRef.current.audios.find(item => item.id === audioId);
    if (!audio || !user || audio.savedToLibrary) return;

    if (!audio.fileKey) {
      await retryUpload(audioId);
      audio = valueRef.current.audios.find(item => item.id === audioId);
    }
    if (!audio?.fileKey) return;

    updateUploadState(audioId, { savingToLibrary: true, libraryError: null });
    try {
      const response = await fetch("/api/audio/save-to-library", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fileKey: audio.fileKey }),
      });
      const data = await response.json();
      if (!response.ok || !data.success) throw new Error(data.error || "存入音频库失败");
      patchAudio(audioId, { savedToLibrary: true });
      updateUploadState(audioId, { savingToLibrary: false, libraryError: null });
    } catch (saveError) {
      updateUploadState(audioId, {
        savingToLibrary: false,
        libraryError: saveError instanceof Error ? saveError.message : "存入音频库失败",
      });
    }
  }, [patchAudio, retryUpload, updateUploadState, user]);

  const saveAllToLibrary = useCallback(async () => {
    const pending = valueRef.current.audios.filter(audio => !audio.savedToLibrary);
    for (const audio of pending) await saveToLibrary(audio.id);
  }, [saveToLibrary]);

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
      isSavingToLibrary: Object.values(uploads).some(upload => upload.savingToLibrary),
      allSavedToLibrary: value.audios.length > 0 && value.audios.every(audio => audio.savedToLibrary),
      commitBlocker,
      isAuthenticated: !!user,
      sourceFor,
      addFiles,
      importByFileKey,
      saveToLibrary,
      saveAllToLibrary,
      retryUpload,
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
