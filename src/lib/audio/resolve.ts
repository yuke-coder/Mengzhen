"use client";

/**
 * 音频 URL 解析工具
 * 优先级：serverUrl > fileKey(Supabase 公开 URL) > dbKey(IndexedDB -> 本地文件)
 */

import type { AudioTrack } from './types';
import type { TaskAudio } from '@/lib/task-types';

const SUPABASE_URL = 'https://br-epic-clam-5a2fd709.supabase2.aidap-global.cn-beijing.volces.com';
const AUDIO_BUCKET = 'audios';

function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const result = reader.result as string;
      const base64 = result.includes(',') ? result.split(',')[1] : result;
      resolve(base64);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

/**
 * 解析播放列表，返回 tracksJson 和首个音频信息
 * 供 UnifiedAudioManager.play() 共用
 */
export async function resolveTracksJson(
  tracks: AudioTrack[]
): Promise<{ tracksJson: string; firstUrl: string; firstName: string }> {
  const resolved: { url: string; name: string }[] = [];
  let firstUrl = '';
  let firstName = '';

  for (const track of tracks) {
    // 直接可用的 URL
    if (track.url && track.url.startsWith('http')) {
      resolved.push({ url: track.url, name: track.name });
      if (!firstUrl) { firstUrl = track.url; firstName = track.name; }
      continue;
    }

    // proxy 来源：/api/audio/proxy?key=xxx -> 构造完整 URL
    if (track.url && track.url.startsWith('/api/audio/proxy')) {
      const fullUrl = `${window.location.origin}${track.url}`;
      resolved.push({ url: fullUrl, name: track.name });
      if (!firstUrl) { firstUrl = fullUrl; firstName = track.name; }
      continue;
    }

    // 从 source 字段判断
    if (track.source === 'server' && track.url) {
      resolved.push({ url: track.url, name: track.name });
      if (!firstUrl) { firstUrl = track.url; firstName = track.name; }
    }
  }

  return {
    tracksJson: JSON.stringify(resolved),
    firstUrl,
    firstName,
  };
}

/**
 * 从 TaskAudio 数组解析 tracksJson（scheduler 用）
 */
export async function resolveTracksJsonFromTaskAudios(
  audios: TaskAudio[]
): Promise<{ tracksJson: string; firstUrl: string; firstName: string }> {
  const resolved: { url: string; name: string }[] = [];
  let firstUrl = '';
  let firstName = '';

  for (const audio of audios) {
    // 1. serverUrl 直链
    if (audio.serverUrl && audio.serverUrl.startsWith('http')) {
      resolved.push({ url: audio.serverUrl, name: audio.name });
      if (!firstUrl) { firstUrl = audio.serverUrl; firstName = audio.name; }
      continue;
    }
    // 2. fileKey -> Supabase 公开 URL
    if (audio.fileKey && audio.fileKey.trim() !== '') {
      const url = `${SUPABASE_URL}/storage/v1/object/public/${AUDIO_BUCKET}/${audio.fileKey}`;
      resolved.push({ url, name: audio.name });
      if (!firstUrl) { firstUrl = url; firstName = audio.name; }
      continue;
    }
    // 3. dbKey -> IndexedDB (Web 端仅保留读取，播放由原生 App 处理)
    if (audio.dbKey && audio.dbKey.trim() !== '') {
      // Web 端无法播放，跳过
      continue;
    }
  }

  return {
    tracksJson: JSON.stringify(resolved),
    firstUrl,
    firstName,
  };
}
