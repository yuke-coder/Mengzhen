export function formatFileSize(bytes: number | undefined | null): string {
  if (!bytes || bytes < 1024) return (bytes || 0) + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

export function formatDuration(seconds: number | undefined | null, showHours: boolean = false): string {
  if (!seconds) return "0:00";
  if (showHours) {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    if (h > 0) return `${h}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  }
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export interface AudioItemBase {
  id: string;
  name: string;
  url?: string;
  duration: number;
  fileKey?: string;
  serverUrl?: string;
  dbKey?: string;
  uploading?: boolean;
  uploadProgress?: number;
  uploadError?: string | null;
}
