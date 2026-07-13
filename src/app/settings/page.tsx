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
import { PlayMode, ScheduledTask, type PlaybackDraft } from "@/lib/task-types";
import { getPlayMode, setPlayMode as savePlayMode, getAllTasks, cleanupCompletedOnceTasks, cleanupCancelledTasks } from "@/lib/task-store";
import { EMPTY_PLAYBACK_DRAFT, getDefaultPlaybackDraft, saveDefaultPlaybackDraft } from "@/lib/playback-draft";
import { startTaskScheduler, stopTaskScheduler, getTaskScheduler } from "@/lib/task-scheduler";
import DynamicBackground from "@/components/dynamic-background";
import { setupAutoUnlock } from "@/lib/audio";
import EnhancedTaskScheduler from "@/lib/background-scheduler";
import { initPwaInstallListener, promptInstall, isPwaInstalled, hasPromptedInstall } from "@/lib/pwa";
import { useIsMobile } from "@/hooks/use-mobile";
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
    const [isPwa, setIsPwa] = useState(false);
    const mounted = useClientOnly();

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


    // PWA - 只提示一次浏览器原生安装
    useEffect(() => {
        if (!mounted) return;
        setIsPwa(isPwaInstalled());
    }, [mounted]);

    useEffect(() => {
        if (!mounted || !audioUnlocked || isPwa || hasPromptedInstall()) return;

        // 只在第一次初始化监听器，并主动触发浏览器原生安装提示
        const cleanup = initPwaInstallListener(() => {
            setTimeout(async () => {
                try {
                    if (!hasPromptedInstall()) {
                        await promptInstall();
                    }
                } catch (e) {
                    console.error('[PWA]', e);
                }
            }, 2000);
        });
        return cleanup;
    }, [mounted, audioUnlocked, isPwa]);


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
        setPlayMode(mode);
        savePlayMode(mode);
        if (mode === "default") {
            setShowTaskForm(false);
        }
    }, []);

    const handleTaskSaved = useCallback(() => {
        setShowTaskForm(false);
        setTasksVersion(v => v + 1);
    }, []);

    const handleEditTask = useCallback((task: ScheduledTask) => {
        setEditingTask(task);
        setTaskFormSession(session => session + 1);
        setShowTaskForm(true);
    }, []);

    const handleCreateTask = useCallback(() => {
        setEditingTask(null);
        setTaskFormSession(session => session + 1);
        setShowTaskForm(true);
    }, []);

    const handleCloseTaskForm = useCallback(() => {
        setShowTaskForm(false);
    }, []);


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

            {/* 屏幕选项弹窗 - 极简 */}
            {showScreenChoice && mounted && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-6" onClick={handleSkipScreenChoice}>
                    <div className="fixed inset-0 bg-black/50" />
                    <div
                        role="dialog"
                        aria-modal="true"
                        className="relative w-full max-w-sm bg-background rounded-xl"
                        onClick={e => e.stopPropagation()}
                    >
                        <div className="p-6 space-y-4">
                            <h3 className="text-lg font-medium text-center">屏幕设置</h3>

                            <button
                                onClick={handleAllowLockScreen}
                                className="w-full py-3 text-center bg-foreground text-background rounded-lg hover:opacity-90 transition-opacity"
                            >
                                允许锁屏
                            </button>

                            <button
                                onClick={handleKeepScreenOn}
                                className="w-full py-3 text-center border border-border rounded-lg hover:bg-muted/50 transition-colors"
                            >
                                保持亮屏
                            </button>

                            <button
                                onClick={handleSkipScreenChoice}
                                className="w-full py-2 text-center text-muted-foreground text-sm"
                            >
                                稍后
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <main className="pt-14">
                <section className="min-h-[80vh] flex flex-col items-center justify-center px-4">
                    <div className="max-w-2xl w-full space-y-8">
                        <div className="text-center space-y-2">
                            <HeroTitle className="w-64 sm:w-80 max-w-full mx-auto" fontSize={isMobile ? "64px" : "64px"} />
                            <p className="text-muted-foreground">助眠音频播放器</p>
                        </div>

                        {playbackDraftLoaded ? (
                            <AudioUpload
                                playbackDraft={sharedPlaybackDraft}
                                onPlaybackDraftChange={setSharedPlaybackDraft}
                                importFileKey={searchParams.get("fileKey") || undefined}
                                mode={playMode}
                                onModeChange={handleModeChange}
                                onAudioUploaded={audioList => {
                                    const last = audioList[audioList.length - 1];
                                    if (last) toast.success(`「${last.name}」上传成功`);
                                }}
                                onAudioRemoved={() => { }}
                            >
                                <div className="space-y-4">
                                    <TaskList
                                        tasks={tasks}
                                        onEdit={handleEditTask}
                                        onCreate={handleCreateTask}
                                        onRefresh={() => setTasksVersion(v => v + 1)}
                                    />
                                </div>
                            </AudioUpload>
                        ) : (
                            <div className="flex justify-center py-8"><Spinner /></div>
                        )}
                    </div>
                </section>
            </main>

            <TaskModal visible={showTaskForm} onClose={handleCloseTaskForm}>
                <TaskForm
                    key={taskFormSession}
                    editTask={editingTask}
                    sharedPlaybackDraft={sharedPlaybackDraft}
                    onSharedPlaybackDraftChange={setSharedPlaybackDraft}
                    onSave={handleTaskSaved}
                    onCancel={handleCloseTaskForm}
                />
            </TaskModal>
        </div>
    );
}
