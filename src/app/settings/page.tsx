"use client";
import { useState, useCallback, useEffect, useRef, Suspense } from "react";
import Link from "next/link";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { AudioUpload } from "@/components/audio-upload";
import { TaskForm } from "@/components/task-form";
import { TaskList } from "@/components/task-list";
import { TaskModal } from "@/components/task-modal";
import { PlayMode, ScheduledTask } from "@/lib/task-types";
import { getPlayMode, setPlayMode as savePlayMode, getAllTasks, cleanupCompletedOnceTasks, cleanupCancelledTasks, type CleanupResult } from "@/lib/task-store";
import { startTaskScheduler, stopTaskScheduler, getTaskScheduler } from "@/lib/task-scheduler";
import { type Theme } from "@/lib/theme-context";
import DynamicBackground from "@/components/dynamic-background";
import { SchedulerDebugPanel } from "@/components/scheduler-debug-panel";

import {
    Plus,
    Upload,
    Clock,
    Volume2,
} from "lucide-react";

import { cn } from "@/lib/utils";

function usePrecisionReveal(
    options?: {
        threshold?: number;
    }
) {
    const ref = useRef<HTMLDivElement>(null);
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        const element = ref.current;

        if (!element)
            return;

        const observer = new IntersectionObserver(([entry]) => {
            if (entry.isIntersecting) {
                setIsVisible(true);
                observer.unobserve(element);
            }
        }, {
            threshold: options?.threshold ?? 0.1
        });

        observer.observe(element);
        return () => observer.disconnect();
    }, [options?.threshold]);

    return {
        ref,
        isVisible
    };
}

function RevealGroup(
    {
        children,
        className = "",
        delayBase = 0,
        id
    }: {
        children: React.ReactNode;
        className?: string;
        delayBase?: number;
        id?: string;
    }
) {
    const {
        ref,
        isVisible
    } = usePrecisionReveal({
        threshold: 0.08
    });

    return (
        <div ref={ref} id={id} className={cn("space-y-2", className)}>
            {Array.isArray(children) ? children.map((child, i) => <div
                key={i}
                style={{
                    opacity: isVisible ? 1 : 0,
                    transform: isVisible ? "translateY(0)" : "translateY(16px)",
                    transition: `opacity 0.5s ease ${delayBase + i * 80}ms, transform 0.5s ease ${delayBase + i * 80}ms`
                }}>
                {child}
            </div>) : <div
                style={{
                    opacity: isVisible ? 1 : 0,
                    transform: isVisible ? "translateY(0)" : "translateY(16px)",
                    transition: `opacity 0.5s ease ${delayBase}ms, transform 0.5s ease ${delayBase}ms`
                }}>
                {children}
            </div>}
        </div>
    );
}

