/**
 * IndexedDB 音频存储工具
 * 
 * 用于在浏览器本地持久化存储音频 Blob 数据。
 * - 游客用户：音频只存 IndexedDB，跨页面播放
 * - 登录用户：同时上传到服务器 + IndexedDB 备份
 * 
 * 优势：支持大文件、跨页面导航和刷新后仍可用、无需登录
 */

const DB_NAME = 'dream_pillow_audio';
const STORE_NAME = 'audios';
const DB_VERSION = 1;

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'id' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

/** 保存音频 Blob 到 IndexedDB */
export async function saveAudioBlob(id: string, blob: Blob): Promise<void> {
  const db = await openDB();
  const tx = db.transaction(STORE_NAME, 'readwrite');
  tx.objectStore(STORE_NAME).put({ id, blob, timestamp: Date.now() });
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

/** 从 IndexedDB 读取音频 Blob */
export async function getAudioBlob(id: string): Promise<Blob | null> {
  const db = await openDB();
  const tx = db.transaction(STORE_NAME, 'readonly');
  const request = tx.objectStore(STORE_NAME).get(id);
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result?.blob ?? null);
    request.onerror = () => reject(request.error);
  });
}

/** 从 IndexedDB 删除音频 Blob */
export async function deleteAudioBlob(id: string): Promise<void> {
  const db = await openDB();
  const tx = db.transaction(STORE_NAME, 'readwrite');
  tx.objectStore(STORE_NAME).delete(id);
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

/** 清空所有音频 Blob */
export async function clearAllAudioBlobs(): Promise<void> {
  const db = await openDB();
  const tx = db.transaction(STORE_NAME, 'readwrite');
  tx.objectStore(STORE_NAME).clear();
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

/** 批量获取多个音频 Blob */
export async function getAudioBlobs(ids: string[]): Promise<Map<string, Blob>> {
  const result = new Map<string, Blob>();
  const db = await openDB();
  const tx = db.transaction(STORE_NAME, 'readonly');
  const store = tx.objectStore(STORE_NAME);

  await Promise.all(
    ids.map(
      (id) =>
        new Promise<void>((resolve) => {
          const request = store.get(id);
          request.onsuccess = () => {
            if (request.result?.blob) {
              result.set(id, request.result.blob);
            }
            resolve();
          };
          request.onerror = () => resolve(); // 跳过读取失败的
        })
    )
  );

  return result;
}
