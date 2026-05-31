"use client";

import {
    Brain,
    Wand2,
    Layers,
    Download,
    Zap,
    Shield,
    FileText,
    Smartphone,
    Monitor,
    Sparkles,
    GraduationCap,
    Briefcase,
    Database,
    Cookie,
    Battery,
    RefreshCw,
    Lock,
    Heart,
    Users,
    Calendar,
    WifiOff,
    Gift,
    Crown,
} from "lucide-react";

import { cn } from "@/lib/utils";
import { useTheme, type Theme } from "@/lib/theme-context";
import RippleButton from "@/components/RippleButton";
import { MindMapTemplate } from "@/lib/mindmap-types";
import { TemplateSelector } from "@/components/template-selector";

type IconComponent = React.ComponentType<{ className?: string }>;

function usePrecisionReveal(options?: { threshold?: number }) {
    const ref = useRef<HTMLDivElement>(null);
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        const element = ref.current;
        if (!element) return;

        const observer = new IntersectionObserver(([entry]) => {
            if (entry.isIntersecting) {
                setIsVisible(true);
                observer.unobserve(element);
            }
        }, { threshold: options?.threshold ?? 0.1 });

        observer.observe(element);
        return () => observer.disconnect();
    }, [options?.threshold]);

    return { ref, isVisible };
}

