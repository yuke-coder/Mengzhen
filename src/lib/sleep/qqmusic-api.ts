/**
 * QQ音乐 API 调用模块
 *
 * 用于获取脑波疗愈模式播放列表中歌曲的播放链接。
 * 用户要求使用 QQ音乐 API 实现所有功能（VIP 账号）。
 *
 * 端点说明：
 * - /api/qqmusic/song/url：获取歌曲播放 URL（需 VIP 账号 cookie）
 */

/** QQ音乐歌曲信息 */
export interface QQSongInfo {
  /** 歌曲 ID */
  songId: string;
  /** 歌曲名称 */
  songName: string;
  /** 歌手名称 */
  singerName: string;
  /** 专辑名称 */
  albumName: string;
  /** 歌曲时长（秒） */
  interval: number;
  /** 播放 URL */
  playUrl: string;
}

/**
 * 批量获取歌曲播放 URL
 *
 * @param songIds 歌曲 ID 列表
 * @returns 歌曲信息列表（含播放 URL）
 */
export async function fetchSongUrls(songIds: string[]): Promise<QQSongInfo[]> {
  if (songIds.length === 0) return [];

  try {
    const response = await fetch('/api/qqmusic/song/url', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ songIds }),
    });

    if (!response.ok) {
      console.warn('[QQMusic] 获取播放 URL 失败:', response.status);
      return [];
    }

    const data = await response.json();
    if (!data.songs || !Array.isArray(data.songs)) return [];

    return data.songs.filter((s: QQSongInfo) => s.playUrl);
  } catch (err) {
    console.warn('[QQMusic] 获取播放 URL 异常:', err);
    return [];
  }
}

/**
 * 获取单首歌曲播放 URL
 */
export async function fetchSongUrl(songId: string): Promise<string | null> {
  const songs = await fetchSongUrls([songId]);
  return songs[0]?.playUrl ?? null;
}
