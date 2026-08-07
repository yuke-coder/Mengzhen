/**
 * 助眠播放器 Hook
 *
 * 整迁自 QQ音乐 Kuikly bw_main_player 页面的播放控制逻辑
 * 对齐 IPlayProcessInterface 的 playMusic/pauseMusic/resumeMusic/stopMusic
 *
 * 播放来源使用 HEALING_PLAY_INFO.from (42800849)
 * 到期停止使用 FromInfo.FROM_AUTO_CLOSE (114)
 */

'use client';

import { useState, useRef, useCallback, useEffect } from 'react';
import { HEALING_SONG_IDS } from './config';
import { fetchSongUrls, type QQSongInfo } from './qqmusic-api';
import { FROM_AUTO_CLOSE, useSleepTimer } from './timer';

/** 播放状态（对齐 TPPlayerState 11种状态的简化版） */
export type SleepPlayState = 'idle' | 'loading' | 'playing' | 'paused' | 'error';

/** 助眠播放器选项 */
interface SleepPlayerOptions {
  /** 播放结束回调 */
  onComplete?: () => void;
  /** 定时到期回调 */
  onTimerExpire?: () => void;
}

/**
 * 助眠播放器 Hook
 *
 * 用法：
 * ```tsx
 * const player = useSleepPlayer();
 * await player.play();   // 开始播放疗愈歌单
 * player.pause();        // 暂停
 * player.resume();       // 恢复
 * player.stop();         // 停止
 * ```
 */
