"use client";
import { useState, useCallback, useEffect, Suspense } from "react";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { toast } from "@/components/sonner";
import { AudioUpload } from "@/components/audio-upload";
import { TaskForm } from "@/components/task-form";
import { TaskList } from "@/components/task-list";
import { TaskModal } from "@/components/task-modal";
import { Spinner } from "@/components/ui/spinner";
import { PlayMode, ScheduledTask } from "@/lib/task-types";
import { getPlayMode, setPlayMode as savePlayMode, getAllTasks, cleanupCompletedOnceTasks, cleanupCancelledTasks } from "@/lib/task-store";
import { startTaskScheduler, stopTaskScheduler, getTaskScheduler } from "@/lib/task-scheduler";
import DynamicBackground from "@/components/dynamic-background";
import UnifiedAudioManager from "@/lib/audio";
import EnhancedTaskScheduler from "@/lib/background-scheduler";
import { setupAutoUnlock, unlockAudio } from "@/lib/audio-unlock";

function useClientOnly() {
    const [mounted, setMounted] = useState(false);
    useEffect(() => setMounted(true), []);
    return mounted;
}

export default function CreatePage() {
    return (
        <Suspense fallback={<LoadingFallback />}>
            <CreatePageContent />
        </Suspense>
    );
}

function LoadingFallback() {
    return (
        <div className="min-h-screen flex items-center justify-center bg-background">
            <div className="flex flex-col items-center gap-4">
                <Spinner className="text-foreground" />
                <p className="text-base text-muted-foreground">Loading...</p>
            </div>
        </div>
    );
}

