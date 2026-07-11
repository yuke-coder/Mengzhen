/**
 * 共享的音频上传逻辑
 */
import { AUDIO_EXTENSIONS } from './audio-formats';
import { useAuth } from './auth-context';

/**
 * 验证音频文件
 */
export const validateAudioFile = (
  file: File,
  existingFiles: Array<{ file: { name: string } }>
): string | null => {
  const ext = "." + file.name.split(".").pop()?.toLowerCase();
  const typeOk = file.type.startsWith('audio/') || file.type === '';
  const extOk = AUDIO_EXTENSIONS.includes(ext);

  if (!typeOk && !extOk) {
    return `不支持的音频格式，请上传 ${AUDIO_EXTENSIONS.join(", ")} 文件`;
  }

  if (existingFiles.some(a => a.file.name === file.name)) {
    return `文件「${file.name}」已存在`;
  }

  return null;
};

/**
 * 上传音频到服务器
 */
export const uploadAudioToServer = async (
  id: string,
  file: File,
  onProgress?: (progress: number) => void,
  onSuccess?: (url: string, fileKey: string) => void,
  onError?: (error: string) => void
): Promise<void> => {
  const formData = new FormData();
  formData.append("audio", file);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener("progress", (e) => {
      if (e.lengthComputable) {
        const progress = Math.round((e.loaded / e.total) * 100);
        onProgress?.(progress);
      }
    });

    xhr.addEventListener("load", () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        const data = JSON.parse(xhr.responseText);
        if (data.success) {
          onSuccess?.(data.audio_url, data.file_key);
          resolve();
        } else {
          const error = data.error || "上传失败";
          onError?.(error);
          reject(new Error(error));
        }
      } else {
        try {
          const data = JSON.parse(xhr.responseText);
          const error = data.error || `上传失败 (${xhr.status})`;
          onError?.(error);
          reject(new Error(error));
        } catch {
          const error = `上传失败 (${xhr.status})`;
          onError?.(error);
          reject(new Error(error));
        }
      }
    });

    xhr.addEventListener("error", () => {
      const error = "网络错误";
      onError?.(error);
      reject(new Error(error));
    });

    xhr.open("POST", "/api/audio/upload?save_to_files=true");
    xhr.send(formData);
  });
};