function RevealGroup({ children, className = "", delayBase = 0, id }: {
    children: React.ReactNode;
    className?: string;
    delayBase?: number;
    id?: string;
}) {
    const { ref, isVisible } = usePrecisionReveal({ threshold: 0.08 });

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

import { useState, useCallback, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";

interface HomePageBelowFoldProps {
    selectedTemplates: MindMapTemplate[];
    setSelectedTemplates: (templates: MindMapTemplate[]) => void;
    recommendedTemplates: MindMapTemplate[];
    setRecommendedTemplates: (templates: MindMapTemplate[]) => void;
    isRecommending: boolean;
    setIsRecommending: (v: boolean) => void;
}

export default function HomePageBelowFold({
    selectedTemplates,
    setSelectedTemplates,
    recommendedTemplates,
    setRecommendedTemplates,
    isRecommending,
    setIsRecommending,
}: HomePageBelowFoldProps) {
    const router = useRouter();
    const { setTheme } = useTheme();

    return (
        <>
            <section id="features" className="py-32 px-6 relative overflow-hidden">
                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                    <div className="absolute top-0 left-1/4 w-96 h-96 rounded-full bg-gradient-radial from-[var(--brand-glow)]/8 via-transparent to-transparent blur-3xl" />
                    <div className="absolute bottom-0 right-1/4 w-80 h-80 rounded-full bg-gradient-radial from-[var(--brand-end)]/5 via-transparent to-transparent blur-3xl" />
                </div>
                <div className="max-w-6xl mx-auto relative z-10">
                    <RevealGroup className="text-center mb-20" delayBase={0}>
                        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[var(--brand-glow)]/10 border border-[var(--brand-glow)]/20 mb-6">
                            <span className="w-1.5 h-1.5 rounded-full bg-[var(--brand-glow)] animate-pulse" />
                            <span suppressHydrationWarning className="text-[var(--brand-glow)] text-sm font-medium">核心优势</span>
                        </div>
                        <h2 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
                            <span suppressHydrationWarning className="text-foreground/90">专为中国浅眠人群</span>
                            <span className="block bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>匠心打造</span>
                        </h2>
                        <p className="text-lg text-muted-foreground/70 max-w-2xl mx-auto">深度适配睡眠浅、对音量突变敏感、半夜易醒的用户群体</p>
                    </RevealGroup>

                    <div className="grid md:grid-cols-2 gap-6 mb-16">
                        <div className="group relative flex items-start gap-4 p-5 rounded-2xl border border-[var(--brand-glow)]/20 hover:border-[var(--brand-glow)]/40 hover:shadow-lg hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-0.5 transition-all duration-300"
                            style={{ background: `radial-gradient(circle 80% 70% at 10% 20%, rgba(34, 211, 170, 0.35), transparent), radial-gradient(circle 60% 80% at 90% 30%, rgba(6, 182, 212, 0.25), transparent), radial-gradient(circle 70% 50% at 30% 80%, rgba(16, 185, 129, 0.2), transparent), radial-gradient(circle 50% 60% at 70% 90%, rgba(0, 212, 170, 0.15), transparent), radial-gradient(circle 90% 40% at 50% 50%, rgba(20, 184, 166, 0.1), transparent)` }}>
                            <div className="absolute -top-2 -right-2 px-2 py-0.5 rounded-full bg-[var(--brand-glow)]/20 text-[var(--brand-glow)] text-xs font-medium">专为浅眠人群</div>
                            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/10 flex items-center justify-center shrink-0">
                                <Monitor className="w-6 h-6 text-[var(--brand-glow)]" />
                            </div>
                            <div>
                                <h3 className="text-lg font-semibold text-foreground/90 mb-2">睡眠深度较浅</h3>
                                <p className="text-sm text-muted-foreground/70 leading-relaxed">对音量突变敏感，音频启停稍有不慎便会彻底惊醒，难以再次入睡</p>
                            </div>
                        </div>
                        <div className="group relative flex items-start gap-4 p-5 rounded-2xl border border-[var(--brand-glow)]/20 hover:border-[var(--brand-glow)]/40 hover:shadow-lg hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-0.5 transition-all duration-300"
                            style={{ background: `radial-gradient(circle 90% 60% at 85% 15%, rgba(251, 191, 36, 0.35), transparent), radial-gradient(circle 70% 80% at 15% 25%, rgba(249, 115, 22, 0.25), transparent), radial-gradient(circle 60% 70% at 40% 85%, rgba(245, 158, 11, 0.2), transparent), radial-gradient(circle 80% 50% at 75% 75%, rgba(234, 179, 8, 0.15), transparent), radial-gradient(circle 50% 90% at 20% 60%, rgba(202, 138, 4, 0.1), transparent)` }}>
                            <div className="absolute -top-2 -right-2 px-2 py-0.5 rounded-full bg-[var(--brand-glow)]/20 text-[var(--brand-glow)] text-xs font-medium">专为浅眠人群</div>
                            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/10 flex items-center justify-center shrink-0">
                                <Monitor className="w-6 h-6 text-[var(--brand-glow)]" />
                            </div>
                            <div>
                                <h3 className="text-lg font-semibold text-foreground/90 mb-2">音量突变惊醒</h3>
                                <p className="text-sm text-muted-foreground/70 leading-relaxed">精准音量渐入渐出自定义，彻底规避音频启停音量骤变惊醒用户的问题</p>
                            </div>
                        </div>
                        <div className="group relative flex items-start gap-4 p-5 rounded-2xl border border-[var(--brand-glow)]/20 hover:border-[var(--brand-glow)]/40 hover:shadow-lg hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-0.5 transition-all duration-300"
                            style={{ background: `radial-gradient(circle 70% 80% at 55% 10%, rgba(236, 72, 153, 0.35), transparent), radial-gradient(circle 80% 60% at 35% 75%, rgba(244, 63, 94, 0.25), transparent), radial-gradient(circle 60% 90% at 85% 45%, rgba(225, 29, 72, 0.2), transparent), radial-gradient(circle 90% 70% at 15% 85%, rgba(190, 24, 93, 0.15), transparent), radial-gradient(circle 50% 50% at 65% 35%, rgba(219, 39, 119, 0.1), transparent)` }}>
                            <div className="absolute -top-2 -right-2 px-2 py-0.5 rounded-full bg-[var(--brand-glow)]/20 text-[var(--brand-glow)] text-xs font-medium">专为浅眠人群</div>
                            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/10 flex items-center justify-center shrink-0">
                                <Calendar className="w-6 h-6 text-[var(--brand-glow)]" />
                            </div>
                            <div>
                                <h3 className="text-lg font-semibold text-foreground/90 mb-2">深夜操作困难</h3>
                                <p className="text-sm text-muted-foreground/70 leading-relaxed">半夜醒来后睡意朦胧，不愿手动操作手机，一键预设定时播放完美适配</p>
                            </div>
                        </div>
                    </div>

                    <div className="text-center mb-8">
                        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[var(--brand-glow)]/10 text-sm text-[var(--brand-glow)] mb-4">
                            <Zap className="w-4 h-4" />
                            核心价值
                        </div>
                        <h3 className="text-2xl font-bold text-foreground/90 mb-2">夜间觉醒后接续睡眠</h3>
                        <p className="text-muted-foreground/60">不求辅助入眠，只为中途觉醒后快速重新入睡</p>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-12">
                        {[
                            { icon: RefreshCw, title: "觉醒自动续播", desc: "夜间醒来后，柔和音频无缝衔接，帮助快速重新入睡", gradient: "radial-gradient(ellipse 70% 60% at 20% 30%, rgba(34, 211, 170, 0.25), transparent), radial-gradient(ellipse 60% 50% at 80% 70%, rgba(6, 182, 212, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(16, 185, 129, 0.15), transparent)" },
                            { icon: Volume2, title: "零突变音量", desc: "全程音量渐入渐出，彻底规避惊醒风险，营造柔和睡眠氛围", gradient: "radial-gradient(ellipse 70% 60% at 30% 40%, rgba(16, 185, 129, 0.25), transparent), radial-gradient(ellipse 60% 50% at 70% 80%, rgba(20, 184, 166, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(5, 150, 105, 0.15), transparent)" },
                            { icon: Monitor, title: "黑屏后台播放", desc: "锁屏休眠持续播放，不干扰睡眠，支持定时自动停止", gradient: "radial-gradient(ellipse 70% 60% at 40% 20%, rgba(139, 92, 246, 0.25), transparent), radial-gradient(ellipse 60% 50% at 60% 80%, rgba(124, 58, 237, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(109, 40, 217, 0.15), transparent)" }
                        ].map((item, idx) => (
                            <div key={idx} className="group relative p-5 rounded-2xl backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/40 shadow-lg hover:shadow-xl hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-1 transition-all duration-300 overflow-hidden"
                                style={{ background: item.gradient }}>
                                <div className="absolute inset-0 bg-gradient-to-br from-[var(--brand-glow)]/8 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                                <div className="relative flex items-center gap-3 mb-3">
                                    <div className="w-10 h-10 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                                        <item.icon className="w-5 h-5 text-[var(--brand-glow)]" />
                                    </div>
                                    <h4 className="font-semibold text-foreground/90 group-hover:text-[var(--brand-glow)] transition-colors duration-300">{item.title}</h4>
                                </div>
                                <p className="relative text-sm text-muted-foreground/70 group-hover:text-muted-foreground/90 transition-colors duration-300">{item.desc}</p>
                            </div>
                        ))}
                    </div>

                    <div className="text-center mb-8">
                        <h3 className="text-2xl font-bold text-foreground/90 mb-2">个性化音频配置</h3>
                        <p className="text-muted-foreground/60">精细化音量控制，适配个人听觉耐受度</p>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-12">
                        {[
                            { icon: Music, title: "全格式兼容", desc: "MP3、WAV、FLAC 等全主流音频格式", gradient: "radial-gradient(ellipse 70% 60% at 20% 30%, rgba(34, 211, 238, 0.25), transparent), radial-gradient(ellipse 60% 50% at 80% 70%, rgba(59, 130, 246, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(14, 165, 233, 0.15), transparent)" },
                            { icon: Headphones, title: "在线试听", desc: "上传后实时预览，快速筛选适配音频", gradient: "radial-gradient(ellipse 70% 60% at 30% 40%, rgba(14, 165, 233, 0.25), transparent), radial-gradient(ellipse 60% 50% at 70% 80%, rgba(99, 102, 241, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(79, 70, 229, 0.15), transparent)" },
                            { icon: Volume2, title: "小数级音量", desc: "0-100% 精细化分级，支持 0.1 微调", gradient: "radial-gradient(ellipse 70% 60% at 40% 20%, rgba(168, 85, 247, 0.25), transparent), radial-gradient(ellipse 60% 50% at 60% 80%, rgba(139, 92, 246, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(124, 58, 237, 0.15), transparent)" },
                            { icon: Layers, title: "播放列表", desc: "自定义音频播放顺序，编排专属播放列表", gradient: "radial-gradient(ellipse 70% 60% at 20% 50%, rgba(251, 191, 36, 0.25), transparent), radial-gradient(ellipse 60% 50% at 80% 30%, rgba(249, 115, 22, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(245, 158, 11, 0.15), transparent)" },
                            { icon: Settings2, title: "全面自定义", desc: "定时、渐变音量、播放规则全部可调", gradient: "radial-gradient(ellipse 70% 60% at 30% 20%, rgba(236, 72, 153, 0.25), transparent), radial-gradient(ellipse 60% 50% at 70% 80%, rgba(244, 63, 94, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(225, 29, 72, 0.15), transparent)" },
                            { icon: Calendar, title: "周期定时", desc: "每日/工作日重复定时，适配长期规律睡眠", gradient: "radial-gradient(ellipse 70% 60% at 20% 30%, rgba(16, 185, 129, 0.25), transparent), radial-gradient(ellipse 60% 50% at 80% 70%, rgba(20, 184, 166, 0.2), transparent), radial-gradient(ellipse 50% 40% at 50% 50%, rgba(5, 150, 105, 0.15), transparent)" }
                        ].map((item, idx) => (
                            <div key={idx} className="group relative p-5 rounded-2xl backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/40 shadow-lg hover:shadow-xl hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-1 transition-all duration-300 overflow-hidden"
                                style={{ background: item.gradient }}>
                                <div className="absolute inset-0 bg-gradient-to-br from-[var(--brand-glow)]/8 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                                <div className="relative flex items-center gap-3 mb-3">
                                    <div className="w-10 h-10 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                                        <item.icon className="w-5 h-5 text-[var(--brand-glow)]" />
                                    </div>
                                    <h4 className="font-semibold text-foreground/90 group-hover:text-[var(--brand-glow)] transition-colors duration-300">{item.title}</h4>
                                </div>
                                <p className="relative text-sm text-muted-foreground/70 group-hover:text-muted-foreground/90 transition-colors duration-300">{item.desc}</p>
                            </div>
                        ))}
                    </div>

                    <div className="mb-12">
                        <div className="text-center mb-8">
                            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/10 mb-4">
                                <Smartphone className="w-8 h-8 text-[var(--brand-glow)]" />
                            </div>
                            <h3 className="text-2xl font-bold text-foreground/90 mb-2">后台稳定播放</h3>
                            <p className="text-muted-foreground/60">夜间锁屏休眠持续播放，不中断接续睡眠</p>
                        </div>
                        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
                            {[
                                { icon: Zap, title: "后台唤醒", desc: "锁屏休眠仍可定时唤醒正常播放", gradient: "radial-gradient(ellipse 80% 70% at 20% 30%, rgba(251, 191, 36, 0.3), transparent), radial-gradient(ellipse 60% 80% at 80% 70%, rgba(249, 115, 22, 0.25), transparent), radial-gradient(ellipse 70% 50% at 40% 85%, rgba(234, 179, 8, 0.2), transparent), radial-gradient(ellipse 90% 60% at 60% 40%, rgba(202, 138, 4, 0.15), transparent)" },
                                { icon: Battery, title: "电池优化", desc: "忽略电池优化引导，提升休眠稳定性", gradient: "radial-gradient(ellipse 80% 70% at 30% 20%, rgba(168, 85, 247, 0.3), transparent), radial-gradient(ellipse 60% 80% at 70% 80%, rgba(139, 92, 246, 0.25), transparent), radial-gradient(ellipse 70% 50% at 50% 50%, rgba(124, 58, 237, 0.2), transparent), radial-gradient(ellipse 90% 60% at 20% 60%, rgba(147, 51, 234, 0.15), transparent)" },
                                { icon: RefreshCw, title: "异常兜底", desc: "系统杀进程后可自动重试唤醒", gradient: "radial-gradient(ellipse 80% 70% at 25% 35%, rgba(236, 72, 153, 0.3), transparent), radial-gradient(ellipse 60% 80% at 75% 65%, rgba(244, 63, 94, 0.25), transparent), radial-gradient(ellipse 70% 50% at 45% 80%, rgba(225, 29, 72, 0.2), transparent), radial-gradient(ellipse 90% 60% at 65% 25%, rgba(190, 24, 93, 0.15), transparent)" },
                                { icon: WifiOff, title: "离线模式", desc: "断网网络不佳时定时播放正常", gradient: "radial-gradient(ellipse 80% 70% at 20% 25%, rgba(14, 165, 233, 0.3), transparent), radial-gradient(ellipse 60% 80% at 80% 75%, rgba(99, 102, 241, 0.25), transparent), radial-gradient(ellipse 70% 50% at 50% 45%, rgba(79, 70, 229, 0.2), transparent), radial-gradient(ellipse 90% 60% at 30% 70%, rgba(99, 102, 241, 0.15), transparent)" }
                            ].map((item, idx) => (
                                <div key={idx} className="group relative text-center p-4 rounded-xl backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/40 shadow-lg hover:shadow-xl hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-0.5 transition-all duration-300 overflow-hidden"
                                    style={{ background: item.gradient }}>
                                    <div className="absolute inset-0 bg-gradient-to-br from-[var(--brand-glow)]/5 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                                    <div className="relative w-10 h-10 rounded-lg bg-[var(--brand-glow)]/10 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                        <item.icon className="w-5 h-5 text-[var(--brand-glow)]" />
                                    </div>
                                    <h4 className="relative font-medium text-foreground/90 text-sm mb-1 group-hover:text-[var(--brand-glow)] transition-colors duration-300">{item.title}</h4>
                                    <p className="relative text-xs text-muted-foreground/60 group-hover:text-muted-foreground/80 transition-colors duration-300">{item.desc}</p>
                                </div>
                            ))}
                        </div>
                        <div className="mt-6 text-center">
                            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-[var(--brand-glow)]/10 text-sm">
                                <Monitor className="w-4 h-4 text-[var(--brand-glow)]" />
                                <span className="text-muted-foreground/80">可添加至手机桌面，像原生 App 一样使用</span>
                            </div>
                        </div>
                    </div>

                    <div className="text-center mb-8">
                        <h3 className="text-2xl font-bold text-foreground/90 mb-2">分层数据存储方案</h3>
                        <p className="text-muted-foreground/60">兼顾云端便捷性与本地隐私安全</p>
                    </div>
                    <div className="grid md:grid-cols-3 gap-4 mb-12">
                        <div className="group relative p-6 rounded-2xl backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/40 shadow-lg hover:shadow-xl hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-2 transition-all duration-400 ease-out overflow-hidden"
                            style={{ background: "radial-gradient(ellipse 80% 70% at 20% 30%, rgba(14, 165, 233, 0.3), transparent), radial-gradient(ellipse 60% 80% at 80% 70%, rgba(59, 130, 246, 0.25), transparent), radial-gradient(ellipse 70% 50% at 40% 85%, rgba(34, 211, 238, 0.2), transparent), radial-gradient(ellipse 90% 60% at 60% 40%, rgba(6, 182, 212, 0.15), transparent)" }}>
                            <div className="absolute inset-0 bg-gradient-to-br from-sky-500/8 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                            <div className="relative w-12 h-12 rounded-xl bg-gradient-to-br from-sky-500/20 to-blue-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                <Database className="w-6 h-6 text-sky-400" />
                            </div>
                            <h4 className="relative text-lg font-semibold text-foreground/90 mb-2 group-hover:text-sky-400 transition-colors duration-300">云端数据库</h4>
                            <p className="relative text-sm text-muted-foreground/70 leading-relaxed">音频文件统一存入云端数据库，跨设备同步无缝使用</p>
                        </div>
                        <div className="group relative p-6 rounded-2xl backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/40 shadow-lg hover:shadow-xl hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-2 transition-all duration-400 ease-out overflow-hidden"
                            style={{ background: "radial-gradient(ellipse 80% 70% at 30% 20%, rgba(251, 191, 36, 0.3), transparent), radial-gradient(ellipse 60% 80% at 70% 80%, rgba(249, 115, 22, 0.25), transparent), radial-gradient(ellipse 70% 50% at 50% 50%, rgba(234, 179, 8, 0.2), transparent), radial-gradient(ellipse 90% 60% at 20% 70%, rgba(202, 138, 4, 0.15), transparent)" }}>
                            <div className="absolute inset-0 bg-gradient-to-br from-amber-500/8 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                            <div className="relative w-12 h-12 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                <Cookie className="w-6 h-6 text-amber-400" />
                            </div>
                            <h4 className="relative text-lg font-semibold text-foreground/90 mb-2 group-hover:text-amber-400 transition-colors duration-300">本地持久化</h4>
                            <p className="relative text-sm text-muted-foreground/70 leading-relaxed">Cookie 本地存储配置信息，响应速度快、隐私性强</p>
                        </div>
                        <div className="group relative p-6 rounded-2xl backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/40 shadow-lg hover:shadow-xl hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-2 transition-all duration-400 ease-out overflow-hidden"
                            style={{ background: "radial-gradient(ellipse 80% 70% at 25% 35%, rgba(16, 185, 129, 0.3), transparent), radial-gradient(ellipse 60% 80% at 75% 65%, rgba(20, 184, 166, 0.25), transparent), radial-gradient(ellipse 70% 50% at 45% 80%, rgba(5, 150, 105, 0.2), transparent), radial-gradient(ellipse 90% 60% at 65% 25%, rgba(6, 182, 212, 0.15), transparent)" }}>
                            <div className="absolute inset-0 bg-gradient-to-br from-emerald-500/8 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                            <div className="relative w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-500/20 to-teal-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                <Shield className="w-6 h-6 text-emerald-400" />
                            </div>
                            <h4 className="relative text-lg font-semibold text-foreground/90 mb-2 group-hover:text-emerald-400 transition-colors duration-300">自主可控</h4>
                            <p className="relative text-sm text-muted-foreground/70 leading-relaxed">数据完全由用户掌控，可随时导出或删除，隐私无忧</p>
                        </div>
                    </div>
                </div>
            </section>

            <section className="py-20 px-6 relative overflow-hidden">
                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[600px] rounded-full bg-gradient-radial from-[var(--brand-glow)]/8 via-transparent to-transparent" />
                </div>
                <div className="max-w-5xl mx-auto relative z-10">
                    <RevealGroup className="text-center mb-16" delayBase={0}>
                        <h2 className="text-2xl md:text-3xl font-bold tracking-tight mb-4">
                            <span suppressHydrationWarning className="text-foreground/80">随心切换</span>
                            <span className="bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>展示模式</span>
                        </h2>
                        <p className="text-sm text-muted-foreground/60">适配不同使用环境，自动切换最佳视觉体验</p>
                    </RevealGroup>

                    <RevealGroup delayBase={100}>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-20 max-w-4xl mx-auto">
                            <button onClick={() => setTheme("light")} type="button"
                                className="relative p-8 rounded-3xl backdrop-blur-sm border-2 border-amber-500/40 hover:border-amber-500 hover:shadow-xl hover:shadow-amber-500/15 hover:-translate-y-2 transition-all duration-400 ease-out overflow-hidden text-left cursor-pointer w-full bg-gradient-to-br from-amber-50 to-amber-100 active:scale-95">
                                <div className="absolute top-4 right-4 w-16 h-16 rounded-2xl bg-gradient-to-br from-amber-200 to-amber-300 flex items-center justify-center">
                                    <Sun className="w-8 h-8 text-amber-700" />
                                </div>
                                <div className="text-left">
                                    <h3 className="text-xl font-bold text-amber-700 mb-2">日间模式</h3>
                                    <p className="text-sm text-amber-600/80">明亮清晰的视觉体验，适合白天使用</p>
                                </div>
                            </button>
                            <button onClick={() => setTheme("dark")} type="button"
                                className="relative p-8 rounded-3xl backdrop-blur-sm border-2 border-indigo-500/40 hover:border-indigo-500 hover:shadow-xl hover:shadow-indigo-500/15 hover:-translate-y-2 transition-all duration-400 ease-out overflow-hidden text-left cursor-pointer w-full bg-gradient-to-br from-indigo-900 to-purple-900 active:scale-95">
                                <div className="absolute top-4 right-4 w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
                                    <Monitor className="w-8 h-8 text-white" />
                                </div>
                                <div className="text-left">
                                    <h3 className="text-xl font-bold text-indigo-400 mb-2">夜间模式</h3>
                                    <p className="text-sm text-indigo-300/80">柔和护眼的深色界面，适合夜晚使用</p>
                                </div>
                            </button>
                        </div>
                    </RevealGroup>

                    <RevealGroup className="text-center mb-10" delayBase={0}>
                        <h2 className="text-2xl md:text-3xl font-bold tracking-tight mb-4">
                            <span className="bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>全功能免费</span>
                            <span suppressHydrationWarning className="text-foreground/80">使用权益</span>
                        </h2>
                        <p className="text-sm text-muted-foreground/60">无需付费、无需订阅、无任何限制，尽情享受完整功能</p>
                    </RevealGroup>

                    <RevealGroup delayBase={100}>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 max-w-4xl mx-auto">
                            {[
                                { icon: Gift, title: "全部功能免费", desc: "无付费门槛", border: "green", gradient: "radial-gradient(ellipse 80% 70% at 20% 30%, rgba(34, 197, 94, 0.25), transparent), radial-gradient(ellipse 60% 80% at 80% 70%, rgba(22, 163, 74, 0.2), transparent), radial-gradient(ellipse 70% 50% at 40% 85%, rgba(21, 128, 61, 0.15), transparent)" },
                                { icon: Calendar, title: "全自动定时", desc: "到点自动播放", border: "blue", gradient: "radial-gradient(ellipse 80% 70% at 30% 20%, rgba(59, 130, 246, 0.25), transparent), radial-gradient(ellipse 60% 80% at 70% 80%, rgba(37, 99, 235, 0.2), transparent), radial-gradient(ellipse 70% 50% at 50% 50%, rgba(29, 78, 216, 0.15), transparent)" },
                                { icon: Zap, title: "无广告弹窗", desc: "纯净体验", border: "purple", gradient: "radial-gradient(ellipse 80% 70% at 25% 35%, rgba(168, 85, 247, 0.25), transparent), radial-gradient(ellipse 60% 80% at 75% 65%, rgba(139, 92, 246, 0.2), transparent), radial-gradient(ellipse 70% 50% at 45% 80%, rgba(124, 58, 237, 0.15), transparent)" },
                                { icon: Shield, title: "隐私零收集", desc: "数据安全", border: "amber", gradient: "radial-gradient(ellipse 80% 70% at 20% 25%, rgba(251, 191, 36, 0.25), transparent), radial-gradient(ellipse 60% 80% at 80% 75%, rgba(249, 115, 22, 0.2), transparent), radial-gradient(ellipse 70% 50% at 50% 45%, rgba(234, 179, 8, 0.15), transparent)" },
                                { icon: Smartphone, title: "全平台通用", desc: "多设备同步", border: "cyan", gradient: "radial-gradient(ellipse 80% 70% at 30% 30%, rgba(34, 211, 238, 0.25), transparent), radial-gradient(ellipse 60% 80% at 70% 70%, rgba(6, 182, 212, 0.2), transparent), radial-gradient(ellipse 70% 50% at 40% 60%, rgba(14, 165, 233, 0.15), transparent)" },
                                { icon: Lock, title: "端到端加密", desc: "传输安全", border: "rose", gradient: "radial-gradient(ellipse 80% 70% at 25% 30%, rgba(244, 63, 94, 0.25), transparent), radial-gradient(ellipse 60% 80% at 75% 70%, rgba(225, 29, 72, 0.2), transparent), radial-gradient(ellipse 70% 50% at 50% 50%, rgba(190, 24, 93, 0.15), transparent)" },
                                { icon: Shield, title: "数据自主", desc: "随时导出", border: "pink", gradient: "radial-gradient(ellipse 80% 70% at 20% 35%, rgba(236, 72, 153, 0.25), transparent), radial-gradient(ellipse 60% 80% at 80% 65%, rgba(219, 39, 119, 0.2), transparent), radial-gradient(ellipse 70% 50% at 50% 50%, rgba(190, 24, 93, 0.15), transparent)" },
                                { icon: Zap, title: "极速响应", desc: "毫秒级启动", border: "emerald", gradient: "radial-gradient(ellipse 80% 70% at 30% 25%, rgba(16, 185, 129, 0.25), transparent), radial-gradient(ellipse 60% 80% at 70% 75%, rgba(5, 150, 105, 0.2), transparent), radial-gradient(ellipse 70% 50% at 50% 50%, rgba(4, 120, 87, 0.15), transparent)" },
                                { icon: Crown, title: "无会员等级", desc: "人人平等", border: "indigo", gradient: "radial-gradient(ellipse 80% 70% at 25% 30%, rgba(99, 102, 241, 0.25), transparent), radial-gradient(ellipse 60% 80% at 75% 70%, rgba(79, 70, 229, 0.2), transparent), radial-gradient(ellipse 70% 50% at 50% 50%, rgba(67, 56, 202, 0.15), transparent)" },
                            ].map((item, idx) => (
                                <div key={idx} className="group relative p-5 rounded-2xl border border-border/50 hover:border-[var(--brand-glow)]/40 hover:shadow-xl hover:shadow-[var(--brand-glow)]/10 hover:-translate-y-1 transition-all duration-300 overflow-hidden text-center"
                                    style={{ background: item.gradient }}>
                                    <div className="absolute inset-0 bg-gradient-to-br from-[var(--brand-glow)]/10 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-[var(--brand-glow)]/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <item.icon className="w-6 h-6 text-[var(--brand-glow)]" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">{item.title}</h4>
                                        <p className="text-xs text-muted-foreground/60">{item.desc}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </RevealGroup>
                </div>
            </section>

            <section id="templates" className="py-28 px-6 relative overflow-hidden">
                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[400px] rounded-full bg-gradient-radial from-[var(--brand-glow)]/10 via-transparent to-transparent blur-3xl" />
                </div>
                <div className="max-w-6xl mx-auto relative z-10">
                    <RevealGroup className="text-center mb-16" delayBase={0}>
                        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[var(--brand-glow)]/10 border border-[var(--brand-glow)]/20 mb-6">
                            <Sparkles className="w-4 h-4 text-[var(--brand-glow)]" />
                            <span suppressHydrationWarning className="text-[var(--brand-glow)] text-sm font-medium">精选模板</span>
                        </div>
                        <h2 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
                            <span suppressHydrationWarning className="text-foreground/90">专业思维导图</span>
                            <span className="block bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>一键生成</span>
                        </h2>
                        <p className="text-lg text-muted-foreground/70 max-w-2xl mx-auto">选择模板，输入内容，AI 自动生成精美思维导图</p>
                    </RevealGroup>
                    <RevealGroup delayBase={200}>
                        <TemplateSelector
                            selected={selectedTemplates}
                            onChange={setSelectedTemplates}
                            maxSelect={5}
                            recommended={recommendedTemplates} />
                    </RevealGroup>
                </div>
            </section>

            <section className="py-28 px-6 relative">
                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                    <svg className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[120%] h-[400px] opacity-20" viewBox="0 0 1200 200">
                        <defs>
                            <linearGradient id="flowGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                                <stop offset="0%" stopColor="var(--brand-dim)" stopOpacity="0" />
                                <stop offset="50%" stopColor="var(--brand-glow)" stopOpacity="0.6" />
                                <stop offset="100%" stopColor="var(--brand-dim)" stopOpacity="0" />
                            </linearGradient>
                        </defs>
                        <path d="M0,100 Q300,60 600,100 T1200,100" fill="none" stroke="url(#flowGrad)" strokeWidth="2" className="animate-flow-line" />
                    </svg>
                </div>
                <div className="max-w-4xl mx-auto relative z-10">
                    <RevealGroup className="text-center mb-16" delayBase={0}>
                        <h2 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
                            <span suppressHydrationWarning className="text-foreground/90">简单</span>
                            <span className="bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>三步</span>
                        </h2>
                        <p className="text-base text-muted-foreground/70">从文章到脑图，弹指之间</p>
                    </RevealGroup>
                    <div className="grid md:grid-cols-3 gap-6 relative">
                        <div className="hidden md:block absolute top-16 left-1/3 right-1/3 h-px">
                            <div className="absolute left-1/2 right-0 top-0 h-px bg-gradient-to-r from-[var(--brand-glow)]/40 to-transparent" />
                            <div className="absolute left-0 right-1/2 top-0 h-px bg-gradient-to-l from-[var(--brand-glow)]/40 to-transparent" />
                        </div>
                        {[{
                            num: "01", icon: FileText, title: "输入内容", desc: "粘贴文章、输入链接或上传文档，支持多种格式", color: "from-[var(--brand-start)]"
                        }, {
                            num: "02", icon: Wand2, title: "智能生成", desc: "AI 瞬间分析内容结构，生成精美思维导图", color: "from-[var(--brand-mid)]"
                        }, {
                            num: "03", icon: Download, title: "导出使用", desc: "一键下载高清图片，或直接全屏展示", color: "from-[var(--brand-end)]"
                        }].map((item, idx) => <RevealGroup key={idx} delayBase={idx * 120}>
                            <div className="relative group">
                                <div className="absolute -top-3 -left-1 text-6xl font-bold opacity-5 select-none pointer-events-none">{item.num}</div>
                                <div className="relative h-[200px] p-8 rounded-2xl bg-background/60 backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/30 shadow-lg hover:shadow-2xl hover:-translate-y-1 transition-all duration-400 ease-out overflow-hidden flex flex-col">
                                    <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${item.color} to-transparent opacity-60`} />
                                    <div className="relative z-10 text-center flex flex-col h-full">
                                        <div className={`w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br ${item.color}/20 to-transparent flex items-center justify-center mb-5 group-hover:scale-110 transition-transform duration-300 shrink-0`}>
                                            <item.icon className="w-7 h-7 text-[var(--brand-glow)]" />
                                        </div>
                                        <h3 className="text-xl font-semibold text-foreground/90 mb-3 tracking-tight shrink-0">{item.title}</h3>
                                        <p className="text-sm text-muted-foreground/70 leading-relaxed flex-1">{item.desc}</p>
                                    </div>
                                </div>
                            </div>
                        </RevealGroup>)}
                    </div>
                </div>
            </section>

            <section id="start" className="py-24 px-6 relative">
                <div className="absolute inset-0 overflow-hidden pointer-events-none">
                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[400px] rounded-full bg-gradient-radial from-[var(--brand-glow)]/10 via-transparent to-transparent blur-3xl" />
                </div>
                <div className="max-w-2xl mx-auto relative z-10 text-center">
                    <RevealGroup delayBase={0}>
                        <h2 className="text-3xl md:text-4xl font-bold tracking-tight mb-4">
                            <span suppressHydrationWarning className="text-foreground/90">开始</span>
                            <span className="bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>创作</span>
                        </h2>
                        <p className="text-base text-muted-foreground/70 mb-8">输入你的文章，让 AI 为你生成精美的思维导图</p>
                        <div className="flex items-center justify-center mb-8">
                            <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-gradient-to-r from-[var(--brand-start)]/15 to-[var(--brand-end)]/10 border border-[var(--brand-start)]/20">
                                <div className="w-2 h-2 rounded-full bg-[var(--brand-start)] animate-pulse" />
                                <span suppressHydrationWarning className="text-[var(--brand-start)] text-sm font-medium">无需登录 · 开箱即用</span>
                            </div>
                        </div>
                        <RippleButton
                            onClick={() => router.push(`/settings${selectedTemplates.length > 0 ? `?templates=${selectedTemplates.join(",")}` : ""}`)}
                            className="group relative inline-flex items-center gap-3 px-8 py-4 rounded-2xl bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] text-white font-semibold text-xl shadow-xl shadow-[var(--brand-start)]/25 hover:shadow-2xl hover:shadow-[var(--brand-start)]/35 hover:scale-105 active:scale-95 transition-all duration-300">
                            <div className="absolute inset-0 bg-gradient-to-r from-[var(--brand-end)] to-[var(--brand-start)] opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
                            <div className="absolute inset-0 opacity-30">
                                <div className="absolute inset-0 bg-[length:200%_100%] bg-gradient-to-r from-transparent via-white/20 to-transparent animate-shimmer" />
                            </div>
                            <div className="relative flex items-center gap-3">
                                <img src="/logo.png" alt="梦枕" className="w-7 h-7 group-hover:scale-110 transition-transform duration-300 rounded shadow-md" />
                                <span suppressHydrationWarning>免费体验</span>
                                <ChevronRight className="w-6 h-6 group-hover:translate-x-1 transition-transform duration-300" />
                            </div>
                        </RippleButton>
                    </RevealGroup>
                </div>
            </section>
        </>
    );
}

import { Volume2 } from "lucide-react";
