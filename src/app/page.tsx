"use client";
import { useState, useCallback, useEffect, useRef } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { TemplateSelector } from "@/components/template-selector";
import RippleButton from "@/components/RippleButton";
import LazySection from "@/components/lazy-section";
import { useTheme } from "@/lib/theme-context";
import { HeroTitle } from "@/components/hero-title";

import {
    Brain,
    Layers,
    Music,
    Clock,
    Volume2,
    Upload,
    Zap,
    Shield,
    Smartphone,
    ChevronRight,
    Sun,
    Moon,
    Monitor,
    Sparkles,
    GraduationCap,
    Briefcase,
    Database,
    Cookie,
    Headphones,
    Settings2,
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

type IconComponent = React.ComponentType<{ className?: string }>;

const homeCardClass = "home-diffuse-card group relative overflow-hidden bg-transparent border border-border/50 shadow-lg shadow-foreground/5 transition-all duration-300 ease-out hover:border-[var(--brand-glow)]/40 hover:shadow-xl hover:shadow-[var(--brand-glow)]/10";
const homeLiftCardClass = cn(homeCardClass, "hover:-translate-y-1");
const homeStrongLiftCardClass = cn(homeCardClass, "hover:-translate-y-2 duration-400");

function PainCard({ icon: Icon, title, desc, iconBg, style }: {
    icon: IconComponent;
    title: string;
    desc: string;
    iconBg: string;
    style?: React.CSSProperties;
}) {
    return (
        <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={style}>
            <div className="flex flex-col items-center text-center gap-4">
                <div
                    className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${iconBg} flex items-center justify-center group-hover:scale-110 transition-transform duration-300`}>
                    <Icon className="w-7 h-7 text-[var(--brand-glow)]" />
                </div>
                <div>
                    <h3 className="text-lg font-semibold text-foreground/90 mb-2">{title}？</h3>
                    <p className="text-sm text-muted-foreground/70 leading-relaxed">{desc}</p>
                </div>
            </div>
        </div>
    );
}

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
        delayBase = 0
    }: {
        children: React.ReactNode;
        className?: string;
        delayBase?: number;
    }
) {
    const {
        ref,
        isVisible
    } = usePrecisionReveal({
        threshold: 0.08
    });

    return (
        <div ref={ref} className={cn("space-y-2", className)}>
            {Array.isArray(children) ? children.map((child, i) => <div
                key={i}
                style={{
                    opacity: isVisible ? 1 : 0,
                    transform: isVisible ? "translateY(0)" : "translateY(16px)",
                    transition: `opacity 0.5s ease ${delayBase + i * 80}ms, transform 0.5s ease ${delayBase + i * 80}ms`,
                    pointerEvents: 'auto'
                }}>
                {child}
            </div>) : <div
                style={{
                    opacity: isVisible ? 1 : 0,
                    transform: isVisible ? "translateY(0)" : "translateY(16px)",
                    transition: `opacity 0.5s ease ${delayBase}ms, transform 0.5s ease ${delayBase}ms`,
                    pointerEvents: 'auto'
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
                    className={cn("char-hidden animate-char-reveal text-foreground/70", wordClassName)}
                    style={{
                        "--char-delay": `${delayBase + i * wordDelay + j * 40}ms`
                    } as React.CSSProperties}>
                    {char}
                </span>)}
                {i < words.length - 1 && <span
                    className={cn("char-hidden animate-char-reveal text-[var(--brand-glow)]/50 mx-2")}
                    style={{
                        "--char-delay": `${delayBase + i * wordDelay + word.length * 40}ms`
                    } as React.CSSProperties}>
                    {separator}
                </span>}
            </span>)}
        </span>
    );
}

function useScrollVisibility(targetRef: React.RefObject<HTMLElement | null>) {
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        const handleScroll = () => {
            if (!targetRef.current) {
                setIsVisible(false);
                return;
            }

            const rect = targetRef.current.getBoundingClientRect();
            const isInView = rect.top < window.innerHeight && rect.bottom > 0;
            setIsVisible(isInView);
        };

        handleScroll();

        window.addEventListener("scroll", handleScroll, {
            passive: true
        });

        return () => window.removeEventListener("scroll", handleScroll);
    }, [targetRef]);

    return isVisible;
}

function FloatingBar(
    {
        visible
    }: {
        visible: boolean;
    }
) {
    const router = useRouter();

    return (
        <div
            className={cn(
                "fixed bottom-0 left-0 right-0 z-[500]",
                visible ? "translate-y-0 opacity-100" : "translate-y-4 opacity-0 pointer-events-none"
            )}
            style={{
                transition: visible ? "transform 0.3s ease-out, opacity 0.3s ease-out" : "transform 0.5s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.5s ease-out"
            }}>
            <div className="mx-auto max-w-md px-5 pb-5">
                <div
                    className="group relative flex items-center justify-between gap-4 px-4 py-3 rounded-2xl backdrop-blur-xl border border-border/60 shadow-lg shadow-foreground/5 hover:shadow-xl hover:shadow-foreground/10 hover:border-border/80 transition-all duration-300 ease-out overflow-hidden">
                    <div className="relative flex items-center gap-3 z-10">
                        <Image
                            src="/logo.png"
                            alt="梦枕"
                            width={36}
                            height={36}
                            className="w-9 h-9 rounded-xl shadow-md shadow-[inset_0_2px_6px_rgba(0,0,0,0.35)] transition-transform duration-300 group-hover:scale-110" />
                        <span className="font-bold text-base tracking-tight">
                            <span
                                className="bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] bg-clip-text text-transparent">梦枕
                            </span>
                        </span>
                    </div>
                    <RippleButton
                        onClick={() => router.replace("/settings")}
                        className="relative flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] text-white font-semibold text-sm shadow-lg shadow-[var(--brand-start)]/25 hover:shadow-xl hover:shadow-[var(--brand-start)]/35 hover:scale-105 active:scale-95 transition-all duration-200 ease-out z-10">
                        <span className="relative flex items-center gap-2">
                            <span suppressHydrationWarning>免费体验</span>
                            <ChevronRight
                                className="w-4 h-4 group-hover:translate-x-0.5 transition-transform duration-200" />
                        </span>
                    </RippleButton>
                </div>
            </div>
        </div>
    );
}

const heroChips: { icon: IconComponent; text: string }[] = [
    { icon: Music, text: "多格式音频适配" },
    { icon: Clock, text: "时段自定义配置" },
    { icon: Volume2, text: "精细化音量管控" }
];

const diffuseShapes = [
    ["82deg", "24%", "38%", "42% 58% 64% 36% / 53% 34% 66% 47%", "rotate(-18deg) skew(-10deg, 3deg)", "rotate(-23deg) skew(-12deg, 5deg) scale(1.12)"],
    ["218deg", "76%", "28%", "61% 39% 44% 56% / 36% 62% 38% 64%", "rotate(14deg) skew(8deg, -5deg)", "rotate(19deg) skew(10deg, -7deg) scale(1.1)"],
    ["312deg", "30%", "82%", "37% 63% 52% 48% / 68% 41% 59% 32%", "rotate(-6deg) skew(11deg, 2deg)", "rotate(-11deg) skew(14deg, 3deg) scale(1.11)"],
    ["154deg", "82%", "74%", "56% 44% 33% 67% / 47% 70% 30% 53%", "rotate(24deg) skew(-5deg, 9deg)", "rotate(30deg) skew(-6deg, 11deg) scale(1.1)"],
    ["18deg", "58%", "18%", "68% 32% 57% 43% / 40% 55% 45% 60%", "rotate(8deg) skew(-14deg, 6deg)", "rotate(13deg) skew(-16deg, 8deg) scale(1.1)"],
    ["268deg", "18%", "66%", "45% 55% 35% 65% / 63% 35% 65% 37%", "rotate(-28deg) skew(6deg, -9deg)", "rotate(-34deg) skew(8deg, -11deg) scale(1.12)"],
] as const;

const diffuseHue = (i: number, step: number) => (152 + i * 37 + step * 28) % 360;
const cardDiffuse = (i: number): React.CSSProperties => {
    const [angle, x, y, radius, transform, hoverTransform] = diffuseShapes[i % diffuseShapes.length];
    const colors = Array.from({ length: 7 }, (_, n) => `hsl(${diffuseHue(i, n)} 72% ${62 + ((i + n) % 4) * 5}% / ${0.4 + ((i + n) % 3) * 0.08})`);
    return {
        ["--diffuse-angle" as string]: angle,
        ["--diffuse-x" as string]: x,
        ["--diffuse-y" as string]: y,
        ["--diffuse-radius" as string]: radius,
        ["--diffuse-transform" as string]: transform,
        ["--diffuse-hover-transform" as string]: hoverTransform,
        ...Object.fromEntries(colors.map((color, n) => [`--diffuse-${"abcdefg"[n]}`, color])),
    };
};

const minimalPrinciples: { icon: IconComponent; title: string; desc: string }[] = [
    { icon: Music, title: "私人音频", desc: "不做内容流，不推曲库，只播放你自己选择的声音。" },
    { icon: Clock, title: "自动任务", desc: "设好时间、时长和音量渐变，夜里按计划安静执行。" },
    { icon: Shield, title: "少即是安", desc: "只保留音频和任务所需数据，不追踪睡眠，也不制造打扰。" }
];

const minimalBoundaries = ["无广告", "无订阅", "无推荐流", "无社交分享", "无睡眠监测", "无多余弹窗"];

const heroNodes = [
    { x: 200, y: 300, r: 4, delay: 0 },
    { x: 400, y: 200, r: 3, delay: 0.2 },
    { x: 600, y: 400, r: 5, delay: 0.4 },
    { x: 800, y: 250, r: 3, delay: 0.6 },
    { x: 1000, y: 350, r: 4, delay: 0.8 },
    { x: 300, y: 500, r: 3, delay: 1 },
    { x: 700, y: 550, r: 4, delay: 1.2 },
    { x: 500, y: 600, r: 3, delay: 1.4 }
];

const heroVariants = {
    mobile: {
        section: "relative sm:hidden min-h-[calc(100svh-56px)] flex flex-col items-center justify-start px-1 pt-4 pb-20 overflow-hidden",
        content: "relative z-10 w-full max-w-[23rem] mx-auto text-center space-y-4",
        title: "w-[22rem] max-w-full",
        subtitle: "PWA构建·云端同步·本地持久化",
        subtitleClass: "flex-wrap gap-x-2 gap-y-1 max-w-[18rem] mx-auto text-xs leading-snug font-light",
        explore: false
    },
    desktop: {
        section: "relative hidden sm:flex min-h-[85vh] flex flex-col items-center justify-center px-6 overflow-hidden",
        content: "relative z-10 w-full max-w-3xl mx-auto text-center space-y-10",
        title: "w-[31rem] max-w-full",
        subtitle: "PWA渐进式网页应用构建·云端数据库数据持久化·Cookie客户端本地持久化存储",
        subtitleClass: "text-lg md:text-xl max-w-xl mx-auto leading-relaxed font-light",
        explore: true
    }
} as const;

type HeroMode = keyof typeof heroVariants;
type HeroProps = {
    buttonRef: React.RefObject<HTMLButtonElement | null>;
    onStart: () => void;
};

function HeroBackdrop({ id }: { id: string }) {
    const nodeGlowId = `${id}NodeGlow`;
    const lineGradId = `${id}LineGrad`;

    return (
        <div className="absolute inset-0 overflow-hidden">
            <svg className="absolute inset-0 w-full h-full opacity-30 z-0" viewBox="0 0 1200 800" preserveAspectRatio="xMidYMid slice">
                <defs>
                    <radialGradient id={nodeGlowId} cx="50%" cy="50%" r="50%">
                        <stop offset="0%" stopColor="var(--brand-glow)" stopOpacity="0.6" />
                        <stop offset="100%" stopColor="var(--brand-glow)" stopOpacity="0" />
                    </radialGradient>
                    <linearGradient id={lineGradId} x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" stopColor="var(--brand-dim)" stopOpacity="0" />
                        <stop offset="50%" stopColor="var(--brand-glow)" stopOpacity="0.8" />
                        <stop offset="100%" stopColor="var(--brand-dim)" stopOpacity="0" />
                    </linearGradient>
                </defs>
                <g>
                    <path d="M100,400 Q300,200 500,350 T900,400" fill="none" stroke={`url(#${lineGradId})`} strokeWidth="1.5" strokeDasharray="8,4" className="animate-dash" />
                    <path d="M200,500 Q400,600 600,450 T1100,300" fill="none" stroke={`url(#${lineGradId})`} strokeWidth="1" strokeDasharray="6,6" className="animate-dash-reverse" style={{ animationDelay: "1s" }} />
                    <path d="M50,300 Q250,100 450,250 T850,150" fill="none" stroke="var(--brand-dim)" strokeWidth="0.8" strokeDasharray="4,8" className="animate-dash" style={{ animationDelay: "0.5s" }} />
                </g>
                <g>
                    {heroNodes.map((node, i) => (
                        <g key={i}>
                            <circle cx={node.x} cy={node.y} r={node.r * 3} fill={`url(#${nodeGlowId})`} className="animate-pulse-slow" style={{ animationDelay: `${node.delay}s` }} />
                            <circle cx={node.x} cy={node.y} r={node.r} fill="var(--brand-glow)" className="animate-glow" style={{ animationDelay: `${node.delay}s` }} />
                        </g>
                    ))}
                </g>
            </svg>
        </div>
    );
}


function HeroBadge({ mobile = false }: { mobile?: boolean }) {
    return (
        <div className={cn(
            "group flex items-center rounded-full bg-gradient-to-r from-[var(--brand-start)]/20 to-[var(--brand-end)]/15 border border-[var(--brand-start)]/30 backdrop-blur-sm cursor-default",
            "hover:border-[var(--brand-start)]/50 hover:from-[var(--brand-start)]/25 hover:to-[var(--brand-end)]/20 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-300 ease-out",
            mobile ? "gap-2 px-4 py-2" : "gap-3 px-6 py-3"
        )}>
            <div className={cn("relative flex items-center", mobile ? "gap-1.5" : "gap-2")}>
                <div className={cn("rounded-full bg-[var(--brand-start)] animate-pulse", mobile ? "w-2 h-2" : "w-2.5 h-2.5")} />
                <span suppressHydrationWarning className={cn("text-[var(--brand-start)] font-semibold tracking-wide", mobile ? "text-xs" : "text-sm")}>
                    {mobile ? "用户认证" : "用户认证系统"}
                </span>
            </div>
            <div className={cn("w-px bg-[var(--brand-start)]/30", mobile ? "h-3.5" : "h-4")} />
            {!mobile && <span suppressHydrationWarning className="text-sm text-foreground/70">全平台兼容</span>}
            <span suppressHydrationWarning className={cn("text-[var(--brand-start)]/60 font-medium tracking-wide group-hover:font-bold group-hover:text-[var(--brand-start)] transition-all duration-300 relative after:absolute after:bottom-0 after:left-0 after:w-0 after:h-0.5 after:bg-[var(--brand-start)] group-hover:after:w-full after:transition-all after:duration-300 after:ease-out", mobile ? "text-xs" : "text-sm")} style={{ fontFamily: "'Georgia', 'Cambria', 'Times New Roman', 'STKaiti', 'KaiTi', 'FangSong', serif" }}>
                全自动流程
            </span>
        </div>
    );
}

function HeroCta({ buttonRef, onClick, mobile = false }: {
    buttonRef: React.RefObject<HTMLButtonElement | null>;
    onClick: () => void;
    mobile?: boolean;
}) {
    return (
        <RippleButton
            ref={buttonRef}
            onClick={onClick}
            className={cn(
                "group relative inline-flex items-center bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] text-white font-semibold text-lg shadow-xl shadow-[var(--brand-start)]/25 z-10",
                "hover:z-20 transition-[transform,box-shadow,z-index] duration-300 hover:shadow-2xl hover:shadow-[var(--brand-start)]/35 hover:scale-105 active:scale-95",
                mobile ? "gap-2.5 px-5 py-2.5 rounded-xl" : "gap-3 px-6 py-3 rounded-2xl"
            )}>
            <div className="relative flex items-center gap-3">
                <Image src="/logo.png" alt="梦枕" width={24} height={24} className={cn("group-hover:scale-110 transition-transform duration-300 rounded shadow-md", mobile ? "w-5 h-5" : "w-6 h-6")} />
                <span className={mobile ? "text-xl" : "text-2xl"} style={{ fontFamily: "DOUYINSANSBOLD-GB", filter: "drop-shadow(rgb(161, 161, 170) 0px 0px 10px)" }} suppressHydrationWarning>免费体验</span>
                <ChevronRight className="w-5 h-5 group-hover:translate-x-1 transition-transform duration-300" />
            </div>
        </RippleButton>
    );
}

function HeroChips({ mobile = false }: { mobile?: boolean }) {
    return (
        <div className={cn("flex items-center justify-center", mobile ? "flex-nowrap gap-1.5 pt-2" : "flex-wrap gap-3 pt-2")}>
            {heroChips.map((item, idx) => (
                <div key={idx} className={cn("group flex items-center rounded-full backdrop-blur-sm border border-border/50 hover:border-[var(--brand-glow)]/40 transition-all duration-300 cursor-default", mobile ? "gap-1 px-2 py-1.5" : "gap-2 px-4 py-2")}>
                    <item.icon className={cn("text-[var(--brand-glow)] group-hover:scale-110 transition-transform", mobile ? "w-3 h-3" : "w-4 h-4")} />
                    <span className="text-sm text-foreground/80 group-hover:text-foreground transition-colors" style={{ fontFamily: "DOUYINSANSBOLD-GB", fontWeight: "normal", fontSize: mobile ? "clamp(10px, 2.8vw, 16px)" : "16px" }}>{item.text}</span>
                </div>
            ))}
        </div>
    );
}

function HeroExplore() {
    return (
        <RevealGroup delayBase={500}>
            <div className="flex flex-col items-center gap-3 pt-8">
                <span className="text-[11px] text-muted-foreground/40 tracking-[0.3em] uppercase" suppressHydrationWarning>向下探索</span>
                <div className="relative w-6 h-10 rounded-full border border-border/30 flex items-start justify-center p-1.5">
                    <div className="w-1.5 h-3 rounded-full bg-gradient-to-b from-[var(--brand-glow)] to-[var(--brand-dim)] animate-scroll-indicator" />
                </div>
            </div>
        </RevealGroup>
    );
}

function HeroView({ mode, buttonRef, onStart }: HeroProps & { mode: HeroMode }) {
    const variant = heroVariants[mode];
    const mobile = mode === "mobile";

    return (
        <section className={variant.section}>
            <HeroBackdrop id={`${mode}Hero`} />
            <div className={variant.content}>
                <HeroTitle gradientId={`${mode}HeroTitle`} className={variant.title} fontSize={mobile ? "76px" : "74px"} animated />
                <WordReveal text={variant.subtitle} className={variant.subtitleClass} delayBase={1200} wordDelay={200} />
                <RevealGroup delayBase={300}>
                    <div className="inline-flex flex-col items-center gap-4">
                        <HeroBadge mobile={mobile} />
                        <HeroCta mobile={mobile} buttonRef={buttonRef} onClick={onStart} />
                    </div>
                    <HeroChips mobile={mobile} />
                </RevealGroup>
                {variant.explore && <HeroExplore />}
            </div>
        </section>
    );
}

function MobileHero(props: HeroProps) {
    return <HeroView mode="mobile" {...props} />;
}

function DesktopHero(props: HeroProps) {
    return <HeroView mode="desktop" {...props} />;
}

export default function HomePage() {
    const router = useRouter();
    const { setTheme } = useTheme();

    const mobileHeroButtonRef = useRef<HTMLButtonElement>(null);
    const desktopHeroButtonRef = useRef<HTMLButtonElement>(null);
    const bottomCtaRef = useRef<HTMLButtonElement>(null);
    const mobileHeroButtonVisible = useScrollVisibility(mobileHeroButtonRef as React.RefObject<HTMLElement | null>);
    const desktopHeroButtonVisible = useScrollVisibility(desktopHeroButtonRef as React.RefObject<HTMLElement | null>);
    const bottomCtaVisible = useScrollVisibility(bottomCtaRef as React.RefObject<HTMLElement | null>);
    const showFloatingBar = !mobileHeroButtonVisible && !desktopHeroButtonVisible && !bottomCtaVisible;
    const startExperience = useCallback(() => {
        router.replace("/settings");
    }, [router]);

    return (
        <div
            className="home-page min-h-screen text-foreground overflow-x-hidden relative"
            suppressHydrationWarning>

            <main className="relative">
                <MobileHero buttonRef={mobileHeroButtonRef} onStart={startExperience} />
                <DesktopHero buttonRef={desktopHeroButtonRef} onStart={startExperience} />
                <LazySection>
                <section id="features" className="py-32 px-6 relative overflow-hidden">
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

                        {/* 第一板块：核心痛点 - 独立卡片平铺 */}
                        <div className="grid md:grid-cols-2 gap-6 mb-16">
                            <div className={cn(homeCardClass, "flex items-start gap-4 p-5 rounded-2xl hover:-translate-y-0.5")} style={cardDiffuse(0)}>
                                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/10 flex items-center justify-center shrink-0">
                                    <Moon className="w-6 h-6 text-[var(--brand-glow)]" />
                                </div>
                                <div>
                                    <h3 className="text-lg font-semibold text-foreground/90 mb-2">睡眠深度较浅</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">对音量突变敏感，音频启停稍有不慎便会彻底惊醒，难以再次入睡</p>
                                </div>
                            </div>
                            <div className={cn(homeCardClass, "flex items-start gap-4 p-5 rounded-2xl hover:-translate-y-0.5")} style={cardDiffuse(1)}>
                                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/10 flex items-center justify-center shrink-0">
                                    <Sun className="w-6 h-6 text-[var(--brand-glow)]" />
                                </div>
                                <div>
                                    <h3 className="text-lg font-semibold text-foreground/90 mb-2">夜间易中途觉醒</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">入睡效率良好，但夜间频繁中途醒来，需要柔和音频辅助接续睡眠</p>
                                </div>
                            </div>
                            <div className={cn(homeCardClass, "flex items-start gap-4 p-5 rounded-2xl hover:-translate-y-0.5")} style={cardDiffuse(2)}>
                                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/10 flex items-center justify-center shrink-0">
                                    <Volume2 className="w-6 h-6 text-[var(--brand-glow)]" />
                                </div>
                                <div>
                                    <h3 className="text-lg font-semibold text-foreground/90 mb-2">音量突变惊醒</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">精准音量渐入渐出自定义，彻底规避音频启停音量骤变惊醒用户的问题</p>
                                </div>
                            </div>
                            <div className={cn(homeCardClass, "flex items-start gap-4 p-5 rounded-2xl hover:-translate-y-0.5")} style={cardDiffuse(3)}>
                                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[var(--brand-start)]/20 to-[var(--brand-end)]/10 flex items-center justify-center shrink-0">
                                    <Clock className="w-6 h-6 text-[var(--brand-glow)]" />
                                </div>
                                <div>
                                    <h3 className="text-lg font-semibold text-foreground/90 mb-2">深夜操作困难</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">半夜醒来后睡意朦胧，不愿手动操作手机，一键预设定时播放完美适配</p>
                                </div>
                            </div>
                        </div>

                        {/* 第二板块：接续睡眠 */}
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
                                { icon: RefreshCw, title: "觉醒自动续播", desc: "夜间醒来后，柔和音频无缝衔接，帮助快速重新入睡" },
                                { icon: Volume2, title: "零突变音量", desc: "全程音量渐入渐出，彻底规避惊醒风险，营造柔和睡眠氛围" },
                                { icon: Moon, title: "黑屏后台播放", desc: "锁屏休眠持续播放，不干扰睡眠，支持定时自动停止" }
                            ].map((item, idx) => (
                                <div key={idx} className={cn(homeLiftCardClass, "p-5 rounded-2xl")} style={cardDiffuse(4 + idx)}>
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

                        {/* 第三板块：音频控制 */}
                        <div className="text-center mb-8">
                            <h3 className="text-2xl font-bold text-foreground/90 mb-2">个性化音频配置</h3>
                            <p className="text-muted-foreground/60">精细化音量控制，适配个人听觉耐受度</p>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-12">
                            {[
                                { icon: Music, title: "全格式兼容", desc: "MP3、WAV、FLAC 等全主流音频格式" },
                                { icon: Headphones, title: "在线试听", desc: "上传后实时预览，快速筛选适配音频" },
                                { icon: Volume2, title: "小数级音量", desc: "0-100% 精细化分级，支持 0.1 微调" },
                                { icon: Layers, title: "播放列表", desc: "自定义音频播放顺序，编排专属播放列表" },
                                { icon: Settings2, title: "全面自定义", desc: "定时、渐变音量、播放规则全部可调" },
                                { icon: Calendar, title: "周期定时", desc: "每日/工作日重复定时，适配长期规律睡眠" }
                            ].map((item, idx) => (
                                <div key={idx} className={cn(homeLiftCardClass, "p-5 rounded-2xl")} style={cardDiffuse(7 + idx)}>
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

                        {/* 第三板块：PWA技术 */}
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
                                    { icon: Zap, title: "后台唤醒", desc: "锁屏休眠仍可定时唤醒正常播放" },
                                    { icon: Battery, title: "电池优化", desc: "忽略电池优化引导，提升休眠稳定性" },
                                    { icon: RefreshCw, title: "异常兜底", desc: "系统杀进程后可自动重试唤醒" },
                                    { icon: WifiOff, title: "离线模式", desc: "断网网络不佳时定时播放正常" }
                                ].map((item, idx) => (
                                    <div key={idx} className={cn(homeCardClass, "p-4 rounded-xl text-center hover:-translate-y-0.5")} style={cardDiffuse(13 + idx)}>
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

                        {/* 第四板块：数据安全 */}
                        <div className="text-center mb-8">
                            <h3 className="text-2xl font-bold text-foreground/90 mb-2">分层数据存储方案</h3>
                            <p className="text-muted-foreground/60">兼顾云端便捷性与本地隐私安全</p>
                        </div>
                        <div className="grid md:grid-cols-3 gap-4 mb-12">
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(17)}>
                                <div className="relative w-12 h-12 rounded-xl bg-gradient-to-br from-sky-500/20 to-blue-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                    <Database className="w-6 h-6 text-sky-400" />
                                </div>
                                <h4 className="relative text-lg font-semibold text-foreground/90 mb-2 group-hover:text-sky-400 transition-colors duration-300">云端数据库</h4>
                                <p className="relative text-sm text-muted-foreground/70 leading-relaxed">音频文件统一存入云端数据库，跨设备同步无缝使用</p>
                            </div>
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(18)}>
                                <div className="relative w-12 h-12 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                    <Cookie className="w-6 h-6 text-amber-400" />
                                </div>
                                <h4 className="relative text-lg font-semibold text-foreground/90 mb-2 group-hover:text-amber-400 transition-colors duration-300">本地持久化</h4>
                                <p className="relative text-sm text-muted-foreground/70 leading-relaxed">Cookie 本地存储配置信息，响应速度快、隐私性强</p>
                            </div>
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(19)}>
                                <div className="relative w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-500/20 to-teal-500/10 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                    <Shield className="w-6 h-6 text-emerald-400" />
                                </div>
                                <h4 className="relative text-lg font-semibold text-foreground/90 mb-2 group-hover:text-emerald-400 transition-colors duration-300">自主可控</h4>
                                <p className="relative text-sm text-muted-foreground/70 leading-relaxed">支持云端备份开关，自主选择是否同步播放配置</p>
                            </div>
                        </div>

                        {/* 第五板块：极简纯粹 */}
                        <div className="mb-12">
                            <div className="mb-6 text-center">
                                <h3 className="mb-2 text-2xl font-bold text-foreground/90">纯粹极简的产品定位</h3>
                                <p className="text-sm text-muted-foreground/65">梦枕不争夺注意力，只在深夜替你完成播放这件事。</p>
                            </div>
                            <div className="grid gap-4 md:grid-cols-[1.05fr_1fr]">
                                <div className={cn(homeStrongLiftCardClass, "rounded-2xl p-6 md:p-8")} style={cardDiffuse(22)}>
                                    <div className="relative flex h-full flex-col justify-between gap-8">
                                        <div>
                                            <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-[var(--brand-glow)]/25 px-3 py-1 text-xs font-medium text-[var(--brand-glow)]">
                                                留下必要，删掉噪音
                                            </div>
                                            <p className="text-2xl font-semibold leading-tight text-foreground/90 md:text-3xl">
                                                不把睡眠变成报表、课程或社区，只把助眠音频按时送到耳边。
                                            </p>
                                        </div>
                                        <div className="flex flex-wrap gap-2">
                                            {minimalBoundaries.map((text) => (
                                                <span key={text} className="rounded-full border border-border/55 px-3 py-1.5 text-xs font-medium text-muted-foreground/75">
                                                    {text}
                                                </span>
                                            ))}
                                        </div>
                                    </div>
                                </div>
                                <div className="grid gap-3">
                                    {minimalPrinciples.map(({ icon: Icon, title, desc }, idx) => (
                                        <div key={title} className={cn(homeLiftCardClass, "rounded-2xl p-4")} style={cardDiffuse(23 + idx)}>
                                            <div className="relative flex items-start gap-3">
                                                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-[var(--brand-glow)]/20 text-[var(--brand-glow)] transition-transform duration-300 group-hover:scale-110">
                                                    <Icon className="h-5 w-5" />
                                                </div>
                                                <div>
                                                    <h4 className="mb-1 font-semibold text-foreground/90">{title}</h4>
                                                    <p className="text-sm leading-relaxed text-muted-foreground/70">{desc}</p>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>

                        {/* 第六板块：安全保障 */}
                        <div className="text-center mb-8">
                            <h3 className="text-2xl font-bold text-foreground/90 mb-2">全方位隐私安全保障</h3>
                            <p className="text-muted-foreground/60">用户数据完全可控</p>
                            <div className="grid md:grid-cols-2 gap-6">
                                <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(26)}>
                                    <div className="relative flex items-center gap-3 mb-4">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-violet-500/20 to-violet-600/10 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                                            <Shield className="w-6 h-6 text-violet-400" />
                                        </div>
                                        <div>
                                            <h4 className="text-lg font-semibold text-foreground/90 group-hover:text-violet-400 transition-colors duration-300">银行级密码安全</h4>
                                            <p className="text-xs text-muted-foreground/60">bcrypt 哈希算法加密</p>
                                        </div>
                                    </div>
                                    <ul className="relative space-y-2 text-sm text-muted-foreground/70">
                                        <li className="flex items-center gap-2">
                                            <span className="w-1.5 h-1.5 rounded-full bg-violet-400" />
                                            不可逆加密处理，杜绝明文泄露
                                        </li>
                                        <li className="flex items-center gap-2">
                                            <span className="w-1.5 h-1.5 rounded-full bg-violet-400" />
                                            独立随机盐值混合加密
                                        </li>
                                        <li className="flex items-center gap-2">
                                            <span className="w-1.5 h-1.5 rounded-full bg-violet-400" />
                                            抵御彩虹表攻击、暴力破解
                                        </li>
                                    </ul>
                                </div>
                                <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(27)}>
                                    <div className="relative flex items-center gap-3 mb-4">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-500/20 to-cyan-600/10 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                                            <Lock className="w-6 h-6 text-cyan-400" />
                                        </div>
                                        <div>
                                            <h4 className="text-lg font-semibold text-foreground/90 group-hover:text-cyan-400 transition-colors duration-300">数据完全可控</h4>
                                            <p className="text-xs text-muted-foreground/60">无第三方快捷登录</p>
                                        </div>
                                    </div>
                                    <ul className="relative space-y-2 text-sm text-muted-foreground/70">
                                        <li className="flex items-center gap-2">
                                            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                                            不收集睡眠数据、不追踪使用行为
                                        </li>
                                        <li className="flex items-center gap-2">
                                            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                                            音频素材自主上传管理
                                        </li>
                                        <li className="flex items-center gap-2">
                                            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                                            无多余数据上报，仅存用户主动数据
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                {/* 第七板块：精准用户群体 */}
                <section className="py-20 px-6 relative overflow-hidden">
                    <div className="max-w-4xl mx-auto relative z-10">
                        <div className="text-center mb-12">
                            <h2 className="text-3xl md:text-4xl font-bold tracking-tight">
                                <span suppressHydrationWarning className="text-foreground/80">专为浅眠人群设计</span>
                                <span className="bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>接续睡眠</span>
                            </h2>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {/* 1. 浅眠 / 神经衰弱人群 */}
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(28)}>
                                <div className="relative">
                                    <div className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                        <Moon className="w-8 h-8 text-[var(--brand-glow)]" />
                                    </div>
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className="px-2 py-0.5 rounded-full bg-[var(--brand-glow)]/20 text-[var(--brand-glow)] text-xs font-medium">核心用户</span>
                                    </div>
                                    <h3 className="text-lg font-bold text-foreground/90 mb-2">浅眠 / 神经衰弱人群</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">
                                        长期睡眠浅、半夜频繁惊醒、对音量突变极度敏感，需要柔和渐变音量 + 全自动定时 + 后台稳定播放
                                    </p>
                                </div>
                            </div>

                            {/* 2. 高压都市上班族 */}
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(29)}>
                                <div className="relative">
                                    <div className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                        <Briefcase className="w-8 h-8 text-amber-500" />
                                    </div>
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className="px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-500 text-xs font-medium">职场首选</span>
                                    </div>
                                    <h3 className="text-lg font-bold text-foreground/90 mb-2">高压都市上班族</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">
                                        职场压力大、入睡困难，PWA免安装即用，全自动定时关闭，厌恶广告付费与臃肿APP
                                    </p>
                                </div>
                            </div>

                            {/* 3. 住校学生群体 */}
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(30)}>
                                <div className="relative">
                                    <div className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                        <GraduationCap className="w-8 h-8 text-purple-500" />
                                    </div>
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className="px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-500 text-xs font-medium">校园适配</span>
                                    </div>
                                    <h3 className="text-lg font-bold text-foreground/90 mb-2">住校学生群体</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">
                                        宿舍环境嘈杂、集体作息受限，自定义专属助眠音频、音量柔和不吵室友，无冗余社交广告
                                    </p>
                                </div>
                            </div>

                            {/* 4. 产后宝妈 / 新手父母 */}
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(31)}>
                                <div className="relative">
                                    <div className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                        <Heart className="w-8 h-8 text-pink-500" />
                                    </div>
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className="px-2 py-0.5 rounded-full bg-pink-500/20 text-pink-500 text-xs font-medium">新手爸妈</span>
                                    </div>
                                    <h3 className="text-lg font-bold text-foreground/90 mb-2">产后宝妈 / 新手父母</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">
                                        睡眠碎片化、夜间频繁惊醒，没有精力手动开关，全自动预设播放、后台静默运行、解放双手
                                    </p>
                                </div>
                            </div>

                            {/* 5. 情绪性失眠 / 焦虑人群 */}
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(32)}>
                                <div className="relative">
                                    <div className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                        <Brain className="w-8 h-8 text-sky-500" />
                                    </div>
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className="px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-500 text-xs font-medium">个性化</span>
                                    </div>
                                    <h3 className="text-lg font-bold text-foreground/90 mb-2">情绪性失眠 / 焦虑人群</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">
                                        依赖个人专属音频助眠（冥想音、雨声、私人歌单），拒绝平台推送、商业化干扰
                                    </p>
                                </div>
                            </div>

                            {/* 6. 中老年浅眠用户 */}
                            <div className={cn(homeStrongLiftCardClass, "p-6 rounded-2xl")} style={cardDiffuse(33)}>
                                <div className="relative">
                                    <div className="w-14 h-14 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                                        <Users className="w-8 h-8 text-emerald-500" />
                                    </div>
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-500 text-xs font-medium">易上手</span>
                                    </div>
                                    <h3 className="text-lg font-bold text-foreground/90 mb-2">中老年浅眠用户</h3>
                                    <p className="text-sm text-muted-foreground/70 leading-relaxed">
                                        睡眠周期短、半夜易醒，产品操作极简、全自动定时、无复杂功能，适配低门槛使用习惯
                                    </p>
                                </div>
                            </div>
                        </div>

                    </div>
                </section>

                {/* 展示模式切换 & 全功能免费 */}
                <section id="display-mode" className="py-20 px-6 relative overflow-hidden scroll-mt-20">
                    <div className="max-w-5xl mx-auto relative z-10">

                        {/* 1. 展示模式切换 */}
                        <RevealGroup className="text-center mb-16" delayBase={0}>
                            <h2 className="text-2xl md:text-3xl font-bold tracking-tight mb-4">
                                <span suppressHydrationWarning className="text-foreground/80">随心切换</span>
                                <span className="bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>展示模式</span>
                            </h2>
                            <p className="text-sm text-muted-foreground/60">适配不同使用环境，自动切换最佳视觉体验</p>
                        </RevealGroup>

                        <RevealGroup delayBase={100}>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-20 max-w-4xl mx-auto">

                                {/* 日间模式 */}
                                <button
                                    onClick={() => {
                                        setTheme("light");
                                    }}
                                    onMouseDown={(e) => {
                                        e.preventDefault();
                                        setTheme("light");
                                    }}
                                    type="button"
                                    className={cn(homeStrongLiftCardClass, "p-8 rounded-3xl border-2 border-amber-500/40 hover:border-amber-500 text-left cursor-pointer w-full active:scale-95")}
                                    style={cardDiffuse(34)}>
                                    <div className="absolute top-4 right-4 w-16 h-16 rounded-2xl bg-gradient-to-br from-amber-200 to-amber-300 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                                        <Sun className="w-8 h-8 text-amber-700" />
                                    </div>
                                    <div className="text-left">
                                        <h3 className="text-xl font-bold text-amber-700 mb-2">日间模式</h3>
                                        <p className="text-sm text-amber-600/80">明亮清晰的视觉体验，适合白天使用</p>
                                    </div>
                                </button>

                                {/* 夜间模式 */}
                                <button
                                    onClick={() => {
                                        setTheme("dark");
                                    }}
                                    onMouseDown={(e) => {
                                        e.preventDefault();
                                        setTheme("dark");
                                    }}
                                    type="button"
                                    className={cn(homeStrongLiftCardClass, "p-8 rounded-3xl border-2 border-indigo-500/40 hover:border-indigo-500 text-left cursor-pointer w-full active:scale-95")}
                                    style={cardDiffuse(35)}>
                                    <div className="absolute top-4 right-4 w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                                        <Moon className="w-8 h-8 text-white" />
                                    </div>
                                    <div className="text-left">
                                        <h3 className="text-xl font-bold text-indigo-400 mb-2">夜间模式</h3>
                                        <p className="text-sm text-indigo-300/80">柔和护眼的深色界面，适合夜晚使用</p>
                                    </div>
                                </button>
                            </div>
                        </RevealGroup>

                        {/* 2. 全功能免费使用 */}
                        <RevealGroup className="text-center mb-10" delayBase={0}>
                            <h2 className="text-2xl md:text-3xl font-bold tracking-tight mb-4">
                                <span className="bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>全功能免费</span>
                                <span suppressHydrationWarning className="text-foreground/80">使用权益</span>
                            </h2>
                            <p className="text-sm text-muted-foreground/60">无需付费、无需订阅、无任何限制，尽情享受完整功能</p>
                        </RevealGroup>

                        <RevealGroup delayBase={100}>
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 max-w-4xl mx-auto">

                                {/* 免费权益卡片1 */}
                                <div className={cn(homeLiftCardClass, "p-5 rounded-2xl text-center")} style={cardDiffuse(36)}>
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-green-500/30 to-green-600/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <Gift className="w-6 h-6 text-green-500" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">全部功能免费</h4>
                                        <p className="text-xs text-muted-foreground/60">无付费门槛</p>
                                    </div>
                                </div>

                                {/* 免费权益卡片2 */}
                                <div className={cn(homeLiftCardClass, "p-5 rounded-2xl text-center")} style={cardDiffuse(37)}>
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500/30 to-blue-600/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <Clock className="w-6 h-6 text-blue-500" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">全自动定时</h4>
                                        <p className="text-xs text-muted-foreground/60">到点自动播放</p>
                                    </div>
                                </div>

                                {/* 免费权益卡片3 */}
                                <div className={cn(homeLiftCardClass, "p-5 rounded-2xl text-center")} style={cardDiffuse(38)}>
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-500/30 to-purple-600/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <Zap className="w-6 h-6 text-purple-500" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">无广告弹窗</h4>
                                        <p className="text-xs text-muted-foreground/60">纯净体验</p>
                                    </div>
                                </div>

                                {/* 免费权益卡片4 */}
                                <div className={cn(homeLiftCardClass, "p-5 rounded-2xl text-center")} style={cardDiffuse(39)}>
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-amber-500/30 to-amber-600/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <Shield className="w-6 h-6 text-amber-500" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">隐私零收集</h4>
                                        <p className="text-xs text-muted-foreground/60">数据安全</p>
                                    </div>
                                </div>

                                {/* 免费权益卡片5 */}
                                <div className={cn(homeLiftCardClass, "p-5 rounded-2xl text-center")} style={cardDiffuse(40)}>
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-500/30 to-cyan-600/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <Smartphone className="w-6 h-6 text-cyan-500" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">全平台通用</h4>
                                        <p className="text-xs text-muted-foreground/60">多设备同步</p>
                                    </div>
                                </div>

                                {/* 免费权益卡片6 */}
                                <div className={cn(homeLiftCardClass, "p-5 rounded-2xl text-center")} style={cardDiffuse(41)}>
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-pink-500/30 to-pink-600/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <Lock className="w-6 h-6 text-pink-500" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">密码安全加密</h4>
                                        <p className="text-xs text-muted-foreground/60">银行级保障</p>
                                    </div>
                                </div>

                                {/* 免费权益卡片7 */}
                                <div className={cn(homeLiftCardClass, "p-5 rounded-2xl text-center")} style={cardDiffuse(42)}>
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-500/30 to-emerald-600/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <Zap className="w-6 h-6 text-emerald-500" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">极速加载</h4>
                                        <p className="text-xs text-muted-foreground/60">PWA应用</p>
                                    </div>
                                </div>

                                {/* 免费权益卡片8 */}
                                <div className={cn(homeLiftCardClass, "p-5 rounded-2xl text-center")} style={cardDiffuse(43)}>
                                    <div className="relative">
                                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500/30 to-indigo-600/15 flex items-center justify-center mx-auto mb-3 group-hover:scale-110 transition-transform duration-300">
                                            <Crown className="w-6 h-6 text-indigo-500" />
                                        </div>
                                        <h4 className="text-sm font-bold text-foreground/90 mb-1">专属音频库</h4>
                                        <p className="text-xs text-muted-foreground/60">云端存储</p>
                                    </div>
                                </div>
                            </div>
                        </RevealGroup>

                    </div>
                </section>

                <section className="py-16 px-6 relative overflow-hidden">
                    <div className="max-w-5xl mx-auto relative z-10">
                        <RevealGroup className="text-center mb-12" delayBase={0}>
                            <h2 className="text-2xl md:text-3xl font-bold tracking-tight mb-4">
                                <span suppressHydrationWarning className="text-foreground/80">你是否也遇到过</span>
                                <span
                                    className="bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>这些困扰</span>
                            </h2>
                            <p className="text-sm text-muted-foreground/60">梦枕帮你轻松解决</p>
                        </RevealGroup>
                        <RevealGroup delayBase={100}>
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                                <PainCard icon={Upload} title="音频难上传" desc="想用专属音频助眠，却找不到支持私人音频文件的应用" iconBg="from-teal-500/20 to-emerald-500/10" style={cardDiffuse(44)} />
                                <PainCard icon={Clock} title="定时不智能" desc="普通定时器无法自动停止，半夜醒来还得手动关闭" iconBg="from-blue-500/20 to-cyan-500/10" style={cardDiffuse(45)} />
                                <PainCard icon={Volume2} title="启停太突兀" desc="音频突然播放或停止，音量骤变极易惊醒浅眠的你" iconBg="from-violet-500/20 to-purple-500/10" style={cardDiffuse(46)} />
                                <PainCard icon={Zap} title="操作太繁琐" desc="现有工具功能分散，全流程自动化难以实现" iconBg="from-amber-500/20 to-orange-500/10" style={cardDiffuse(47)} />
                            </div>
                        </RevealGroup>
                        <RevealGroup delayBase={300}>
                            <div className="mt-10 text-center">
                                <div
                                    className="inline-flex items-center gap-3 px-6 py-3 rounded-full bg-gradient-to-r from-[var(--brand-start)]/10 to-[var(--brand-end)]/10 border border-[var(--brand-start)]/20">
                                    <Sparkles className="w-4 h-4 text-[var(--brand-glow)]" />
                                    <span suppressHydrationWarning className="text-sm text-muted-foreground">上传音频·自定义定时·淡入淡出·全自动运行</span>
                                </div>
                            </div>
                        </RevealGroup>
                    </div>
                </section>
                <section id="templates" className="py-20 px-6">
                    <div className="max-w-5xl mx-auto">
                        <RevealGroup className="text-center mb-10" delayBase={0}>
                            <h2
                                className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] bg-clip-text text-transparent mb-4 tracking-wide">助眠能力</h2>
                            <p className="text-base text-muted-foreground font-medium">围绕夜间自动播放，把关键细节拆成可感知的功能卡片</p>
                        </RevealGroup>
                        <RevealGroup delayBase={100}>
                            <TemplateSelector />
                        </RevealGroup>
                    </div>
                </section>
                <section className="py-28 px-6 relative">
                    <div className="max-w-4xl mx-auto relative z-10">
                        <RevealGroup className="text-center mb-16" delayBase={0}>
                            <h2 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
                                <span suppressHydrationWarning className="text-foreground/90">简单</span>
                                <span
                                    className="bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>三步</span>
                            </h2>
                            <p className="text-base text-muted-foreground/70">从音频到任务，几步完成夜间自动播放</p>
                        </RevealGroup>
                        <div className="grid md:grid-cols-3 gap-6 relative">
                            {[
                                {
                                    num: "01",
                                    icon: Upload,
                                    title: "上传音频",
                                    desc: "导入助眠音乐、白噪音或自己的录音文件",
                                    color: "from-[var(--brand-start)]"
                                },
                                {
                                    num: "02",
                                    icon: Clock,
                                    title: "设置任务",
                                    desc: "选择开始时间、播放时长、音量和淡入淡出",
                                    color: "from-[var(--brand-mid)]"
                                },
                                {
                                    num: "03",
                                    icon: Headphones,
                                    title: "自动播放",
                                    desc: "让任务在夜里按计划执行，减少手动操作",
                                    color: "from-[var(--brand-end)]"
                                }
                            ].map((item, idx) => <RevealGroup key={idx} delayBase={idx * 120}>
                                <div className="relative group">
                                    <div
                                        className={cn(homeLiftCardClass, "h-[200px] p-6 rounded-2xl flex flex-col duration-400")}
                                        style={cardDiffuse(48 + idx)}>
                                        <div
                                            className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${item.color} to-transparent opacity-60`} />
                                        <div className="relative z-10 text-center flex flex-col h-full">
                                            <div
                                                className={`w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br ${item.color}/20 to-transparent flex items-center justify-center mb-5 group-hover:scale-110 transition-transform duration-300 shrink-0`}>
                                                <item.icon className="w-7 h-7 text-[var(--brand-glow)]" />
                                            </div>
                                            <h3
                                                className="text-xl font-semibold text-foreground/90 mb-3 tracking-tight shrink-0">{item.title}</h3>
                                            <p className="text-sm text-muted-foreground/70 leading-relaxed flex-1">{item.desc}</p>
                                        </div>
                                    </div>
                                </div>
                            </RevealGroup>)}
                        </div>
                    </div>
                </section>
                <section id="start" className="py-24 px-6 relative">
                    <div className="max-w-2xl mx-auto relative z-10 text-center">
                        <RevealGroup delayBase={0}>
                            <h2 className="text-3xl md:text-4xl font-bold tracking-tight mb-4">
                                <span suppressHydrationWarning className="text-foreground/90">开始</span>
                                <span
                                    className="bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>设置</span>
                            </h2>
                            <p className="text-base text-muted-foreground/70 mb-8">上传音频并创建任务，让梦枕按你的节奏播放</p>
                            <div className="flex items-center justify-center mb-8">
                                <div
                                    className="flex items-center gap-2 px-4 py-2 rounded-full bg-gradient-to-r from-[var(--brand-start)]/15 to-[var(--brand-end)]/10 border border-[var(--brand-start)]/20">
                                    <div className="w-2 h-2 rounded-full bg-[var(--brand-start)] animate-pulse" />
                                    <span suppressHydrationWarning className="text-[var(--brand-start)] text-sm font-medium">无需登录·开箱即用</span>
                                </div>
                            </div>
                            <RippleButton
                                ref={bottomCtaRef}
                                onClick={() => router.replace("/settings")}
                                className="group relative inline-flex items-center gap-3 px-8 py-4 rounded-2xl bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] text-white font-semibold text-xl shadow-xl shadow-[var(--brand-start)]/25 hover:shadow-2xl hover:shadow-[var(--brand-start)]/35 hover:scale-105 active:scale-95 transition-all duration-300">
                                <div className="relative flex items-center gap-3">
                                    <Image
                                        src="/logo.png"
                                        alt="梦枕"
                                        width={28}
                                        height={28}
                                        className="w-7 h-7 group-hover:scale-110 transition-transform duration-300 rounded shadow-md" />
                                    <span suppressHydrationWarning>开始设置</span>
                                    <ChevronRight
                                        className="w-6 h-6 group-hover:translate-x-1 transition-transform duration-300" />
                                </div>
                            </RippleButton>
                        </RevealGroup>
                    </div>
                </section>
                </LazySection>
            </main>
            <footer className="border-t border-border py-8 px-6 bg-muted/20 relative z-20">
                <div className="max-w-5xl mx-auto text-center">
                    <div className="flex items-center justify-center gap-2 mb-3">
                        <Image
                            src="/logo.png"
                            alt="梦枕"
                            width={20}
                            height={20}
                            className="rounded-md shadow-[inset_0_1px_4px_rgba(0,0,0,0.35)]" />
                        <span
                            className="font-bold text-lg bg-gradient-to-r from-[var(--brand-start)] to-[var(--brand-end)] bg-clip-text text-transparent" suppressHydrationWarning>梦枕</span>
                    </div>
                    <p className="text-xs text-muted-foreground">深夜助眠播放器·PWA渐进式网页应用·自定义音频</p>
                </div>
            </footer>
            <FloatingBar visible={showFloatingBar} />
        </div>
    );
}