function CreatePageContent() {
    const searchParams = useSearchParams();
    const [playMode, setPlayMode] = useState<PlayMode>("default");
    const [showTaskForm, setShowTaskForm] = useState(false);
    const [editingTask, setEditingTask] = useState<ScheduledTask | null>(null);
    const [tasks, setTasks] = useState<ScheduledTask[]>([]);
    const [tasksVersion, setTasksVersion] = useState(0);
    const [audioUnlocked, setAudioUnlocked] = useState(false);
    const mounted = useClientOnly();

    // 初始化增强版调度器
    useEffect(() => {
        if (!mounted) return;

        const initScheduler = async () => {
            try {
                await EnhancedTaskScheduler.getInstance().initialize();
            } catch (error) {
                console.error('[App] 调度器初始化失败:', error);
            }
        };

        initScheduler();

        return () => EnhancedTaskScheduler.getInstance().destroy();
    }, [mounted]);

    // 页面重新可见时恢复播放状态
    useEffect(() => {
        if (!mounted) return;

        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible') {
                // 恢复所有保存的播放状态
                EnhancedTaskScheduler.getInstance().restoreAllSavedTasks();
            }
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);
        return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
    }, [mounted]);

    // 设置自动音频解锁（用户交互时触发）
    useEffect(() => {
        if (!mounted) return;

        const cleanup = setupAutoUnlock();
        return cleanup;
    }, [mounted]);

    // 检查是否已经解锁过音频
    useEffect(() => {
        if (!mounted) return;
        try {
            const alreadyUnlocked = localStorage.getItem('audio_unlocked') === 'true';
            if (alreadyUnlocked) {
                setAudioUnlocked(true);
                if (UnifiedAudioManager.isAndroidDevice()) {
                    EnhancedTaskScheduler.getInstance().requestWakeLock?.();
                }
            }
        } catch {}
    }, [mounted]);

    useEffect(() => {
        if (!mounted) return;
        setPlayMode(getPlayMode());
        const completed = cleanupCompletedOnceTasks();
        const cancelled = cleanupCancelledTasks();
        if (completed.removedCount > 0) toast.info(`已自动清理 ${completed.removedCount} 个已完成的一次性任务`, { duration: 3000 });
        if (cancelled.removedCount > 0) toast.info(`已自动清理 ${cancelled.removedCount} 个已取消的一次性任务`, { duration: 3000 });
        setTasks(getAllTasks());
    }, [mounted]);

    useEffect(() => {
        if (!mounted) return;

        const executing = getAllTasks().filter(t => t.status === "executing");
        startTaskScheduler().then(() => {
            const resumed = executing.filter(t => getTaskScheduler().getTaskPhase(t.id) !== "idle").map(t => t.name);
            if (resumed.length > 0) toast.success(`${resumed.length} 个任务已恢复执行`, { duration: 3000 });
        });

        return () => stopTaskScheduler();
    }, [mounted]);

    // 用户点击解锁音频
    const handleUnlockAudio = useCallback(async () => {
        try {
            const scheduler = EnhancedTaskScheduler.getInstance();

            // 先尝试初始化AudioContext
            const initialized = await scheduler.initializeAudioContext();

            // 尝试获取唤醒锁
            if (UnifiedAudioManager.isAndroidDevice()) {
                await scheduler.requestWakeLock();
                toast.success('音频已解锁，设备将保持亮屏播放');
            } else {
                toast.success('音频已解锁');
            }

            setAudioUnlocked(true);
            localStorage.setItem('audio_unlocked', 'true');

        } catch (error) {
            console.error('解锁音频失败:', error);
            toast.error('解锁失败，请再试一次');
        }
    }, []);

    // 暴露全局解锁方法，方便其他地方调用
    useEffect(() => {
        (window as any).unlockAudio = async () => {
            await handleUnlockAudio();
        };
        return () => {
            delete (window as any).unlockAudio;
        };
    }, [handleUnlockAudio]);

    useEffect(() => {
        if (!mounted) return;
        setTasks(getAllTasks());
    }, [tasksVersion, mounted]);

    useEffect(() => {
        if (!mounted) return;
        const handleVisibilityChange = () => {
            if (document.visibilityState !== "visible") return;
            const completed = cleanupCompletedOnceTasks();
            const cancelled = cleanupCancelledTasks();
            if (completed.removedCount > 0) toast.info(`已自动清理 ${completed.removedCount} 个已完成的一次性任务`, { duration: 3000 });
            if (cancelled.removedCount > 0) toast.info(`已自动清理 ${cancelled.removedCount} 个已取消的一次性任务`, { duration: 3000 });
            setTasks(getAllTasks());
        };
        document.addEventListener("visibilitychange", handleVisibilityChange);
        return () => document.removeEventListener("visibilitychange", handleVisibilityChange);
    }, [mounted]);

    const handleModeChange = useCallback((mode: PlayMode) => {
        setPlayMode(mode);
        savePlayMode(mode);
        if (mode === "default") {
            setShowTaskForm(false);
            setEditingTask(null);
        }
    }, []);

    const handleTaskSaved = useCallback(() => {
        setShowTaskForm(false);
        setEditingTask(null);
        setTasksVersion(v => v + 1);
    }, []);

    const handleEditTask = useCallback((task: ScheduledTask) => {
        setEditingTask(task);
        setShowTaskForm(true);
    }, []);

    return (
        <div className="min-h-screen text-foreground overflow-x-hidden relative z-10">
            {/* 音频解锁引导层 - 必须用户点击按钮后才消失 */}
            {!audioUnlocked && mounted && (
                <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-black/85 backdrop-blur-sm animate-in fade-in duration-300">
                    <div className="text-center space-y-8 px-4 max-w-md">
                        <div className="w-24 h-24 mx-auto mb-4 relative">
                            <div className="absolute inset-0 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-full animate-pulse opacity-30"></div>
                            <div className="relative w-full h-full bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-full flex items-center justify-center">
                                <svg className="w-12 h-12 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M9 18V6L3 12V18H9Z" />
                                    <path d="M15 6L18 12L15 18" />
                                    <path d="M21 18V6L18 12" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-white">解锁音频播放</h2>
                        <p className="text-white/70 text-base leading-relaxed">
                            为了确保定时任务能正常自动播放，请点击下方按钮解锁音频权限。<br/>
                            解锁后，你的定时任务将在设定时间自动播放音频。
                        </p>
                        <div className="flex justify-center pt-6">
                            <button
                                onClick={handleUnlockAudio}
                                className="px-10 py-4 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-lg text-white font-bold text-lg hover:opacity-90 active:opacity-80 transition-all shadow-lg hover:shadow-xl active:scale-95"
                            >
                                立即解锁
                            </button>
                        </div>
                        <div className="flex justify-center pt-4">
                            <svg className="w-8 h-8 text-[var(--brand-start)] animate-bounce" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M12 5V19M5 12L12 5L19 12" />
                            </svg>
                        </div>
                    </div>
                </div>
            )}

            {/* 调试用：显示音频状态 */}
            {process.env.NODE_ENV === 'development' && mounted && (
                <div className="fixed bottom-4 right-4 z-40 bg-black/70 text-white p-2 rounded text-xs">
                    Audio State: {EnhancedTaskScheduler.getInstance().getAudioState() || 'uninitialized'}
                </div>
            )}

            <DynamicBackground />
            <main className="pt-0 sm:pt-14 relative">
                <section className="relative min-h-[85vh] flex flex-col items-center justify-center px-4 sm:px-6 overflow-hidden">
                    <div className="relative z-20 max-w-4xl mx-auto w-full space-y-6 px-2 sm:px-4 md:px-0">
                        <div className="text-center space-y-4 mt-8">
                            <div className="relative inline-block text-center">
                                <svg className="w-full max-w-4xl mx-auto" viewBox="0 0 1100 700" preserveAspectRatio="xMidYMid meet">
                                    <defs>
                                        <linearGradient id="createTitleGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                                            <stop offset="0%" stopColor="#5EEDA0" />
                                            <stop offset="35%" stopColor="#40C78A" />
                                            <stop offset="50%" stopColor="#60C4A0" />
                                            <stop offset="65%" stopColor="#9055E0" />
                                            <stop offset="100%" stopColor="#A855F7" />
                                        </linearGradient>
                                    </defs>
                                    <text x="50%" y="320" textAnchor="middle" dominantBaseline="middle" fontSize="clamp(100px, 28vw, 280px)" fontWeight="bold" fontFamily="system-ui, -apple-system, sans-serif" fill="url(#createTitleGradient)">
                                        <tspan x="50%" dy="-0.5em">星河入眠</tspan>
                                        <tspan x="50%" dy="1.2em">伴你梦枕</tspan>
                                    </text>
                                </svg>
                            </div>
                            <p className="text-lg text-muted-foreground/70">上传音频 · 自定义定时 · 自动助眠播放</p>
                        </div>

                        <AudioUpload
                            importFileKey={searchParams.get("fileKey") || undefined}
                            mode={playMode}
                            onModeChange={handleModeChange}
                            onAudioUploaded={audioList => {
                                const last = audioList[audioList.length - 1];
                                if (last) toast.success(`「${last.file.name}」上传成功`);
                            }}
                            onAudioRemoved={() => { }}
                        >
                            <div className="space-y-5 sm:space-y-6">
                                <TaskList
                                    tasks={tasks}
                                    onEdit={handleEditTask}
                                    onCreate={() => { setEditingTask(null); setShowTaskForm(true); }}
                                    onRefresh={() => setTasksVersion(v => v + 1)}
                                />
                            </div>
                        </AudioUpload>
                    </div>
                </section>
            </main>

            <TaskModal visible={showTaskForm} onClose={() => { setShowTaskForm(false); setEditingTask(null); }}>
                <TaskForm editTask={editingTask} onSave={handleTaskSaved} onCancel={() => { setShowTaskForm(false); setEditingTask(null); }} />
            </TaskModal>

            <footer className="hidden sm:block border-t border-border py-8 px-6 bg-muted/20 relative z-20">
                <div className="max-w-5xl mx-auto text-center">
                    <div className="flex items-center justify-center gap-2 mb-3">
                        <Image src="/logo.png" alt="梦枕" width={20} height={20} className="rounded-md shadow-[inset_0_1px_4px_rgba(0,0,0,0.35)]" />
                        <span className="font-bold text-lg bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent">梦枕</span>
                    </div>
                    <p className="text-xs text-muted-foreground">深夜助眠播放器 · PWA渐进式网页应用 · 自定义音频</p>
                </div>
            </footer>
        </div>
    );
}
