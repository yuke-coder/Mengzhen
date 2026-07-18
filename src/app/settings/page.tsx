"use client";
import { useState, useCallback, useEffect, Suspense } from "react";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { toast } from "@/components/sonner";
import { PlaybackSettingsPanel } from "@/components/playback-settings-panel";
import { TaskForm } from "@/components/task-form";
import { TaskList } from "@/components/task-list";
import { TaskModal } from "@/components/task-modal";
import { Spinner } from "@/components/ui/spinner";
import { PlayMode, ScheduledTask, type PlaybackDraft } from "@/lib/task-types";
import { getPlayMode, setPlayMode as savePlayMode, getAllTasks, cleanupCompletedOnceTasks, cleanupCancelledTasks } from "@/lib/task-store";
import { EMPTY_PLAYBACK_DRAFT, getDefaultPlaybackDraft, saveDefaultPlaybackDraft } from "@/lib/playback-draft";
import { startTaskScheduler, stopTaskScheduler, getTaskScheduler } from "@/lib/task-scheduler";
import DynamicBackground from "@/components/dynamic-background";
import { setupAutoUnlock } from "@/lib/audio";
import EnhancedTaskScheduler from "@/lib/background-scheduler";
import { useIsMobile } from "@/hooks/use-mobile";
import { usePlaybackController } from "@/hooks/use-playback-controller";
import { HeroTitle } from "@/components/hero-title";

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
    const isMobile = useIsMobile();
    const [playMode, setPlayMode] = useState<PlayMode>("default");
    const [showTaskForm, setShowTaskForm] = useState(false);
    const [editingTask, setEditingTask] = useState<ScheduledTask | null>(null);
    const [taskFormSession, setTaskFormSession] = useState(0);
    const [sharedPlaybackDraft, setSharedPlaybackDraft] = useState<PlaybackDraft>(() => ({
        ...EMPTY_PLAYBACK_DRAFT,
        audios: [],
    }));
    const [playbackDraftLoaded, setPlaybackDraftLoaded] = useState(false);
    const [tasks, setTasks] = useState<ScheduledTask[]>([]);
    const [tasksVersion, setTasksVersion] = useState(0);
    const [audioUnlocked, setAudioUnlocked] = useState(false);
    const [showScreenChoice, setShowScreenChoice] = useState(false);
    const mounted = useClientOnly();
    const sharedPlaybackController = usePlaybackController({
        value: sharedPlaybackDraft,
        onChange: setSharedPlaybackDraft,
    });
    const stopSharedPreview = sharedPlaybackController.preview.stop;

    useEffect(() => {
        if (!mounted) return;
        setPlayMode(getPlayMode());
        setSharedPlaybackDraft(getDefaultPlaybackDraft());
        setPlaybackDraftLoaded(true);
    }, [mounted]);

    useEffect(() => {
        if (!mounted || !playbackDraftLoaded) return;
        saveDefaultPlaybackDraft(sharedPlaybackDraft);
    }, [mounted, playbackDraftLoaded, sharedPlaybackDraft]);

    // 初始化
    useEffect(() => {
        if (!mounted) return;

        const init = async () => {
            try {
                await EnhancedTaskScheduler.getInstance().initialize();
            } catch (error) {
                console.error('[App] 初始化失败:', error);
            }
        };

        init();

        // 检查是否已解锁
        const alreadyUnlocked = localStorage.getItem('audio_unlocked') === 'true';
        if (alreadyUnlocked) {
            setAudioUnlocked(true);
        }

        return () => EnhancedTaskScheduler.getInstance().destroy();
    }, [mounted]);

    // 页面可见时恢复
    useEffect(() => {
        if (!mounted) return;
        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible') {
                EnhancedTaskScheduler.getInstance().restoreAllSavedTasks();
            }
        };
        document.addEventListener('visibilitychange', handleVisibilityChange);
        return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
    }, [mounted]);

    // 自动解锁
    useEffect(() => {
        if (!mounted) return;
        const cleanup = setupAutoUnlock();
        return cleanup;
    }, [mounted]);


    const requestPermissions = useCallback(async () => {
        // 只在没有权限时才请求浏览器原生权限弹窗
        try {
            if ('Notification' in window && Notification.permission === 'default') {
                await Notification.requestPermission();
            }
        } catch (e) {
            console.error('[Notification]', e);
        }

        try {
            if (navigator.storage && navigator.storage.persisted) {
                const alreadyPersisted = await navigator.storage.persisted();
                if (!alreadyPersisted && navigator.storage.persist) {
                    await navigator.storage.persist();
                }
            }
        } catch (e) {
            console.error('[Storage]', e);
        }
    }, []);

    const handleStart = useCallback(async () => {
        try {
            await EnhancedTaskScheduler.getInstance().initializeAudioContext();
            setAudioUnlocked(true);
            localStorage.setItem('audio_unlocked', 'true');

            // 检查是否已经选过屏幕选项
            const screenChoiceMade = localStorage.getItem('keep_screen_on') !== null;
            if (!screenChoiceMade) {
                setShowScreenChoice(true);
            } else {
                // 已经选过了，直接请求权限
                toast.success('✓ 开始使用');
                await requestPermissions();
            }
        } catch (error) {
            console.error('解锁失败:', error);
            toast.error('解锁失败，请再试一次');
        }
    }, [requestPermissions]);

    const handleKeepScreenOn = useCallback(async () => {
        localStorage.setItem('keep_screen_on', 'true');
        toast.success('✓ 已选择保持亮屏');
        setShowScreenChoice(false);
        await requestPermissions();
    }, [requestPermissions]);

    const handleAllowLockScreen = useCallback(async () => {
        localStorage.setItem('keep_screen_on', 'false');
        toast.success('✓ 已选择允许锁屏');
        setShowScreenChoice(false);
        await requestPermissions();
    }, [requestPermissions]);

    const handleSkipScreenChoice = useCallback(async () => {
        // 默认允许锁屏
        localStorage.setItem('keep_screen_on', 'false');
        setShowScreenChoice(false);
        await requestPermissions();
    }, [requestPermissions]);

    const handleModeChange = useCallback((mode: PlayMode) => {
        stopSharedPreview();
        setPlayMode(mode);
        savePlayMode(mode);
        if (mode === "default") {
            setShowTaskForm(false);
        }
    }, [stopSharedPreview]);

    const handleTaskSaved = useCallback(() => {
        stopSharedPreview();
        setShowTaskForm(false);
        setTasksVersion(v => v + 1);
    }, [stopSharedPreview]);

    const handleEditTask = useCallback((task: ScheduledTask) => {
        stopSharedPreview();
        setEditingTask(task);
        setTaskFormSession(session => session + 1);
        setShowTaskForm(true);
    }, [stopSharedPreview]);

    const handleCreateTask = useCallback(() => {
        setEditingTask(null);
        setTaskFormSession(session => session + 1);
        setShowTaskForm(true);
    }, []);

    const handleCloseTaskForm = useCallback(() => {
        stopSharedPreview();
        setShowTaskForm(false);
    }, [stopSharedPreview]);


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

    useEffect(() => {
        if (!mounted) return;
        const executing = getAllTasks().filter(t => t.status === "executing");
        startTaskScheduler().then(() => {
            const resumed = executing.filter(t => getTaskScheduler().getTaskPhase(t.id) !== "idle").map(t => t.name);
            if (resumed.length > 0) toast.success(`${resumed.length} 个任务已恢复执行`, { duration: 3000 });
        });
        return () => stopTaskScheduler();
    }, [mounted]);

    return (
        <div className="min-h-screen text-foreground overflow-x-hidden relative z-10">
            {/* 点击页面任意位置解锁音频 */}
            {!audioUnlocked && mounted && (
                <div
                    className="fixed inset-0 z-50 cursor-pointer"
                    onClick={handleStart}
                />
            )}

            {/* 屏幕选项弹窗 */}
            {showScreenChoice && mounted && (
                <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center" onClick={handleSkipScreenChoice}>
                    <div
                        className="fixed inset-0 bg-black/60 backdrop-blur-md animate-in fade-in duration-200"
                    />
                    <div
                        role="dialog"
                        aria-modal="true"
                        className="relative w-full max-w-md bg-background rounded-t-3xl sm:rounded-3xl shadow-2xl overflow-hidden animate-in slide-in-from-bottom duration-300 sm:zoom-in-90"
                        onClick={e => e.stopPropagation()}
                    >
                        {/* Header */}
                        <div className="relative overflow-hidden">
                            <div className="absolute inset-0 bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/20" />
                            <div className="relative px-6 pt-8 pb-6 text-center">
                                <div className="w-20 h-20 mx-auto mb-4 relative">
                                    <div className="absolute inset-0 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-full animate-pulse opacity-20 scale-150" />
                                    <div className="relative w-full h-full bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-2xl flex items-center justify-center shadow-lg">
                                        <svg className="w-10 h-10 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0a4 4 0 018 0z" />
                                        </svg>
                                    </div>
                                </div>
                                <h3 className="text-2xl font-bold text-foreground">选择播放模式</h3>
                                <p className="text-muted-foreground mt-2">选择助眠音频播放时的屏幕状态</p>
                            </div>
                        </div>

                        {/* Options */}
                        <div className="px-6 pb-2 space-y-3">
                            {/* Keep Screen On */}
                            <button
                                onClick={handleKeepScreenOn}
                                className="w-full group p-4 rounded-2xl border-2 border-border hover:border-[var(--brand-start)]/50 hover:bg-muted/50 transition-all duration-200 text-left"
                            >
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 rounded-xl bg-muted flex items-center justify-center group-hover:bg-[var(--brand-start)]/10 transition-colors">
                                        <svg className="w-6 h-6 text-muted-foreground group-hover:text-[var(--brand-start)] transition-colors" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0a4 4 0 018 0z" />
                                        </svg>
                                    </div>
                                    <div className="flex-1">
                                        <div className="font-semibold text-foreground">保持亮屏</div>
                                        <div className="text-sm text-muted-foreground mt-0.5">播放时屏幕一直亮着，最稳定</div>
                                    </div>
                                    <svg className="w-5 h-5 text-muted-foreground group-hover:text-[var(--brand-start)] transition-colors" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M9 18l6-6-6-6" />
                                    </svg>
                                </div>
                            </button>

                            {/* Allow Lock Screen */}
                            <button
                                onClick={handleAllowLockScreen}
                                className="w-full group p-4 rounded-2xl bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] text-white shadow-lg shadow-[var(--brand-start)]/25 hover:shadow-xl hover:shadow-[var(--brand-start)]/30 transition-all duration-200 text-left"
                            >
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center">
                                        <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                                        </svg>
                                    </div>
                                    <div className="flex-1">
                                        <div className="font-semibold">允许锁屏（省电）</div>
                                        <div className="text-sm opacity-90 mt-0.5">通过锁屏通知控制播放，更省电</div>
                                    </div>
                                    <svg className="w-5 h-5 text-white opacity-90" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M9 18l6-6-6-6" />
                                    </svg>
                                </div>
                            </button>
                        </div>

                        {/* Skip button */}
                        <div className="px-6 pb-8 pt-2">
                            <button
                                onClick={handleSkipScreenChoice}
                                className="w-full py-3 text-muted-foreground hover:text-foreground transition-colors text-sm"
                            >
                                稍后再说（默认省电）
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <DynamicBackground />
            <main className="pt-0 sm:pt-14 relative">
                <section className="relative min-h-[85vh] flex flex-col items-center justify-center px-1 sm:px-6 overflow-hidden">
                    <div className="relative z-20 max-w-4xl mx-auto w-full space-y-6 px-0 sm:px-4 md:px-0">
                        <div className="text-center space-y-4 mt-8">
                            <HeroTitle className="w-[22rem] sm:w-[31rem] max-w-full" fontSize={isMobile ? "76px" : "74px"} />
                            <p className="text-lg text-muted-foreground/70">上传音频 · 自定义定时 · 自动助眠播放</p>
                        </div>

                        {playbackDraftLoaded ? (
                            <PlaybackSettingsPanel
                                controller={sharedPlaybackController}
                                importFileKey={searchParams.get("fileKey") || undefined}
                                mode={playMode}
                                onModeChange={handleModeChange}
                                onAudioUploaded={audioList => {
                                    const last = audioList[audioList.length - 1];
                                    if (last) toast.success(`已添加「${last.name}」`);
                                }}
                                onAudioRemoved={() => { }}
                            >
                                <div className="space-y-5 sm:space-y-6">
                                    <TaskList
                                        tasks={tasks}
                                        onEdit={handleEditTask}
                                        onCreate={handleCreateTask}
                                        onRefresh={() => setTasksVersion(v => v + 1)}
                                    />
                                </div>
                            </PlaybackSettingsPanel>
                        ) : (
                            <div className="flex justify-center py-12"><Spinner /></div>
                        )}
                    </div>
                </section>
            </main>

            <TaskModal visible={showTaskForm} onClose={handleCloseTaskForm}>
                <TaskForm
                    key={taskFormSession}
                    editTask={editingTask}
                    sharedPlaybackController={sharedPlaybackController}
                    active={showTaskForm}
                    onSave={handleTaskSaved}
                    onCancel={handleCloseTaskForm}
                />
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
