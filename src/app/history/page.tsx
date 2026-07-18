'use client';

import { useEffect, useState, useCallback, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import Navbar from '@/components/navbar';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import {
  Music,
  Play,
  Clock,
  FileText,
  Download,
  RefreshCw,
  Calendar,
  AlertCircle,
} from 'lucide-react';
import Link from 'next/link';
import { cn } from '@/lib/utils';
import { formatFileSize, formatDuration } from '@/lib/audio';

interface AudioRecord {
  id: string;
  title: string;
  file_url: string;
  file_key: string;
  file_name: string;
  file_size: number;
  duration: number;
  mime_type: string;
  created_at: string;
}



export default function HistoryPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();
  const [audios, setAudios] = useState<AudioRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [playingId, setPlayingId] = useState<string | null>(null);
  const currentAudioRef = useRef<HTMLAudioElement | null>(null);

  // 组件卸载时清理音频
  useEffect(() => {
    return () => {
      if (currentAudioRef.current) {
        currentAudioRef.current.pause();
        currentAudioRef.current.removeAttribute('src');
        currentAudioRef.current.load();
        currentAudioRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!authLoading && !user) {
      router.push('/auth/login');
    }
  }, [user, authLoading, router]);

  const fetchAudios = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setLoadError(null);
    try {
      const res = await fetch('/api/audio/my-list', { cache: 'no-store' });
      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || '获取我的音频失败');
      }
      setAudios(data.audios || []);
    } catch (error) {
      console.error('获取音频列表失败:', error);
      setLoadError(error instanceof Error && /[\u4e00-\u9fff]/.test(error.message)
        ? error.message
        : '音频库加载失败，请重试');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    fetchAudios();
  }, [fetchAudios]);

  const handlePlay = useCallback((audio: AudioRecord) => {
    // 停止当前播放
    if (currentAudioRef.current) {
      currentAudioRef.current.pause();
      currentAudioRef.current.removeAttribute('src');
      currentAudioRef.current.load();
      currentAudioRef.current = null;
    }

    // 如果点击的是正在播放的，则停止
    if (playingId === audio.id) {
      setPlayingId(null);
      return;
    }

    const url = audio.file_key
      ? `/api/audio/proxy?key=${encodeURIComponent(audio.file_key)}`
      : audio.file_url;

    const audioEl = new Audio();
    audioEl.src = url;
    
    audioEl.onended = () => {
      setPlayingId(null);
      currentAudioRef.current = null;
    };
    audioEl.onerror = () => {
      console.error('[梦枕] 播放失败:', audio.title);
      setPlayingId(null);
      currentAudioRef.current = null;
    };

    currentAudioRef.current = audioEl;
    setPlayingId(audio.id);
    
    audioEl.play().catch((err) => {
      // AbortError 是正常的（快速切换/导航时），不报错
      if (err.name !== 'AbortError') {
        console.error('[梦枕] 播放失败:', err);
      }
      setPlayingId(null);
      currentAudioRef.current = null;
    });
  }, [playingId]);

  const handleExport = async (audio: AudioRecord) => {
    try {
      const url = audio.file_key
        ? `/api/audio/proxy?key=${encodeURIComponent(audio.file_key)}`
        : audio.file_url;

      const response = await fetch(url);
      const blob = await response.blob();
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = audio.file_name || audio.title;
      link.click();
      window.URL.revokeObjectURL(blobUrl);
    } catch (err) {
      console.error('导出失败:', err);
    }
  };

  const handleImport = (audio: AudioRecord) => {
    // 跳转到设置页，携带我的音频 file_key 参数
    const fileKey = audio.file_key 
      ? encodeURIComponent(audio.file_key) 
      : '';
    router.push(`/settings?fileKey=${fileKey}`);
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (authLoading || !user) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Spinner className="text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <div className="max-w-3xl mx-auto px-4 pt-24 pb-12">
        {/* 顶部 */}
        <div className="flex items-center gap-4 mb-8">
          <Button
            variant="ghost"
            size="icon"
            className="rounded-full"
            onClick={() => fetchAudios()}
            disabled={loading}
          >
            {loading ? (
              <Spinner size="sm" className="h-5 w-5 text-muted-foreground" />
            ) : (
              <RefreshCw className="w-5 h-5" />
            )}
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-foreground">我的音频</h1>
            <p className="text-sm text-muted-foreground mt-1">
              共 {audios.length} 个已保存音频
            </p>
          </div>
        </div>

        {/* 列表 */}
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Spinner className="text-muted-foreground" />
          </div>
        ) : loadError ? (
          <div className="flex flex-col items-center justify-center py-20 gap-4">
            <AlertCircle className="w-8 h-8 text-red-400" />
            <p className="text-sm text-red-400">{loadError}</p>
            <Button variant="ghost" onClick={() => void fetchAudios()}>
              重新加载
            </Button>
          </div>
        ) : audios.length === 0 ? (
          <div
            className="flex flex-col items-center justify-center py-20 px-4 text-center"
            aria-live="polite"
          >
            <div className="w-14 h-14 rounded-2xl bg-[var(--brand-fill)] flex items-center justify-center">
              <Music className="w-7 h-7 text-[var(--brand-deep)]" />
            </div>
            <h2 className="mt-5 text-lg font-semibold text-foreground text-balance">
              音频库还是空的
            </h2>
            <p className="mt-2 max-w-md text-sm leading-6 text-muted-foreground text-pretty">
              选择音频只会为任务准备播放资源，不会自动保存到这里。请在设置页手动点击“存入音频库”。
            </p>
            <Link href="/settings" className="mt-6">
              <Button className="bg-[var(--brand-deep)] text-white hover:bg-[var(--brand-deep)]/90">
                返回设置
              </Button>
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {audios.map((audio) => (
              <div
                key={audio.id}
                className={cn(
                  "flex items-center gap-3 p-4 rounded-xl",
                  "bg-card border border-border/50",
                  "hover:border-primary/30 hover:bg-muted/20",
                  "transition-all duration-200"
                )}
              >
                {/* 播放按钮 */}
                <button
                  onClick={() => handlePlay(audio)}
                  className={cn(
                    "w-11 h-11 rounded-full flex items-center justify-center flex-shrink-0",
                    "bg-gradient-to-br from-pink-500 to-purple-500",
                    "text-white shadow-md",
                    "hover:shadow-lg hover:shadow-pink-500/20",
                    "hover:scale-105 active:scale-95",
                    "transition-all duration-200"
                  )}
                >
                  {playingId === audio.id ? (
                    <div className="w-4 h-4 flex items-center justify-center">
                      <div className="w-1 h-4 bg-white rounded-full animate-pulse" />
                    </div>
                  ) : (
                    <Play className="w-4 h-4 ml-0.5" fill="white" />
                  )}
                </button>

                {/* 音频信息 */}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground truncate">
                    {audio.title || audio.file_name}
                  </p>
                  <div className="flex items-center gap-3 mt-1 text-xs text-muted-foreground flex-wrap">
                    <span className="flex items-center gap-1">
                      <FileText className="w-3 h-3" />
                      {formatFileSize(audio.file_size)}
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {audio.duration > 0 ? formatDuration(audio.duration) : '--:--'}
                    </span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {formatDate(audio.created_at)}
                    </span>
                  </div>
                </div>

                {/* 导出和导入按钮 */}
                <div className="flex items-center gap-2 flex-shrink-0">
                  <button
                    onClick={() => handleExport(audio)}
                    className={cn(
                      "w-9 h-9 rounded-lg flex items-center justify-center",
                      "bg-muted hover:bg-muted/80",
                      "text-muted-foreground hover:text-foreground",
                      "transition-all duration-200",
                      "hover:scale-105 active:scale-95"
                    )}
                    title="导出下载"
                  >
                    <Download className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleImport(audio)}
                    className={cn(
                      "w-9 h-9 rounded-lg flex items-center justify-center",
                      "bg-gradient-to-br from-pink-500/10 to-purple-500/10",
                      "hover:from-pink-500/20 hover:to-purple-500/20",
                      "text-pink-500 hover:text-pink-400",
                      "transition-all duration-200",
                      "hover:scale-105 active:scale-95"
                    )}
                    title="导入到设置"
                  >
                    <RefreshCw className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
