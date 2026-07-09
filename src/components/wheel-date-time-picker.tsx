"use client";
import React, { useEffect, useCallback, useRef, useMemo, useState } from "react";
import { cn } from "@/lib/utils";
import { useIsMobile } from "@/hooks/use-mobile";
import { useTheme } from "@/lib/theme-context";

export interface DateTimeValue {
    year: number;
    month: number;
    day: number;
    hour: number;
    minute: number;
    second: number;
}

export interface WheelDateTimePickerProps {
    value: DateTimeValue;
    onChange: (value: DateTimeValue) => void;
    label: string;
}

const ITEM_HEIGHT = 36;
const VISIBLE_COUNT = 5;
const centerOffset = (VISIBLE_COUNT * ITEM_HEIGHT) / 2 - ITEM_HEIGHT / 2;

interface WheelPickerColumnProps {
    items: { value: number; label: string }[];
    value: number;
    onChange: (value: number) => void;
    label: string;
    isDark?: boolean;
    loop?: boolean;
}

const formatDateTime = (v: DateTimeValue) =>
    `${v.year}-${String(v.month).padStart(2, "0")}-${String(v.day).padStart(2, "0")} ${String(v.hour).padStart(2, "0")}:${String(v.minute).padStart(2, "0")}:${String(v.second).padStart(2, "0")}`;

const daysInMonth = (year: number, month: number) => new Date(year, month, 0).getDate();
const makeOptions = (count: number, start = 0, pad = 2) =>
    Array.from({ length: count }, (_, i) => ({ value: start + i, label: String(start + i).padStart(pad, "0") }));
const clampIndex = (index: number, length: number) => Math.max(0, Math.min(length - 1, index));

function useClientOnly() {
    const [mounted, setMounted] = useState(false);
    useEffect(() => setMounted(true), []);
    return mounted;
}

