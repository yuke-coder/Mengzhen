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
import UnifiedAudioManager, { setupAutoUnlock } from "@/lib/audio";
import EnhancedTaskScheduler from "@/lib/background-scheduler";
import { initPwaInstallListener, promptInstall, isPwaInstalled, hasPromptedInstall } from "@/lib/pwa";

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
    const [showPwaPrompt, setShowPwaPrompt] = useState(false);
    const [isPwa, setIsPwa] = useState(false);
    const mounted = useClientOnly();

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

    // 检查权限状态
    useEffect(() => {
        if (!mounted || !audioUnlocked) return;

        const permissionsDone = localStorage.getItem('permissions_done') === 'true';
        if (permissionsDone) return;

        // 直接请求浏览器原生权限
        const requestPermissions = async () => {
            try {
                // 通知权限
                if ('Notification' in window && Notification.permission !== 'granted') {
                    await Notification.requestPermission();
                }
                // 存储权限
                if (navigator.storage && navigator.storage.persist) {
                    await navigator.storage.persist();
                }
                localStorage.setItem('permissions_done', 'true');
            } catch (e) {
                console.error('[Permission]', e);
            }
        };

        requestPermissions();
    }, [mounted, audioUnlocked]);

    // PWA
    useEffect(() => {
        if (!mounted) return;
        setIsPwa(isPwaInstalled());
    }, [mounted]);

    useEffect(() => {
        if (!mounted || !audioUnlocked || isPwa || hasPromptedInstall()) return;
        const cleanup = initPwaInstallListener(() => {
            setTimeout(() => setShowPwaPrompt(true), 2000);
        });
        return cleanup;
    }, [mounted, audioUnlocked, isPwa]);

    const handleStart = useCallback(async () => {
        try {
            await EnhancedTaskScheduler.getInstance().initializeAudioContext();
            setAudioUnlocked(true);
            localStorage.setItem('audio_unlocked', 'true');
            localStorage.setItem('keep_screen_on', 'false');
            toast.success('✓ 开始使用');

            // 直接请求浏览器原生权限弹窗
            if ('Notification' in window && Notification.permission !== 'granted') {
                await Notification.requestPermission();
            }
            if (navigator.storage && navigator.storage.persist) {
                await navigator.storage.persist();
            }
            localStorage.setItem('permissions_done', 'true');
        } catch (error) {
            console.error('解锁失败:', error);
            toast.error('解锁失败，请再试一次');
        }
    }, []);

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

    const handleInstallPwa = useCallback(async () => {
        try {
            const success = await promptInstall();
            if (success) {
                toast.success('🎉 太棒了！梦枕已安装到桌面');
                setShowPwaPrompt(false);
            } else {
                toast.info('下次再说也可以～');
                setShowPwaPrompt(false);
            }
        } catch (error) {
            console.error('PWA 安装失败:', error);
            toast.error('安装失败，请稍后再试');
        }
    }, []);

    const handleLaterPwa = useCallback(() => {
        setShowPwaPrompt(false);
        toast.info('好的，下次再提示你～');
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
            {/* 只显示一个开始按钮 */}
            {!audioUnlocked && mounted && (
                <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-black/85 backdrop-blur-sm animate-in fade-in duration-300">
                    <div className="text-center space-y-6 px-4 max-w-md w-full">
                        <div className="w-24 h-24 mx-auto mb-4 relative">
                            <div className="absolute inset-0 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-full animate-pulse opacity-30" />
                            <div className="relative w-full h-full rounded-full flex items-center justify-center bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)]">
                                <svg className="w-12 h-12 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M9 18V6L3 12V18H9Z" />
                                    <path d="M15 6L18 12L15 18" />
                                    <path d="M21 18V6L18 12" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-white">欢迎使用梦枕</h2>
                        <p className="text-white/70 text-base leading-relaxed">点击开始使用</p>
                        <button
                            onClick={handleStart}
                            className="px-10 py-4 text-lg bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-lg text-white font-bold hover:opacity-90 active:opacity-80 transition-all shadow-lg hover:shadow-xl active:scale-95"
                        >
                            开始使用
                        </button>
                    </div>
                </div>
            )}

            {/* 调试 */}
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

            {/* PWA */}
            {showPwaPrompt && mounted && !isPwa && (
                <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-black/85 backdrop-blur-sm animate-in fade-in duration-300">
                    <div className="text-center space-y-6 px-4 max-w-md w-full">
                        <div className="w-24 h-24 mx-auto mb-4 relative">
                            <div className="absolute inset-0 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-2xl animate-pulse opacity-30"></div>
                            <div className="relative w-full h-full bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-2xl flex items-center justify-center shadow-2xl">
                                <Image src="/logo.png" alt="梦枕" width={64} height={64} className="rounded-2xl" />
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-white">安装到桌面</h2>
                        <p className="text-white/70 text-base leading-relaxed">
                            把梦枕安装到手机桌面，就像原生 APP 一样！<br />更稳定，体验更好～
                        </p>
                        <div className="bg-white/5 rounded-xl p-4 space-y-2 text-left">
                            <div className="flex items-center gap-3 text-white/80 text-sm">
                                <svg className="w-5 h-5 text-[var(--brand-start)] flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
                                </svg>
                                <span>更快的启动速度</span>
                            </div>
                            <div className="flex items-center gap-3 text-white/80 text-sm">
                                <svg className="w-5 h-5 text-[var(--brand-start)] flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2" />
                                </svg>
                                <span>像普通 APP 一样使用</span>
                            </div>
                            <div className="flex items-center gap-3 text-white/80 text-sm">
                                <svg className="w-5 h-5 text-[var(--brand-start)] flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                                </svg>
                                <span>更好的后台播放支持</span>
                            </div>
                        </div>
                        <div className="flex justify-center gap-4 pt-2">
                            <button
                                onClick={handleLaterPwa}
                                className="px-6 py-3 bg-white/10 rounded-lg text-white/70 font-medium hover:bg-white/20 transition-all"
                            >
                                稍后再说
                            </button>
                            <button
                                onClick={handleInstallPwa}
                                className="px-8 py-3 bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] rounded-lg text-white font-bold hover:opacity-90 active:opacity-80 transition-all shadow-lg"
                            >
                                安装到桌面
                            </button>
                        </div>
                    </div>
                </div>
            )}

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
