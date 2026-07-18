import { randomBytes } from "node:crypto";
import { AUDIO_EXTENSIONS } from "@/lib/audio/formats";

export const AUDIO_BUCKET = "audios";

const MAX_FILE_NAME_LENGTH = 255;
const MAX_TITLE_LENGTH = 200;

export function getAudioExtension(fileName: string): string {
  const dotIndex = fileName.lastIndexOf(".");
  if (dotIndex < 0) return "";
  return fileName.slice(dotIndex).toLowerCase();
}

export function isSupportedAudio(fileName: string, mimeType: string): boolean {
  const extension = getAudioExtension(fileName);
  return AUDIO_EXTENSIONS.includes(extension) || mimeType.startsWith("audio/");
}

export function normalizeAudioFileName(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const fileName = value.replace(/[\u0000-\u001f\u007f]/g, "").trim().slice(0, MAX_FILE_NAME_LENGTH);
  return fileName || null;
}

export function toAudioTitle(fileName: string): string {
  return fileName.replace(/\.[^/.]+$/, "").trim().slice(0, MAX_TITLE_LENGTH) || "音频";
}

export function createAudioObjectKey(userId: string, fileName: string): string {
  const extension = getAudioExtension(fileName);
  const safeExtension = AUDIO_EXTENSIONS.includes(extension) ? extension : ".mp3";
  return `audios/${userId}/${Date.now()}_${randomBytes(10).toString("hex")}${safeExtension}`;
}

export function isUserAudioObjectKey(fileKey: unknown, userId: string): fileKey is string {
  if (typeof fileKey !== "string") return false;
  const prefix = `audios/${userId}/`;
  return fileKey.startsWith(prefix) && !fileKey.includes("..") && fileKey.length <= 500;
}

export function toDirectTusEndpoint(supabaseUrl: string): string {
  const url = new URL(supabaseUrl);
  if (url.hostname.endsWith(".supabase.co")) {
    url.hostname = url.hostname.replace(/\.supabase\.co$/, ".storage.supabase.co");
  }
  url.pathname = "/storage/v1/upload/resumable";
  url.search = "";
  url.hash = "";
  return url.toString();
}

export function isTusUploadEnabled(supabaseUrl: string, override = process.env.AUDIO_TUS_ENABLED): boolean {
  const normalizedOverride = override?.trim().toLowerCase();
  if (["1", "true", "yes", "on"].includes(normalizedOverride ?? "")) return true;
  if (["0", "false", "no", "off"].includes(normalizedOverride ?? "")) return false;

  // Supabase's official hosted domains support the documented TUS flow. Custom
  // gateways must opt in after their signed-upload compatibility is verified.
  return new URL(supabaseUrl).hostname.endsWith(".supabase.co");
}

export function formatByteSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(bytes >= 100 * 1024 * 1024 ? 0 : 1)} MB`;
}