function WheelPickerColumn({ items, value, onChange, label, isDark = false, loop = false }: WheelPickerColumnProps) {
    const scrollerRef = useRef<HTMLDivElement>(null);
    const commitTimerRef = useRef<NodeJS.Timeout | null>(null);
    const isResettingRef = useRef(false);
    const ignoreNextScrollRef = useRef(false);
    const lastScrollTopRef = useRef(0);
    const mounted = useClientOnly();
    const baseLen = items.length;

    const displayItems = useMemo(() => loop && baseLen ? [...items, ...items, ...items] : items, [items, loop, baseLen]);
    const selectedIndex = items.findIndex(item => item.value === value);
    const currentIndex = selectedIndex >= 0 ? selectedIndex : Math.floor(baseLen / 2);
    const initialScrollIndex = loop ? currentIndex + baseLen : currentIndex;

    const getRealIndex = useCallback((scrollPos: number) => {
        const rawIdx = Math.round(scrollPos / ITEM_HEIGHT);
        if (!loop) return clampIndex(rawIdx, baseLen);
        return ((rawIdx % baseLen) + baseLen) % baseLen;
    }, [loop, baseLen]);

    const checkAndResetLoop = useCallback(() => {
        if (!loop) return;
        const el = scrollerRef.current;
        if (!el) return;
        const scrollTop = el.scrollTop;
        const singleLen = baseLen * ITEM_HEIGHT;
        let newTop = scrollTop;
        if (scrollTop < singleLen * 0.5) newTop = scrollTop + singleLen;
        else if (scrollTop > singleLen * 2.5) newTop = scrollTop - singleLen;
        if (newTop !== scrollTop) {
            isResettingRef.current = true;
            ignoreNextScrollRef.current = true;
            el.scrollTop = newTop;
            requestAnimationFrame(() => {
                isResettingRef.current = false;
                ignoreNextScrollRef.current = false;
            });
        }
    }, [loop, baseLen]);

    useEffect(() => {
        if (!mounted) return;
        const el = scrollerRef.current;
        if (!el) return;
        const targetTop = initialScrollIndex * ITEM_HEIGHT;
        if (Math.abs(el.scrollTop - targetTop) > 1) {
            isResettingRef.current = true;
            ignoreNextScrollRef.current = true;
            el.scrollTop = targetTop;
            requestAnimationFrame(() => {
                isResettingRef.current = false;
                ignoreNextScrollRef.current = false;
            });
        }
    }, [initialScrollIndex, mounted]);

    // 检查滚动是否停止（用于惯性滚动）
    const checkScrollEnd = useCallback(() => {
        const el = scrollerRef.current;
        if (!el) return;

        if (Math.abs(el.scrollTop - lastScrollTopRef.current) < 1) {
            // 滚动停止
            const index = getRealIndex(el.scrollTop);
            const realIdx = loop ? ((index % baseLen) + baseLen) % baseLen : index;
            const item = items[realIdx];
            if (item && item.value !== value) {
                onChange(item.value);
            }
        } else {
            // 还在滚动，继续检查
            lastScrollTopRef.current = el.scrollTop;
            commitTimerRef.current = setTimeout(checkScrollEnd, 100);
        }
    }, [getRealIndex, items, value, onChange, loop, baseLen]);

    const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
        if (isResettingRef.current || ignoreNextScrollRef.current) {
            ignoreNextScrollRef.current = false;
            return;
        }
        checkAndResetLoop();

        lastScrollTopRef.current = e.currentTarget.scrollTop;

        // 每次滚动都重置计时器，等滚动完全停止后再提交
        if (commitTimerRef.current) clearTimeout(commitTimerRef.current);
        commitTimerRef.current = setTimeout(checkScrollEnd, 150);
    }, [checkAndResetLoop, checkScrollEnd]);

    useEffect(() => () => {
        if (commitTimerRef.current) clearTimeout(commitTimerRef.current);
    }, []);

    return (
        <div className="flex flex-col items-center min-w-0 sm:w-20 sm:min-w-20 flex-1 sm:flex-initial">
            <div className={cn("text-xs mb-1", isDark ? "text-zinc-500" : "text-zinc-400")}>{label}</div>
            <div className="relative overflow-hidden rounded-lg select-none w-full" style={{
                height: ITEM_HEIGHT * VISIBLE_COUNT,
                background: isDark ? 'linear-gradient(to bottom, #18181b, #27272a, #18181b)' : 'linear-gradient(to bottom, #f4f4f5, #e4e4e7, #f4f4f5)',
            }}>
                <div className="absolute top-1/2 left-0 right-0 -translate-y-1/2 z-10 pointer-events-none" style={{
                    height: ITEM_HEIGHT,
                    background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.05)',
                    borderTop: isDark ? '1px solid rgba(255,255,255,0.1)' : '1px solid rgba(0,0,0,0.1)',
                    borderBottom: isDark ? '1px solid rgba(255,255,255,0.1)' : '1px solid rgba(0,0,0,0.1)',
                    borderRadius: 4,
                }} />
                <div
                    ref={scrollerRef}
                    className="h-full overflow-y-auto snap-y snap-mandatory"
                    onScroll={mounted ? handleScroll : undefined}
                    style={{
                        paddingTop: centerOffset,
                        paddingBottom: centerOffset,
                        scrollbarWidth: "none",
                        WebkitOverflowScrolling: "touch",
                    }}
                >
                    {displayItems.map((item, displayIndex) => {
                        const realIdx = loop ? displayIndex - baseLen : displayIndex;
                        const isSelected = item.value === value;
                        return (
                            <div
                                key={`${item.value}-${displayIndex}`}
                                className={cn("flex w-full snap-center items-center justify-center",
                                    isSelected ? (isDark ? "text-white font-semibold text-lg" : "text-black font-semibold text-lg")
                                        : (isDark ? "text-zinc-500 text-base" : "text-zinc-400 text-base")
                                )}
                                style={{ height: ITEM_HEIGHT }}
                                onClick={() => {
                                    const targetIdx = loop
                                        ? (realIdx < 0 ? displayIndex + baseLen : realIdx >= baseLen ? displayIndex - baseLen : displayIndex)
                                        : displayIndex;
                                    const idx = getRealIndex(targetIdx * ITEM_HEIGHT);
                                    const targetItem = items[idx];
                                    if (targetItem && targetItem.value !== value) {
                                        onChange(targetItem.value);
                                    }
                                }}
                            >
                                {item.label}
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}

export const WheelDateTimePicker = React.memo(function WheelDateTimePicker({ value, onChange, label }: WheelDateTimePickerProps) {
    const mounted = useClientOnly();
    const isMobile = useIsMobile();
    const { resolvedTheme } = useTheme();
    const isDark = resolvedTheme === "dark";

    const now = mounted ? new Date() : new Date("2025-01-01T00:00:00Z");
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;
    const currentDay = now.getDate();

    const safeValue = useMemo(() => ({
        year: value?.year ?? currentYear,
        month: value?.month ?? currentMonth,
        day: value?.day ?? currentDay,
        hour: value?.hour ?? 0,
        minute: value?.minute ?? 0,
        second: value?.second ?? 0
    }), [value, currentYear, currentMonth, currentDay]);

    const handleChange = useCallback((key: keyof DateTimeValue) => (val: number) => {
        const next = { ...safeValue, [key]: val };
        if (key === 'year' || key === 'month') next.day = Math.min(next.day, daysInMonth(next.year, next.month));
        onChange(next);
    }, [safeValue, onChange]);

    const yearOptions = useMemo(() => makeOptions(21, currentYear - 10, 0), [currentYear]);
    const monthOptions = useMemo(() => makeOptions(12, 1), []);
    const dayOptions = useMemo(() => makeOptions(daysInMonth(safeValue.year, safeValue.month), 1), [safeValue.year, safeValue.month]);
    const hourOptions = useMemo(() => makeOptions(24), []);
    const minuteOptions = useMemo(() => makeOptions(60), []);
    const secondOptions = useMemo(() => makeOptions(60), []);

    // mounted 之前用桌面端列，确保服务端和客户端一致
    const actualIsMobile = mounted ? isMobile : false;

    const columns = useMemo(() => actualIsMobile ? [
        { options: dayOptions, key: 'day' as const, label: '日', loop: false },
        { options: hourOptions, key: 'hour' as const, label: '时', loop: true },
        { options: minuteOptions, key: 'minute' as const, label: '分', loop: true },
    ] : [
        { options: yearOptions, key: 'year' as const, label: '年', loop: false },
        { options: monthOptions, key: 'month' as const, label: '月', loop: true },
        { options: dayOptions, key: 'day' as const, label: '日', loop: false },
        { options: hourOptions, key: 'hour' as const, label: '时', loop: true },
        { options: minuteOptions, key: 'minute' as const, label: '分', loop: true },
        { options: secondOptions, key: 'second' as const, label: '秒', loop: true },
    ], [actualIsMobile, yearOptions, monthOptions, dayOptions, hourOptions, minuteOptions, secondOptions]);

    return (
        <div className="w-full">
            <div className={cn("text-sm mb-2 flex justify-between items-center", isDark ? "text-zinc-400" : "text-zinc-500")}>
                <span>{label}</span>
                <span className="font-mono text-xs tabular-nums opacity-70">{mounted ? formatDateTime(safeValue) : ""}</span>
            </div>
            <div className="flex items-center gap-0.5 sm:gap-1">
                {columns.map(col => (
                    <WheelPickerColumn
                        key={col.key}
                        items={col.options}
                        value={safeValue[col.key]}
                        onChange={handleChange(col.key)}
                        label={col.label}
                        isDark={isDark}
                        loop={col.loop}
                    />
                ))}
            </div>
        </div>
    );
});
