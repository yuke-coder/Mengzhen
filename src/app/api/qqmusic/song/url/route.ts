/**
 * QQ音乐获取歌曲播放 URL API 路由
 *
 * 服务端代理请求 QQ音乐 API，避免浏览器 CORS 限制。
 * 需要配置 QQ音乐 VIP 账号的 cookie（环境变量 QQMUSIC_COOKIE）
 */

import { NextRequest, NextResponse } from 'next/server';

export const runtime = 'nodejs';

/** QQ音乐 API 请求头 */
function buildHeaders(): Record<string, string> {
  return {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    'Referer': 'https://y.qq.com/',
    'Cookie': process.env.QQMUSIC_COOKIE ?? '',
  };
}

interface QQMusicSongItem {
  songmid?: string;
  songid?: string;
  songname?: string;
  singer?: Array<{ name: string }>;
  albumname?: string;
  interval?: number;
  url?: string;
}

interface QQMusicUrlResponse {
  req_0?: {
    data?: {
      midurlinfo?: Array<{
        purl?: string;
        songmid?: string;
      }>;
    };
  };
  req_1?: {
    data?: {
      track_info?: QQMusicSongItem;
    };
  };
}

export async function POST(request: NextRequest) {
  try {
    const { songIds } = await request.json() as { songIds: string[] };

    if (!Array.isArray(songIds) || songIds.length === 0) {
      return NextResponse.json({ error: 'songIds is required' }, { status: 400 });
    }

    // 限制单次最多 50 首
    const ids = songIds.slice(0, 50);
    const songMidList = ids.join(',');

    // 获取播放 URL
    const urlParams = new URLSearchParams({
      format: 'json',
      inCharset: 'utf8',
      outCharset: 'utf-8',
      notice: '0',
      platform: 'yqq.json',
      needNewCode: '0',
      data: JSON.stringify({
        req_0: {
          module: 'vkey.GetVkeyServer',
          method: 'CgiGetVkey',
          param: {
            guid: '10000',
            songmid: ids,
            songtype: ids.map(() => 0),
            uin: '0',
            loginflag: 1,
            platform: '20',
          },
        },
      }),
    });

    const urlResponse = await fetch(`https://u.y.qq.com/cgi-bin/musicu.fcg?${urlParams.toString()}`, {
      headers: buildHeaders(),
    });

    if (!urlResponse.ok) {
      return NextResponse.json({ error: 'QQ Music API error', songs: [] }, { status: 502 });
    }

    const urlData: QQMusicUrlResponse = await urlResponse.json();
    const midurlinfo = urlData.req_0?.data?.midurlinfo ?? [];

    // 获取歌曲信息
    const infoParams = new URLSearchParams({
      format: 'json',
      inCharset: 'utf8',
      outCharset: 'utf-8',
      notice: '0',
      platform: 'yqq.json',
      needNewCode: '0',
      data: JSON.stringify({
        req_1: {
          module: 'music.trackInfoTracker',
          method: 'GetTrackInfo',
          param: {
            ids: ids.map((id) => parseInt(id, 10)),
            types: ids.map(() => 0),
          },
        },
      }),
    });

    const infoResponse = await fetch(`https://u.y.qq.com/cgi-bin/musicu.fcg?${infoParams.toString()}`, {
      headers: buildHeaders(),
    });

    const infoMap = new Map<string, QQMusicSongItem>();
    if (infoResponse.ok) {
      const infoData: QQMusicUrlResponse = await infoResponse.json();
      const trackInfo = infoData.req_1?.data?.track_info;
      // 简化处理：仅使用 midurlinfo 中的 purl
    }

    const songs = ids.map((songId, index) => {
      const urlInfo = midurlinfo[index];
      const purl = urlInfo?.purl ?? '';
      return {
        songId,
        songName: '',
        singerName: '',
        albumName: '',
        interval: 0,
        playUrl: purl ? `https://dl.stream.qqmusic.qq.com/${purl}` : '',
      };
    }).filter((s) => s.playUrl);

    return NextResponse.json({ songs });
  } catch (err) {
    console.error('[QQMusic API] 获取播放 URL 失败:', err);
    return NextResponse.json({ error: 'Internal error', songs: [] }, { status: 500 });
  }
}