function WordReveal(
    {
        text,
        className = "",
        wordClassName = "",
        delayBase = 0,
        wordDelay = 150,
        separator = "·"
    }: {
        text: string;
        className?: string;
        wordClassName?: string;
        delayBase?: number;
        wordDelay?: number;
        separator?: string;
    }
) {
    const words = text.split(separator).filter(w => w.trim());

    return (
        <span
            className={cn("inline-flex items-center justify-center gap-3", className)}
            suppressHydrationWarning>
            {words.map((word, i) => <span key={i} className="inline-flex">
                {word.split("").map((char, j) => <span
                    key={j}
                    className={cn(
                        "char-hidden animate-char-reveal text-foreground/60",
                        wordClassName
                    )}
                    style={{
                        '--char-delay': `${delayBase + i * wordDelay + j * 40}ms`
                    } as React.CSSProperties}>
                    {char}
                </span>)}
                {i < words.length - 1 && <span
                    className={cn(
                        "char-hidden animate-char-reveal text-[var(--brand-glow)]/50 mx-2"
                    )}
                    style={{
                        '--char-delay': `${delayBase + i * wordDelay + word.length * 40}ms`
                    } as React.CSSProperties}>
                    {separator}
                </span>}
            </span>)}
        </span>
    );
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
        <div
            className="min-h-screen flex items-center justify-center bg-background"
            suppressHydrationWarning>
            <div className="text-center">
                <Image
                    src="/logo.png"
                    alt="梦枕"
                    width={48}
                    height={48}
                    className="rounded-xl mx-auto mb-4 animate-pulse shadow-[inset_0_2px_6px_rgba(0,0,0,0.35)]"
                    priority
                />
                <p className="text-muted-foreground">加载中...</p>
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

    useEffect(() => {
        setPlayMode(getPlayMode());
        const completedResult = cleanupCompletedOnceTasks();
        const cancelledResult = cleanupCancelledTasks();
        if (completedResult.removedCount > 0) toast.info(`已自动清理 ${completedResult.removedCount} 个已完成的一次性任务`, { duration: 3000 });
        if (cancelledResult.removedCount > 0) toast.info(`已自动清理 ${cancelledResult.removedCount} 个已取消的一次性任务`, { duration: 3000 });
        setTasks(getAllTasks());
    }, []);

    useEffect(() => {
        const executingTasks = getAllTasks().filter(t => t.status === "executing");
        startTaskScheduler().then(() => {
            const resumedNames = executingTasks.filter(t => getTaskScheduler().getTaskPhase(t.id) !== "idle").map(t => t.name);
            if (resumedNames.length > 0) toast.success(`${resumedNames.length} 个任务已恢复执行`, { duration: 3000 });
        });
        return () => { stopTaskScheduler(); };
    }, []);

    useEffect(() => { setTasks(getAllTasks()); }, [tasksVersion]);

    useEffect(() => {
        const handleVisibilityChange = () => {
            if (document.visibilityState !== "visible") return;
            const completedResult = cleanupCompletedOnceTasks();
            const cancelledResult = cleanupCancelledTasks();
            if (completedResult.removedCount > 0) toast.info(`已自动清理 ${completedResult.removedCount} 个已完成的一次性任务`, { duration: 3000 });
            if (cancelledResult.removedCount > 0) toast.info(`已自动清理 ${cancelledResult.removedCount} 个已取消的一次性任务`, { duration: 3000 });
            setTasks(getAllTasks());
        };
        document.addEventListener("visibilitychange", handleVisibilityChange);
        return () => document.removeEventListener("visibilitychange", handleVisibilityChange);
    }, []);

    const handleModeChange = useCallback((mode: PlayMode) => {
        setPlayMode(mode);
        savePlayMode(mode);
        if (mode === "default") {
            setShowTaskForm(false);
            setEditingTask(null);
        }
    }, []);

    const handleTaskSaved = useCallback((_task: ScheduledTask) => {
        setShowTaskForm(false);
        setEditingTask(null);
        setTasksVersion(v => v + 1);
    }, []);

    const handleEditTask = useCallback((task: ScheduledTask) => {
        setEditingTask(task);
        setShowTaskForm(true);
    }, []);

    return (
        <div className="min-h-screen text-foreground overflow-x-hidden relative z-10" suppressHydrationWarning>
            <DynamicBackground />
            <main className="pt-14 relative">
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

                        <div className="flex flex-wrap items-center justify-center gap-3">
                            {[
                                { icon: Upload, text: "音频上传", desc: "私人声音库" },
                                { icon: Clock, text: "定时任务", desc: "按计划播放" },
                                { icon: Volume2, text: "淡入淡出", desc: "减少惊醒" }
                            ].map((item, idx) => <div key={idx} className="group relative flex items-center gap-3 px-5 py-3 rounded-2xl md:bg-background/40 md:backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/50 md:hover:bg-background/70 transition-all duration-300 cursor-default overflow-hidden">
                                <div className="relative flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/20">
                                    <item.icon className="w-5 h-5 text-[var(--brand-glow)]" />
                                </div>
                                <div className="relative flex flex-col">
                                    <span className="text-sm font-medium text-foreground/90">{item.text}</span>
                                    <span className="text-xs text-muted-foreground/60">{item.desc}</span>
                                </div>
                            </div>)}
                        </div>

                        <div className="relative md:bg-background/90 md:backdrop-blur-2xl md:border md:border-border/80 md:rounded-3xl md:overflow-hidden md:shadow-2xl md:shadow-[var(--brand-start)]/10">
                            <div className="hidden sm:block absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)]" />
                            <div className="p-2 sm:p-6 md:p-8">
                                <AudioUpload
                                    importFileKey={searchParams.get("fileKey") || undefined}
                                    mode={playMode}
                                    onModeChange={handleModeChange}
                                    onAudioUploaded={(audioList) => {
                                        const last = audioList[audioList.length - 1];
                                        if (last) toast.success(`「${last.file.name}」上传成功`);
                                    }}
                                    onAudioRemoved={() => {}}
                                >
                                    <div className="space-y-5 sm:space-y-6">
                                        <button
                                            onClick={() => {
                                                setEditingTask(null);
                                                setShowTaskForm(true);
                                            }}
                                            className="w-full relative overflow-hidden px-5 sm:px-5 py-4 sm:py-3.5 rounded-xl font-bold text-sm transition-all duration-300 transform text-[#050510] hover:-translate-y-0.5 cursor-pointer flex items-center justify-center gap-2"
                                            style={{ background: "linear-gradient(135deg, #00d4aa 0%, #00b894 50%, #00d4aa 100%)", boxShadow: "0 4px 15px rgba(0, 212, 170, 0.3)" }}
                                        >
                                            <Plus className="w-4 h-4" />
                                            新建任务
                                        </button>
                                        <TaskList tasks={tasks} onEdit={handleEditTask} onRefresh={() => setTasksVersion(v => v + 1)} />
                                    </div>
                                </AudioUpload>
                            </div>
                        </div>
                    </div>
                </section>
            </main>

            <TaskModal visible={showTaskForm} onClose={() => {
                setShowTaskForm(false);
                setEditingTask(null);
            }}>
                <TaskForm editTask={editingTask} onSave={handleTaskSaved} onCancel={() => {
                    setShowTaskForm(false);
                    setEditingTask(null);
                }} />
            </TaskModal>

            <footer className="border-t border-border py-8 px-6 bg-muted/20 relative z-20">
                <div className="max-w-5xl mx-auto text-center">
                    <div className="flex items-center justify-center gap-2 mb-3">
                        <Image src="/logo.png" alt="梦枕" width={20} height={20} className="rounded-md shadow-[inset_0_1px_4px_rgba(0,0,0,0.35)]" />
                        <span className="font-bold text-lg bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent">梦枕</span>
                    </div>
                    <p className="text-xs text-muted-foreground">深夜助眠播放器 · PWA渐进式网页应用 · 自定义音频</p>
                </div>
            </footer>
            <SchedulerDebugPanel />
        </div>
    );
}