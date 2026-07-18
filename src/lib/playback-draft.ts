import type { PlaybackDraft, ScheduledTask, TaskAudio } from './task-types';

const DEFAULT_PLAYBACK_KEY = 'dream_default_play_config';
const LEGACY_PLAYBACK_KEY = 'dream_config';

export const EMPTY_PLAYBACK_DRAFT: PlaybackDraft = {
  audios: [],
  volume: 50,
  fadeInDuration: 60,
  fadeOutDuration: 60,
  enableFade: true,
};

function clamp(value: unknown, fallback: number, min: number, max: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) return fallback;
  return Math.min(max, Math.max(min, value));
}

function normalizeAudio(value: unknown): TaskAudio | null {
  if (!value || typeof value !== 'object') return null;
  const audio = value as Partial<TaskAudio>;
  if (typeof audio.id !== 'string' || typeof audio.name !== 'string' || !audio.name.trim()) {
    return null;
  }

  return {
    id: audio.id,
    name: audio.name,
    duration: clamp(audio.duration, 0, 0, Number.MAX_SAFE_INTEGER),
    size: clamp(audio.size, 0, 0, Number.MAX_SAFE_INTEGER),
    fileKey: typeof audio.fileKey === 'string' ? audio.fileKey : undefined,
    serverUrl: typeof audio.serverUrl === 'string' ? audio.serverUrl : undefined,
    dbKey: typeof audio.dbKey === 'string' ? audio.dbKey : undefined,
    savedToLibrary: audio.savedToLibrary === true,
  };
}

function readJson(key: string): Record<string, unknown> | null {
  const raw = localStorage.getItem(key);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? parsed as Record<string, unknown> : null;
  } catch {
    return null;
  }
}

function unwrapPlayback(value: Record<string, unknown> | null): Record<string, unknown> | null {
  if (!value) return null;
  const nested = value.playback;
  return nested && typeof nested === 'object'
    ? nested as Record<string, unknown>
    : value;
}

export function normalizePlaybackDraft(
  value: Partial<PlaybackDraft> | Record<string, unknown> | null | undefined,
  fallback: PlaybackDraft = EMPTY_PLAYBACK_DRAFT,
): PlaybackDraft {
  const source = value && typeof value === 'object' ? value as Record<string, unknown> : {};
  const sourceAudios = Array.isArray(source.audios) ? source.audios : fallback.audios;

  return {
    audios: sourceAudios.map(normalizeAudio).filter((audio): audio is TaskAudio => audio !== null),
    volume: clamp(source.volume, fallback.volume, 0, 100),
    fadeInDuration: clamp(source.fadeInDuration, fallback.fadeInDuration, 0, 120),
    fadeOutDuration: clamp(source.fadeOutDuration, fallback.fadeOutDuration, 0, 120),
    enableFade: typeof source.enableFade === 'boolean' ? source.enableFade : fallback.enableFade,
  };
}

export function getDefaultPlaybackDraft(): PlaybackDraft {
  if (typeof window === 'undefined') return clonePlaybackDraft(EMPTY_PLAYBACK_DRAFT);

  const saved = unwrapPlayback(readJson(DEFAULT_PLAYBACK_KEY));
  const legacy = unwrapPlayback(readJson(LEGACY_PLAYBACK_KEY));
  const legacyDraft = normalizePlaybackDraft(legacy);

  return normalizePlaybackDraft(saved, legacyDraft);
}

export function saveDefaultPlaybackDraft(draft: PlaybackDraft): void {
  if (typeof window === 'undefined') return;
  const playback = clonePlaybackDraft(normalizePlaybackDraft(draft));
  localStorage.setItem(DEFAULT_PLAYBACK_KEY, JSON.stringify({ version: 2, playback }));
}

export function clonePlaybackDraft(draft: PlaybackDraft): PlaybackDraft {
  return {
    ...draft,
    audios: draft.audios.map(audio => ({ ...audio })),
  };
}

export function playbackDraftFromTask(task: ScheduledTask): PlaybackDraft {
  return normalizePlaybackDraft({
    audios: task.audios,
    volume: task.volume,
    fadeInDuration: task.fadeInDuration,
    fadeOutDuration: task.fadeOutDuration,
    enableFade: task.enableFade,
  });
}
