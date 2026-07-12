"use client";
import { cn } from "@/lib/utils";

export function HeroTitle({
    className,
    fontSize,
    gradientId = "heroTitleGradient",
    animated = false
}: {
    className?: string;
    fontSize: string;
    gradientId?: string;
    animated?: boolean;
}) {
    if (animated) {
        return (
            <svg className={cn("w-full mx-auto", className)} viewBox="0 0 600 300" preserveAspectRatio="xMidYMid meet">
                <defs>
                    <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#5EEDA0" />
                        <stop offset="35%" stopColor="#40C78A" />
                        <stop offset="50%" stopColor="#60C4A0" />
                        <stop offset="65%" stopColor="#9055E0" />
                        <stop offset="100%" stopColor="#A855F7" />
                    </linearGradient>
                </defs>
                <text x="50%" y="150" textAnchor="middle" dominantBaseline="middle" fontSize={fontSize} fontWeight="bold" fontFamily="system-ui, -apple-system, sans-serif" fill={`url(#${gradientId})`} style={{ boxShadow: "rgba(0, 0, 0, 0.15) 0px 0px 30px 0px" }}>
                    <tspan x="50%" dy="-0.5em" className="svg-char" style={{ ["--char-delay" as string]: "200ms" }}>星</tspan>
                    <tspan className="svg-char" style={{ ["--char-delay" as string]: "260ms" }}>河</tspan>
                    <tspan className="svg-char" style={{ ["--char-delay" as string]: "320ms" }}>入</tspan>
                    <tspan className="svg-char" style={{ ["--char-delay" as string]: "380ms" }}>眠</tspan>
                    <tspan x="50%" dy="1.2em" className="svg-char" style={{ ["--char-delay" as string]: "500ms" }}>伴</tspan>
                    <tspan className="svg-char" style={{ ["--char-delay" as string]: "560ms" }}>你</tspan>
                    <tspan className="svg-char" style={{ ["--char-delay" as string]: "620ms" }}>梦</tspan>
                    <tspan className="svg-char" style={{ ["--char-delay" as string]: "680ms" }}>枕</tspan>
                </text>
            </svg>
        );
    }

    return (
        <svg className={cn("w-full mx-auto", className)} viewBox="0 0 600 300" preserveAspectRatio="xMidYMid meet">
            <defs>
                <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#5EEDA0" />
                    <stop offset="35%" stopColor="#40C78A" />
                    <stop offset="50%" stopColor="#60C4A0" />
                    <stop offset="65%" stopColor="#9055E0" />
                    <stop offset="100%" stopColor="#A855F7" />
                </linearGradient>
            </defs>
            <text x="50%" y="150" textAnchor="middle" dominantBaseline="middle" fontSize={fontSize} fontWeight="bold" fontFamily="system-ui, -apple-system, sans-serif" fill={`url(#${gradientId})`} style={{ boxShadow: "rgba(0, 0, 0, 0.15) 0px 0px 30px 0px" }}>
                <tspan x="50%" dy="-0.5em">星河入眠</tspan>
                <tspan x="50%" dy="1.2em">伴你梦枕</tspan>
            </text>
        </svg>
    );
}
