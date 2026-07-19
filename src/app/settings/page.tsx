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
import { useIsMobile } from "@/hooks/use-mobile";
import { usePlaybackController } from "@/hooks/use-playback-controller";
import { HeroTitle } from "@/components/hero-title";
import { syncTasksToNative, isNativeEnvironment } from "@/lib/native-scheduler";

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
        const alreadyUnlocked = localStorage.getItem('audio_unlocked') === 'true';
        if (alreadyUnlocked) setAudioUnlocked(true);
        return () => stopTaskScheduler();
    }, [mounted]);

    // 自动解锁（原生环境不需要）
    useEffect(() => {
        if (!mounted || isNativeEnvironment()) return;
        return setupAutoUnlock();
    }, [mounted]);

    // 任务变更时同步到原生 AlarmScheduler
    useEffect(() => {
        if (!mounted) return;
        if (isNativeEnvironment()) {
            syncTasksToNative();
        }
        try { getTaskScheduler().refreshSchedule(); } catch {}
    }, [mounted, tasksVersion]);

    // 页面可见时清理过期任务
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

    // 启动调度器
    useEffect(() => {
        if (!mounted) return;
        const executing = getAllTasks().filter(t => t.status === "executing");
        startTaskScheduler().then(() => {
            const resumed = executing.filter(t => getTaskScheduler().getTaskPhase(t.id) !== "idle").map(t => t.name);
            if (resumed.length > 0) toast.success(`${resumed.length} 个任务已恢复执行`, { duration: 3000 });
        });
        return () => stopTaskScheduler();
    }, [mounted]);

    useEffect(() => {
        if (!mounted) return;
        setTasks(getAllTasks());
    }, [tasksVersion, mounted]);

    const requestPermissions = useCallback(async () => {
        try {
            if ('Notification' in window && Notification.permission === 'default') {
                await Notification.requestPermission();
            }
        } catch {}
        try {
            if (navigator.storage?.persisted) {
                const ok = await navigator.storage.persisted();
                if (!ok && navigator.storage.persist) await navigator.storage.persist();
            }
        } catch {}
    }, []);

    const handleStart = useCallback(async () => {
        try {
            setAudioUnlocked(true);
            localStorage.setItem('audio_unlocked', 'true');
            await requestPermissions();
            toast.success('✓ 开始使用');
        } catch (error) {
            console.error('解锁失败:', error);
            toast.error('解锁失败，请再试一次');
        }
    }, [requestPermissions]);

    const handleModeChange = useCallback((mode: PlayMode) => {
        stopSharedPreview();
        setPlayMode(mode);
        savePlayMode(mode);
        if (mode === "default") setShowTaskForm(false);
    }, [stopSharedPreview]);

    const handleTaskSaved = useCallback(() => {
        stopSharedPreview();
        setShowTaskForm(false);
        setTasksVersion(v => v + 1);
    }, [stopSharedPreview]);

    const handleEditTask = useCallback((task: ScheduledTask) => {
        stopSharedPreview();
        setEditingTask(task);
        setTaskFormSession(s => s + 1);
        setShowTaskForm(true);
    }, [stopSharedPreview]);

    const handleCreateTask = useCallback(() => {
        setEditingTask(null);
        setTaskFormSession(s => s + 1);
        setShowTaskForm(true);
    }, []);

    const handleCloseTaskForm = useCallback(() => {
        stopSharedPreview();
        setShowTaskForm(false);
    }, [stopSharedPreview]);

    return (
        <div className="min-h-screen text-foreground overflow-x-hidden relative z-10">
            {!audioUnlocked && mounted && (
                <div className="fixed inset-0 z-50 cursor-pointer" onClick={handleStart} />
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
                                onAudioRemoved={() => {}}
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
                    <p className="text-xs text-muted-foreground">深夜助眠播放器 · 自定义音频</p>
                </div>
            </footer>
        </div>
    );
}