export function useSleepPlayer(options: SleepPlayerOptions = {}) {
  const [playState, setPlayState] = useState<SleepPlayState>('idle');
  const [currentSong, setCurrentSong] = useState<QQSongInfo | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [songs, setSongs] = useState<QQSongInfo[]>([]);
  const [error, setError] = useState<string | null>(null);

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const fadeTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const targetVolumeRef = useRef(0.7);
  const onCompleteRef = useRef(options.onComplete);
  const currentIndexRef = useRef(0);

  // 定时器（到期后停止播放）
  const timer = useSleepTimer(() => {
    // 定时到期：对齐 stopMusic(FROM_AUTO_CLOSE=114)
    stop(true);
    options.onTimerExpire?.();
  });

  useEffect(() => {
    onCompleteRef.current = options.onComplete;
  }, [options.onComplete]);

  // 清理
  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
        audioRef.current.src = '';
      }
      if (fadeTimerRef.current) {
        clearInterval(fadeTimerRef.current);
      }
    };
  }, []);

  /** 加载播放列表 */
  const loadPlaylist = useCallback(async () => {
    if (songs.length > 0) return songs;
    setPlayState('loading');
    setError(null);
    const fetched = await fetchSongUrls(HEALING_SONG_IDS);
    if (fetched.length === 0) {
      setError('无法获取播放列表，请检查 QQ音乐 VIP 配置');
      setPlayState('error');
      return [];
    }
    setSongs(fetched);
    return fetched;
  }, [songs]);

  /** 播放指定歌曲 */
  const playSong = useCallback((songList: QQSongInfo[], index: number) => {
    if (index < 0 || index >= songList.length) return;
    const song = songList[index];
    currentIndexRef.current = index;
    setCurrentIndex(index);
    setCurrentSong(song);

    // 创建新的 Audio 元素
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.src = '';
    }

    const audio = new Audio(song.playUrl);
    audio.loop = false;
    audio.volume = 0; // 从 0 开始渐入
    audioRef.current = audio;

    // 渐入
    const targetVolume = targetVolumeRef.current;
    const fadeSteps = 30;
    const fadeInterval = 50; // ms
    let step = 0;
    if (fadeTimerRef.current) clearInterval(fadeTimerRef.current);
    fadeTimerRef.current = setInterval(() => {
      step++;
      audio.volume = Math.min((step / fadeSteps) * targetVolume, targetVolume);
      if (step >= fadeSteps) {
        if (fadeTimerRef.current) {
          clearInterval(fadeTimerRef.current);
          fadeTimerRef.current = null;
        }
      }
    }, fadeInterval);

    audio.play().then(() => {
      setPlayState('playing');
    }).catch((err) => {
      console.error('[SleepPlayer] 播放失败:', err);
      setError('播放失败，请重试');
      setPlayState('error');
    });

    // 播放结束 → 下一首
    audio.addEventListener('ended', () => {
      const nextIndex = currentIndexRef.current + 1;
      if (nextIndex < songList.length) {
        playSong(songList, nextIndex);
      } else {
        // 列表播放完毕，从头循环
        playSong(songList, 0);
      }
    });

    audio.addEventListener('error', () => {
      setError('音频加载失败');
      setPlayState('error');
    });
  }, []);

  /** 开始播放 */
  const play = useCallback(async () => {
    setError(null);
    const songList = await loadPlaylist();
    if (songList.length === 0) return;
    playSong(songList, 0);
  }, [loadPlaylist, playSong]);

  /** 暂停 */
  const pause = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.pause();
      setPlayState('paused');
    }
  }, []);

  /** 恢复播放 */
  const resume = useCallback(() => {
    if (audioRef.current) {
      audioRef.current.play().then(() => {
        setPlayState('playing');
      }).catch(() => {
        setError('恢复播放失败');
        setPlayState('error');
      });
    }
  }, []);

  /** 停止播放 */
  const stop = useCallback((fromAutoClose = false) => {
    if (audioRef.current) {
      // 渐出
      const audio = audioRef.current;
      const currentVolume = audio.volume;
      const fadeSteps = 20;
      const fadeInterval = 50;
      let step = 0;
      if (fadeTimerRef.current) clearInterval(fadeTimerRef.current);
      fadeTimerRef.current = setInterval(() => {
        step++;
        audio.volume = Math.max(currentVolume * (1 - step / fadeSteps), 0);
        if (step >= fadeSteps) {
          if (fadeTimerRef.current) {
            clearInterval(fadeTimerRef.current);
            fadeTimerRef.current = null;
          }
          audio.pause();
          audio.src = '';
          setPlayState('idle');
          setCurrentSong(null);
          if (fromAutoClose) {
            onCompleteRef.current?.();
          }
        }
      }, fadeInterval);
    } else {
      setPlayState('idle');
    }
    // 停止定时器
    if (fromAutoClose) {
      timer.stop();
    }
  }, [timer]);

  /** 下一首 */
  const next = useCallback(() => {
    if (songs.length === 0) return;
    const nextIndex = (currentIndexRef.current + 1) % songs.length;
    playSong(songs, nextIndex);
  }, [songs, playSong]);

  /** 上一首 */
  const previous = useCallback(() => {
    if (songs.length === 0) return;
    const prevIndex = (currentIndexRef.current - 1 + songs.length) % songs.length;
    playSong(songs, prevIndex);
  }, [songs, playSong]);

  /** 设置音量 */
  const setVolume = useCallback((volume: number) => {
    const v = Math.max(0, Math.min(1, volume));
    targetVolumeRef.current = v;
    if (audioRef.current && !fadeTimerRef.current) {
      audioRef.current.volume = v;
    }
  }, []);

  /** 启动定时关闭 */
  const startTimer = useCallback((minutes: number) => {
    timer.start(minutes);
  }, [timer]);

  /** 停止定时关闭 */
  const stopTimer = useCallback(() => {
    timer.stop();
  }, [timer]);

  return {
    /** 播放状态 */
    playState,
    /** 当前歌曲 */
    currentSong,
    /** 当前歌曲索引 */
    currentIndex,
    /** 播放列表 */
    songs,
    /** 错误信息 */
    error,
    /** 是否播放中 */
    isPlaying: playState === 'playing',
    /** 定时器状态 */
    timer: {
      isRunning: timer.isRunning,
      remainingMs: timer.remainingMs,
      formatRemaining: timer.formatRemaining,
      timerMode: timer.timerMode,
      timerMinutes: timer.timerMinutes,
      presets: timer.presets,
    },
    /** 播放控制 */
    play,
    pause,
    resume,
    stop,
    next,
    previous,
    setVolume,
    /** 定时控制 */
    startTimer,
    stopTimer,
  };
}
